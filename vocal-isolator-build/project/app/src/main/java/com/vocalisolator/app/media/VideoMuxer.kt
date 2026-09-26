package com.vocalisolator.app.media

import android.content.Context
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import com.vocalisolator.app.model.AppError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.coroutineContext

interface VideoMuxer {
    suspend fun createVocalOnlyVideo(sourceVideo: Uri, vocalsWav: File, outputMp4: File, onProgress: (Float) -> Unit = {}): Result<File>
    fun cancel()
}

private class PcmWavSource(file: File) : AutoCloseable {
    private val raf = RandomAccessFile(file, "r")
    val sampleRate: Int
    val channels: Int
    val dataOffset: Long
    val dataBytes: Long
    private var bytesRead = 0L
    init {
        fun read4(): String = String(ByteArray(4).also(raf::readFully), Charsets.US_ASCII)
        fun u16(): Int = java.lang.Short.reverseBytes(raf.readShort()).toInt() and 0xffff
        fun u32(): Long = Integer.toUnsignedLong(Integer.reverseBytes(raf.readInt()))
        require(read4() == "RIFF"); u32(); require(read4() == "WAVE")
        var fmtRate = 0; var fmtChannels = 0; var fmtBits = 0
        var foundFmt = false; var foundData = false
        var dOffset = 0L; var dBytes = 0L
        while (raf.filePointer + 8 <= raf.length() && !(foundFmt && foundData)) {
            val id = read4(); val size = u32(); val payload = raf.filePointer
            when (id) {
                "fmt " -> {
                    val audioFormat = u16(); fmtChannels = u16(); fmtRate = u32().toInt()
                    u32(); u16(); fmtBits = u16(); require(audioFormat == 1); foundFmt = true
                }
                "data" -> { dOffset = payload; dBytes = size; foundData = true }
            }
            raf.seek(payload + size + (size and 1L))
        }
        require(foundFmt && foundData)
        require(fmtRate == 44_100 && fmtChannels == 2 && fmtBits == 16)
        sampleRate = fmtRate; channels = fmtChannels; dataOffset = dOffset; dataBytes = dBytes
        raf.seek(dataOffset)
    }
    fun readInto(buffer: ByteBuffer): Int {
        val remaining = dataBytes - bytesRead
        if (remaining <= 0) return -1
        val max = minOf(buffer.remaining().toLong(), remaining).toInt().let { it - (it % 4) }
        if (max <= 0) return -1
        val tmp = ByteArray(max)
        val count = raf.read(tmp)
        if (count <= 0) return -1
        buffer.put(tmp, 0, count); bytesRead += count; return count
    }
    override fun close() = raf.close()
}

