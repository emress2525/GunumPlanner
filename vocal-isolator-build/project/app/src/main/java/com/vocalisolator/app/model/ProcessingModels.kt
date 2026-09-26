package com.vocalisolator.app.model

import java.io.File

data class NormalizedAudio(
    val wavFile: File,
    val sampleRateHz: Int = 44_100,
    val channelCount: Int = 2,
    val durationMs: Long,
)

data class SeparationResult(
    val vocalsWav: File,
    val instrumentalWav: File,
    val durationMs: Long,
)

enum class ProcessingStage { PREPARING, SEPARATING, EXPORTING }

data class ProcessingProgress(
    val stage: ProcessingStage,
    val fraction: Float,
    val message: String = "",
)
