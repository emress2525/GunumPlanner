package com.vocalisolator.app.separation

import android.content.Context
import com.vocalisolator.app.model.AppError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class AndroidModelProvisioner(private val context: Context) : ModelProvisioner {
    override suspend fun ensureModelFile(): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val dir = File(context.filesDir, "models").apply { mkdirs() }
            val target = File(dir, ModelMetadata.FILE_NAME)
            val spec = ModelSpec(ModelMetadata.FILE_NAME, ModelMetadata.SIZE_BYTES, ModelMetadata.SHA256)
            if (ModelFileVerifier.matches(target, spec)) return@runCatching target

            val temp = File(dir, "${ModelMetadata.FILE_NAME}.download")
            temp.delete()
            val connection = (URL(ModelMetadata.SOURCE_URL).openConnection() as HttpURLConnection).apply {
                connectTimeout = 30_000
                readTimeout = 120_000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "Vocal-Isolator-Android/1.0")
            }
            try {
                connection.connect()
                if (connection.responseCode !in 200..299) {
                    throw AppError.ModelLoadFailure(IllegalStateException("Model download HTTP ${connection.responseCode}"))
                }
                connection.inputStream.buffered().use { input ->
                    temp.outputStream().buffered().use { output -> input.copyTo(output, 1024 * 1024) }
                }
            } finally {
                connection.disconnect()
            }

            if (!ModelFileVerifier.matches(temp, spec)) {
                temp.delete()
                throw AppError.ModelLoadFailure(IllegalStateException("Downloaded model integrity check failed"))
            }
            if (target.exists()) target.delete()
            if (!temp.renameTo(target)) {
                temp.copyTo(target, overwrite = true)
                temp.delete()
            }
            if (!ModelFileVerifier.matches(target, spec)) {
                target.delete()
                throw AppError.ModelLoadFailure(IllegalStateException("Installed model verification failed"))
            }
            target
        }.recoverCatching { cause ->
            when (cause) {
                is AppError -> throw cause
                else -> throw AppError.ModelLoadFailure(cause)
            }
        }
    }
}