class AndroidVideoMuxer(private val context: Context) : VideoMuxer {
    private val cancelled = AtomicBoolean(false)
    override suspend fun createVocalOnlyVideo(
        sourceVideo: Uri,
        vocalsWav: File,
        outputMp4: File,
        onProgress: (Float) -> Unit,
    ): Result<File> = withContext(Dispatchers.IO) {
        cancelled.set(false)
        outputMp4.parentFile?.mkdirs(); outputMp4.delete()
        runCatching {
            if (!vocalsWav.isFile) throw AppError.VideoMuxFailure(IllegalArgumentException("Vocal WAV is missing"))
            val extractor = MediaExtractor()
            var encoder: MediaCodec? = null
            var muxer: MediaMuxer? = null
            var muxerStarted = false
            try {
                extractor.setDataSource(context, sourceVideo, null)
                var videoTrackIndex = -1
                var videoFormat: MediaFormat? = null
                for (i in 0 until extractor.trackCount) {
                    val f = extractor.getTrackFormat(i)
                    if (f.getString(MediaFormat.KEY_MIME).orEmpty().startsWith("video/")) { videoTrackIndex = i; videoFormat = f; break }
                }
                if (videoTrackIndex < 0 || videoFormat == null) throw AppError.VideoMuxFailure(IllegalArgumentException("Source has no video track"))
                extractor.selectTrack(videoTrackIndex)
                val videoDurationUs = if (videoFormat.containsKey(MediaFormat.KEY_DURATION)) videoFormat.getLong(MediaFormat.KEY_DURATION) else 0L
                val audioFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, 44_100, 2).apply {
                    setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
                    setInteger(MediaFormat.KEY_BIT_RATE, 192_000)
                    setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 32 * 1024)
                }
                encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
                encoder.configure(audioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE); encoder.start()
                muxer = MediaMuxer(outputMp4.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
                var muxVideoTrack = -1; var muxAudioTrack = -1
                var inputDone = false; var outputDone = false; var framesFed = 0L
                val info = MediaCodec.BufferInfo()
                PcmWavSource(vocalsWav).use { wav ->
                    while (!outputDone) {
                        coroutineContext.ensureActive()
                        if (cancelled.get()) throw AppError.Cancelled()
                        if (!inputDone) {
                            val inputIndex = encoder.dequeueInputBuffer(10_000)
                            if (inputIndex >= 0) {
                                val inputBuffer = encoder.getInputBuffer(inputIndex) ?: throw AppError.VideoMuxFailure()
                                inputBuffer.clear()
                                val bytes = wav.readInto(inputBuffer)
                                if (bytes < 0) {
                                    val pts = framesFed * 1_000_000L / 44_100L
                                    encoder.queueInputBuffer(inputIndex, 0, 0, pts, MediaCodec.BUFFER_FLAG_END_OF_STREAM); inputDone = true
                                } else {
                                    val frames = bytes / 4; val pts = framesFed * 1_000_000L / 44_100L
                                    encoder.queueInputBuffer(inputIndex, 0, bytes, pts, 0); framesFed += frames
                                }
                            }
                        }
                        when (val outIndex = encoder.dequeueOutputBuffer(info, 10_000)) {
                            MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                                if (muxerStarted) throw AppError.VideoMuxFailure(IllegalStateException("AAC format changed twice"))
                                muxVideoTrack = muxer.addTrack(videoFormat)
                                muxAudioTrack = muxer.addTrack(encoder.outputFormat)
                                muxer.start(); muxerStarted = true
                            }
                            MediaCodec.INFO_TRY_AGAIN_LATER -> Unit
                            else -> if (outIndex >= 0) {
                                val outputBuffer = encoder.getOutputBuffer(outIndex) ?: throw AppError.VideoMuxFailure()
                                if (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) info.size = 0
                                if (info.size > 0) {
                                    if (!muxerStarted) throw AppError.VideoMuxFailure(IllegalStateException("Muxer not started"))
                                    outputBuffer.position(info.offset); outputBuffer.limit(info.offset + info.size)
                                    muxer.writeSampleData(muxAudioTrack, outputBuffer, info)
                                    val audioDurationUs = framesFed * 1_000_000L / 44_100L
                                    if (audioDurationUs > 0) onProgress((0.45f * info.presentationTimeUs.toFloat() / audioDurationUs).coerceIn(0f, 0.45f))
                                }
                                outputDone = info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                                encoder.releaseOutputBuffer(outIndex, false)
                            }
                        }
                    }
                }
                if (!muxerStarted) throw AppError.VideoMuxFailure(IllegalStateException("AAC encoder produced no output format"))
                extractor.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
                val maxInput = if (videoFormat.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) videoFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE) else 2 * 1024 * 1024
                val videoBuffer = ByteBuffer.allocateDirect(maxOf(maxInput, 2 * 1024 * 1024))
                val videoInfo = MediaCodec.BufferInfo()
                while (true) {
                    coroutineContext.ensureActive()
                    if (cancelled.get()) throw AppError.Cancelled()
                    videoBuffer.clear()
                    val size = extractor.readSampleData(videoBuffer, 0)
                    if (size < 0) break
                    videoInfo.set(0, size, extractor.sampleTime, extractor.sampleFlags)
                    videoBuffer.position(0); videoBuffer.limit(size)
                    muxer.writeSampleData(muxVideoTrack, videoBuffer, videoInfo)
                    if (videoDurationUs > 0) onProgress((0.45f + 0.55f * extractor.sampleTime.toFloat() / videoDurationUs).coerceIn(0.45f, 0.99f))
                    extractor.advance()
                }
                onProgress(1f); outputMp4
            } finally {
                runCatching { encoder?.stop() }; runCatching { encoder?.release() }
                if (muxerStarted) runCatching { muxer?.stop() }
                runCatching { muxer?.release() }; runCatching { extractor.release() }
            }
        }.recoverCatching { cause ->
            outputMp4.delete()
            when (cause) {
                is AppError -> throw cause
                is kotlinx.coroutines.CancellationException -> throw AppError.Cancelled()
                else -> throw AppError.VideoMuxFailure(cause)
            }
        }
    }
    override fun cancel() { cancelled.set(true) }
}
