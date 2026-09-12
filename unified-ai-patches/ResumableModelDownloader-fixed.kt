package com.unifiedai.app.data.provider.local

import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile

internal class ResumableModelDownloader(
    private val client: OkHttpClient,
    private val url: String,
    private val expectedSize: Long,
    private val chunkSizeBytes: Long = 8L * 1024L * 1024L,
    private val maxAttemptsPerChunk: Int = 3,
) {
    init {
        require(expectedSize > 0L) { "Beklenen model boyutu 0'dan büyük olmalı" }
        require(chunkSizeBytes > 0L) { "Parça boyutu 0'dan büyük olmalı" }
        require(maxAttemptsPerChunk > 0) { "Deneme sayısı 0'dan büyük olmalı" }
    }

    fun download(target: File, onProgress: (Long) -> Unit = {}) {
        target.parentFile?.mkdirs()
        if (target.exists() && target.length() > expectedSize) {
            check(target.delete()) { "Geçersiz yarım model dosyası temizlenemedi" }
        }
        if (target.length() == expectedSize) {
            onProgress(expectedSize)
            return
        }

        RandomAccessFile(target, "rw").use { output ->
            var offset = output.length()
            onProgress(offset)

            while (offset < expectedSize) {
                val chunkStart = offset
                val chunkEnd = minOf(chunkStart + chunkSizeBytes - 1L, expectedSize - 1L)
                val requestedBytes = chunkEnd - chunkStart + 1L
                var completed = false
                var lastFailure: Exception? = null

                for (attempt in 1..maxAttemptsPerChunk) {
                    output.setLength(chunkStart)
                    output.seek(chunkStart)

                    try {
                        val request = Request.Builder()
                            .url(url)
                            .header("User-Agent", "BirlesikAI/0.5 Android")
                            .header("Range", "bytes=$chunkStart-$chunkEnd")
                            .header("Accept-Encoding", "identity")
                            .header("Cache-Control", "no-cache")
                            .build()

                        client.newCall(request).execute().use { response ->
                            if (response.code != 206) {
                                throw IOException(
                                    "Model sunucusu byte aralığı yerine HTTP ${response.code} döndürdü",
                                )
                            }

                            validateContentRange(
                                header = response.header("Content-Range"),
                                requestedStart = chunkStart,
                                requestedEnd = chunkEnd,
                            )

                            var received = 0L
                            response.body.byteStream().use { input ->
                                val buffer = ByteArray(DEFAULT_BUFFER_SIZE * 8)
                                while (received < requestedBytes) {
                                    val remaining = requestedBytes - received
                                    val read = input.read(
                                        buffer,
                                        0,
                                        minOf(buffer.size.toLong(), remaining).toInt(),
                                    )
                                    if (read < 0) break
                                    if (read == 0) {
                                        throw IOException("Model sunucusu 0 baytlık okuma döndürdü")
                                    }
                                    output.write(buffer, 0, read)
                                    received += read
                                    onProgress(chunkStart + received)
                                }
                            }

                            if (received != requestedBytes) {
                                throw IOException(
                                    "Model parçası eksik indi ($received / $requestedBytes bayt)",
                                )
                            }
                        }

                        offset = chunkEnd + 1L
                        completed = true
                        break
                    } catch (error: Exception) {
                        lastFailure = error
                        output.setLength(chunkStart)
                        output.seek(chunkStart)
                    }
                }

                if (!completed) {
                    throw IOException(
                        "Model parçası $chunkStart-$chunkEnd, $maxAttemptsPerChunk denemede indirilemedi: " +
                            (lastFailure?.message ?: "bilinmeyen hata"),
                        lastFailure,
                    )
                }
            }
        }
    }

    private fun validateContentRange(
        header: String?,
        requestedStart: Long,
        requestedEnd: Long,
    ) {
        val value = header ?: throw IOException("Model sunucusu Content-Range başlığı döndürmedi")
        val match = CONTENT_RANGE.matchEntire(value.trim())
            ?: throw IOException("Geçersiz Content-Range: $value")

        val actualStart = match.groupValues[1].toLongOrNull()
        val actualEnd = match.groupValues[2].toLongOrNull()
        val actualTotal = match.groupValues[3].toLongOrNull()

        if (
            actualStart != requestedStart ||
            actualEnd != requestedEnd ||
            actualTotal != expectedSize
        ) {
            throw IOException(
                "Beklenmeyen model aralığı: $value; beklenen bytes $requestedStart-$requestedEnd/$expectedSize",
            )
        }
    }

    private companion object {
        val CONTENT_RANGE = Regex("^bytes (\\d+)-(\\d+)/(\\d+)$")
    }
}
