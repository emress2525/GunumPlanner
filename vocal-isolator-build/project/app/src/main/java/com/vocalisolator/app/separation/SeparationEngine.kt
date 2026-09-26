package com.vocalisolator.app.separation

import com.vocalisolator.app.model.NormalizedAudio
import com.vocalisolator.app.model.SeparationResult
import java.io.File

interface SeparationEngine {
    suspend fun initialize(): Result<Unit>
    suspend fun separate(
        input: NormalizedAudio,
        outputDir: File,
        onProgress: (Float, String) -> Unit = { _, _ -> },
    ): Result<SeparationResult>
    fun cancel()
    fun release()
}
