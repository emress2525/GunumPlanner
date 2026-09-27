package com.vocalisolator.app.cloud

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.delay
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

enum class CloudQuality { FAST, BALANCED, BEST }

data class CloudResult(
    val vocals: File,
    val instrumental: File,
)

class StemSplitClient(
    private val context: Context,
    private val apiKey: String,
) {
    private val baseUrl = "https://stemsplit.io/api/v1"

    suspend fun separate(
        uri: Uri,
        quality: CloudQuality,
        onProgress: (Float, String) -> Unit,
    ): CloudResult {
        val input = materialize(uri)
        try {
            onProgress(0.03f, "Dosya hazırlanıyor")
            val upload = postJson(
                "$baseUrl/upload",
                JSONObject().put("filename", input.name),
            )
            val uploadUrl = upload.getString("uploadUrl")
            val uploadKey = upload.getString("uploadKey")

            putFile(uploadUrl, input) { sent, total ->
                val p = if (total > 0) sent.toFloat() / total.toFloat() else 0f
                onProgress(0.05f + p.coerceIn(0f, 1f) * 0.25f, "Sunucuya yükleniyor")
            }

            onProgress(0.31f, "AI işlemi başlatılıyor")
            val job = postJson(
                "$baseUrl/jobs",
                JSONObject()
                    .put("uploadKey", uploadKey)
                    .put("outputType", "BOTH")
                    .put("quality", quality.name)
                    .put("outputFormat", "MP3"),
            )
            val jobId = job.getString("id")

            var vocalsUrl: String? = null
            var instrumentalUrl: String? = null

            while (true) {
                val status = getJson("$baseUrl/jobs/$jobId")
                val state = status.optString("status")
                val serverProgress = status.optInt("progress", 0).coerceIn(0, 100)
                onProgress(
                    0.32f + (serverProgress / 100f) * 0.55f,
                    when (state) {
                        "PENDING" -> "Sırada bekliyor"
                        "PROCESSING" -> "AI vokali ayırıyor"
                        "COMPLETED" -> "Sonuçlar hazırlanıyor"
                        else -> "İşleniyor"
                    },
                )

                when (state) {
                    "COMPLETED" -> {
                        val outputs = status.getJSONObject("outputs")
                        vocalsUrl = outputs.getJSONObject("vocals").getString("url")
                        instrumentalUrl = outputs.getJSONObject("instrumental").getString("url")
                        break
                    }
                    "FAILED", "CANCELLED", "ERROR" -> {
                        val message = status.optString("error")
                            .ifBlank { status.optString("message") }
                            .ifBlank { "Sunucu ayırma işlemini tamamlayamadı" }
                        error(message)
                    }
                }
                delay(2_000)
            }

            val outDir = File(context.cacheDir, "cloud-results").apply { mkdirs() }
            val vocals = File(outDir, input.nameWithoutExtension + "_vocals.mp3")
            val instrumental = File(outDir, input.nameWithoutExtension + "_instrumental.mp3")

            onProgress(0.88f, "Vokal indiriliyor")
            download(vocalsUrl!!, vocals) { done, total ->
                val p = if (total > 0) done.toFloat() / total.toFloat() else 0f
                onProgress(0.88f + p.coerceIn(0f, 1f) * 0.055f, "Vokal indiriliyor")
            }
            onProgress(0.94f, "Enstrümantal indiriliyor")
            download(instrumentalUrl!!, instrumental) { done, total ->
                val p = if (total > 0) done.toFloat() / total.toFloat() else 0f
                onProgress(0.94f + p.coerceIn(0f, 1f) * 0.06f, "Enstrümantal indiriliyor")
            }

            onProgress(1f, "Tamamlandı")
            return CloudResult(vocals, instrumental)
        } finally {
            input.delete()
        }
    }

    private fun materialize(uri: Uri): File {
        val resolver = context.contentResolver
        var name = "input.bin"
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) {
                val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) name = c.getString(idx) ?: name
            }
        }
        val safe = name.replace(Regex("[^A-Za-z0-9._ -]"), "_")
        val file = File(context.cacheDir, "upload_${System.currentTimeMillis()}_$safe")
        resolver.openInputStream(uri)?.use { input ->
            file.outputStream().buffered().use { output ->
                input.copyTo(output, 1024 * 1024)
            }
        } ?: error("Dosya açılamadı")
        return file
    }

    private fun postJson(url: String, body: JSONObject): JSONObject {
        val conn = open(url)
        try {
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            val bytes = body.toString().toByteArray(Charsets.UTF_8)
            conn.setFixedLengthStreamingMode(bytes.size)
            conn.outputStream.use { it.write(bytes) }
            return readJson(conn)
        } finally {
            conn.disconnect()
        }
    }

    private fun getJson(url: String): JSONObject {
        val conn = open(url)
        try {
            conn.requestMethod = "GET"
            return readJson(conn)
        } finally {
            conn.disconnect()
        }
    }

    private fun open(url: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 30_000
            readTimeout = 60_000
            setRequestProperty("Authorization", "Bearer $apiKey")
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "VocalIsolator-Android/2")
        }

    private fun readJson(conn: HttpURLConnection): JSONObject {
        val code = conn.responseCode
        val text = (if (code in 200..299) conn.inputStream else conn.errorStream)
            ?.bufferedReader()
            ?.use { it.readText() }
            .orEmpty()
        if (code !in 200..299) {
            val message = runCatching {
                JSONObject(text).optString("message").ifBlank {
                    JSONObject(text).optString("error")
                }
            }.getOrNull().orEmpty()
            error(
                when (code) {
                    401, 403 -> "API anahtarı geçersiz veya yetkisiz"
                    402 -> "Sunucu kredisi yetersiz"
                    413 -> "Dosya sunucu sınırından büyük"
                    429 -> "Sunucu yoğun. Biraz sonra tekrar dene"
                    else -> message.ifBlank { "Sunucu hatası: HTTP $code" }
                }
            )
        }
        return JSONObject(text)
    }

    private fun putFile(url: String, file: File, progress: (Long, Long) -> Unit) {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "PUT"
            doOutput = true
            connectTimeout = 30_000
            readTimeout = 120_000
            setFixedLengthStreamingMode(file.length())
            setRequestProperty("Content-Type", contentType(file.name))
        }
        try {
            val total = file.length()
            file.inputStream().buffered().use { input ->
                conn.outputStream.buffered().use { output ->
                    val buffer = ByteArray(1024 * 1024)
                    var sent = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        sent += read
                        progress(sent, total)
                    }
                }
            }
            val code = conn.responseCode
            if (code !in 200..299) error("Dosya yüklenemedi: HTTP $code")
        } finally {
            conn.disconnect()
        }
    }

    private fun download(url: String, out: File, progress: (Long, Long) -> Unit) {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 30_000
            readTimeout = 120_000
        }
        try {
            val code = conn.responseCode
            if (code !in 200..299) error("Sonuç indirilemedi: HTTP $code")
            val total = conn.contentLengthLong
            conn.inputStream.buffered().use { input ->
                out.outputStream().buffered().use { output ->
                    val buffer = ByteArray(1024 * 1024)
                    var done = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        done += read
                        progress(done, total)
                    }
                }
            }
        } finally {
            conn.disconnect()
        }
    }

    private fun contentType(name: String): String = when (name.substringAfterLast('.', "").lowercase()) {
        "mp3" -> "audio/mpeg"
        "wav" -> "audio/wav"
        "m4a" -> "audio/mp4"
        "mp4" -> "video/mp4"
        "mov" -> "video/quicktime"
        "mkv" -> "video/x-matroska"
        else -> "application/octet-stream"
    }
}
