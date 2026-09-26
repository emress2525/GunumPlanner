package com.vocalisolator.app.media
import kotlin.math.floor
import kotlin.math.roundToInt
class StreamingPcmResampler(
    private val sourceRateHz: Int,
    private val sourceChannels: Int,
    private val targetRateHz: Int = 44_100,
    private val sink: (ShortArray, Int) -> Unit,
) {
    private val step = sourceRateHz.toDouble() / targetRateHz.toDouble()
    private var totalInputFrames = 0L
    private var nextOutputPos = 0.0
    private var previousFrame: ShortArray? = null
    init { require(sourceRateHz > 0); require(sourceChannels > 0) }
    fun push(interleaved: ShortArray, sampleCount: Int = interleaved.size) {
        val frames = sampleCount / sourceChannels
        if (frames <= 0) return
        val prev = previousFrame
        val baseIndex = if (prev != null) totalInputFrames - 1 else totalInputFrames
        val availableFrames = frames + if (prev != null) 1 else 0
        val lastIndexExclusive = baseIndex + availableFrames
        val out = ShortArray((frames * targetRateHz.toDouble() / sourceRateHz + 4).toInt().coerceAtLeast(4) * 2)
        var outCount = 0
        fun channelAt(absFrame: Long, channel: Int): Short {
            if (prev != null && absFrame == totalInputFrames - 1) return prev[channel.coerceAtMost(prev.lastIndex)]
            val local = (absFrame - totalInputFrames).toInt()
            val srcBase = local * sourceChannels
            return if (sourceChannels == 1) interleaved[srcBase] else interleaved[srcBase + channel.coerceAtMost(sourceChannels - 1)]
        }
        while (nextOutputPos + 1.0 < lastIndexExclusive.toDouble()) {
            val i0 = floor(nextOutputPos).toLong(); val i1 = i0 + 1
            if (i0 < baseIndex || i1 >= lastIndexExclusive) break
            val frac = nextOutputPos - i0
            for (channel in 0..1) {
                val a = channelAt(i0, channel).toDouble(); val b = channelAt(i1, channel).toDouble()
                val value = (a + (b - a) * frac).roundToInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                if (outCount == out.size) { sink(out, outCount); outCount = 0 }
                out[outCount++] = value
            }
            nextOutputPos += step
        }
        if (outCount > 0) sink(out, outCount)
        previousFrame = ShortArray(2).also { dst ->
            val srcBase = (frames - 1) * sourceChannels
            dst[0] = interleaved[srcBase]
            dst[1] = if (sourceChannels == 1) interleaved[srcBase] else interleaved[srcBase + 1.coerceAtMost(sourceChannels - 1)]
        }
        totalInputFrames += frames
    }
}
