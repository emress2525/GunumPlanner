package com.vocalisolator.app.media
import java.io.Closeable
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
class Pcm16WavWriter(file: File, private val sampleRateHz: Int = 44_100, private val channelCount: Int = 2) : Closeable {
    private val raf = RandomAccessFile(file, "rw")
    private var dataBytes = 0L
    init { raf.setLength(0); raf.write(ByteArray(44)) }
    fun write(samples: ShortArray, count: Int = samples.size) {
        if (count <= 0) return
        val bytes = ByteBuffer.allocate(count * 2).order(ByteOrder.LITTLE_ENDIAN)
        for (i in 0 until count) bytes.putShort(samples[i])
        raf.write(bytes.array()); dataBytes += count * 2L
    }
    override fun close() {
        raf.seek(0)
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
        header.put("RIFF".toByteArray(Charsets.US_ASCII)); header.putInt((36L + dataBytes).coerceAtMost(Int.MAX_VALUE.toLong()).toInt())
        header.put("WAVE".toByteArray(Charsets.US_ASCII)); header.put("fmt ".toByteArray(Charsets.US_ASCII)); header.putInt(16); header.putShort(1)
        header.putShort(channelCount.toShort()); header.putInt(sampleRateHz); header.putInt(sampleRateHz * channelCount * 2)
        header.putShort((channelCount * 2).toShort()); header.putShort(16); header.put("data".toByteArray(Charsets.US_ASCII))
        header.putInt(dataBytes.coerceAtMost(Int.MAX_VALUE.toLong()).toInt())
        raf.write(header.array()); raf.close()
    }
}
