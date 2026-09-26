package com.vocalisolator.app.separation

import com.vocalisolator.app.model.AppError
import com.vocalisolator.app.model.NormalizedAudio
import com.vocalisolator.app.model.SeparationResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.atomic.AtomicLong

fun interface NativeProgressCallback {
    fun onProgress(fraction: Float, message: String)
}

class DemucsNativeBridge(
    private val modelFileProvider: suspend () -> Result<File>,
) : SeparationEngine {
    private val handle = AtomicLong(0L)

    init {
        System.loadLibrary("demucs_jni")
    }

    override suspend fun initialize(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            if (handle.get() != 0L) return@runCatching
            val modelFile = modelFileProvider().getOrThrow()
            val created = nativeCreate(modelFile.absolutePath)
            if (created == 0L) throw AppError.ModelLoadFailure()
            handle.set(created)
        }
    }

    override suspend fun separate(
        input: NormalizedAudio,
        outputDir: File,
        onProgress: (Float, String) -> Unit,
    ): Result<SeparationResult> = withContext(Dispatchers.IO) {
        runCatching {
            if (handle.get() == 0L) initialize().getOrThrow()
            outputDir.mkdirs()
            val vocals = File(outputDir, "vocals.wav")
            val instrumental = File(outputDir, "instrumental.wav")
            val status = nativeSeparate(
                handle.get(),
                input.wavFile.absolutePath,
                vocals.absolutePath,
                instrumental.absolutePath,
                NativeProgressCallback(onProgress),
            )
            NativeStatusMapper.toError(status)?.let { throw it }
            SeparationResult(vocals, instrumental, input.durationMs)
        }
    }

    override fun cancel() {
        handle.get().takeIf { it != 0L }?.let(::nativeCancel)
    }

    override fun release() {
        val current = handle.getAndSet(0L)
        if (current != 0L) nativeRelease(current)
    }

    private external fun nativeCreate(modelPath: String): Long
    private external fun nativeSeparate(
        handle: Long,
        inputWavPath: String,
        vocalsPath: String,
        instrumentalPath: String,
        callback: NativeProgressCallback,
    ): Int
    private external fun nativeCancel(handle: Long)
    private external fun nativeRelease(handle: Long)
}
