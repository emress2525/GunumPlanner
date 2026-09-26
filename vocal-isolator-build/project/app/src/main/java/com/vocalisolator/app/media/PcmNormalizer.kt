package com.vocalisolator.app.media

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import com.vocalisolator.app.model.AppError
import com.vocalisolator.app.model.NormalizedAudio
import com.vocalisolator.app.model.SelectedMedia
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteOrder
import kotlin.coroutines.coroutineContext

interface PcmNormalizer {
    suspend fun normalize(media: SelectedMedia, sessionDir: File, onProgress: (Float) -> Unit = {}): Result<NormalizedAudio>
}

class AndroidPcmNormalizer(private val context: Context) : PcmNormalizer {
    override suspend fun normalize(
        media: SelectedMedia,
        sessionDir: File,
        onProgress: (Float) -> Unit,
    ): Result<NormalizedAudio> = withContext(Dispatchers.IO) {
        runCatching {
            sessionDir.mkdirs()
            val output = File(sessionDir, "normalized.wav")
            val extractor = MediaExtractor()
            var decoder: MediaCodec? = null
            try {
                extractor.setDataSource(context, media.uri, null)
                var audioTrack = -1
                var inputFormat: MediaFormat? = null
                for (i in 0 until extractor.trackCount) {
                    val format = extractor.getTrackFormat(i)
                    val mime = format.getString(MediaFormat.KEY_MIME).orEmpty()
                    if (mime.startsWith("audio/")) {
                        audioTrack = i
                        inputFormat = format
                        break
                    }
                }
                if (audioTrack < 0 || inputFormat == null) throw AppError.NoAudioTrack()
                extractor.selectTrack(audioTrack)
                val mime = inputFormat.getString(MediaFormat.KEY_MIME) ?: throw AppError.DecoderFailure()
                decoder = MediaCodec.createDecoderByType(mime)
                decoder.configure(inputFormat, null, null, 0)
                decoder.start()

                var inputDone = false
                var outputDone = false
                var sampleRate = inputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                var channels = inputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                var resampler: StreamingPcmResampler? = null
                var outputFrames = 0L
                Pcm16WavWriter(output).use { writer ->
                    fun ensureResampler(): StreamingPcmResampler =
                        resampler ?: StreamingPcmResampler(sampleRate, channels) { samples, count ->
                            writer.write(samples, count)
                            outputFrames += count / 2L
                        }.also { resampler = it }

                    val info = MediaCodec.BufferInfo()
                    while (!outputDone) {
                        coroutineContext.ensureActive()
                        if (!inputDone) {
                            val index = decoder.dequeueInputBuffer(10_000)
                            if (index >= 0) {
                                val buffer = decoder.getInputBuffer(index) ?: throw AppError.DecoderFailure()
                                val size = extractor.readSampleData(buffer, 0)
                                if (size < 0) {
                                    decoder.queueInputBuffer(index, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                    inputDone = true
                                } else {
                                    decoder.queueInputBuffer(index, 0, size, extractor.sampleTime, 0)
                                    extractor.advance()
                                }
                            }
                        }
                        when (val outIndex = decoder.dequeueOutputBuffer(info, 10_000)) {
                            MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                                val format = decoder.outputFormat
                                sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                                channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                                resampler = null
                            }
                            MediaCodec.INFO_TRY_AGAIN_LATER -> Unit
                            else -> if (outIndex >= 0) {
                                if (info.size > 0) {
                                    val buffer = decoder.getOutputBuffer(outIndex) ?: throw AppError.DecoderFailure()
                                    buffer.position(info.offset)
                                    buffer.limit(info.offset + info.size)
                                    val shorts = buffer.slice().order(ByteOrder.nativeOrder()).asShortBuffer()
                                    val data = ShortArray(shorts.remaining())
                                    shorts.get(data)
                                    ensureResampler().push(data)
                                    if (media.durationMs > 0 && info.presentationTimeUs >= 0) {
                                        onProgress((info.presentationTimeUs / 1000f / media.durationMs).coerceIn(0f, 1f))
                                    }
                                }
                                outputDone = info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                                decoder.releaseOutputBuffer(outIndex, false)
                            }
                        }
                    }
                }
                onProgress(1f)
                NormalizedAudio(output, durationMs = NormalizationMath.durationMs(outputFrames, 44_100))
            } finally {
                runCatching { decoder?.stop() }
                runCatching { decoder?.release() }
                runCatching { extractor.release() }
            }
        }.recoverCatching { cause ->
            when (cause) {
                is AppError -> throw cause
                is kotlinx.coroutines.CancellationException -> throw cause
                else -> throw AppError.DecoderFailure(cause)
            }
        }
    }
}
