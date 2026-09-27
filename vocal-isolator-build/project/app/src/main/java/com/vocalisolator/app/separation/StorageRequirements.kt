package com.vocalisolator.app.separation

object StorageRequirements {
    private const val MIB = 1024L * 1024L
    private const val PCM_BYTES_PER_SECOND = 44_100L * 2L * 2L

    fun estimatedPcmBytes(durationMs: Long): Long =
        ((durationMs.coerceAtLeast(0L) * PCM_BYTES_PER_SECOND) + 999L) / 1000L

    fun requiredTempBytes(durationMs: Long): Long =
        (64L * MIB) + (estimatedPcmBytes(durationMs) * 3L)
}
