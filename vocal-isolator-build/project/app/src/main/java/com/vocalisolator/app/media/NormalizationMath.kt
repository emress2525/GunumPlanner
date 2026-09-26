package com.vocalisolator.app.media
import kotlin.math.roundToLong
object NormalizationMath {
    fun targetFrames(sourceFrames: Long, sourceRateHz: Int, targetRateHz: Int = 44_100): Long {
        require(sourceFrames >= 0); require(sourceRateHz > 0 && targetRateHz > 0)
        return (sourceFrames.toDouble() * targetRateHz.toDouble() / sourceRateHz.toDouble()).roundToLong()
    }
    fun durationMs(frames: Long, sampleRateHz: Int): Long {
        require(frames >= 0 && sampleRateHz > 0)
        return (frames * 1000.0 / sampleRateHz).roundToLong()
    }
}
