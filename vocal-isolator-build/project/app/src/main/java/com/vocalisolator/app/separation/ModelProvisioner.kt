package com.vocalisolator.app.separation

import com.vocalisolator.app.model.AppError
import java.io.File
import java.io.InputStream
import java.security.MessageDigest

interface ModelProvisioner {
    suspend fun ensureModelFile(): Result<File>
}

fun interface ModelAssetSource {
    fun open(): InputStream
}

data class ModelSpec(
    val fileName: String,
    val sizeBytes: Long,
    val sha256: String,
)

object ModelFileVerifier {
    fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(1024 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                if (read > 0) digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    fun matches(file: File, spec: ModelSpec): Boolean =
        file.isFile && file.length() == spec.sizeBytes && sha256(file).equals(spec.sha256, ignoreCase = true)
}

class FileModelProvisioner(
    private val modelDir: File,
    private val source: ModelAssetSource,
    private val spec: ModelSpec = ModelSpec(ModelMetadata.FILE_NAME, ModelMetadata.SIZE_BYTES, ModelMetadata.SHA256),
) {
    fun ensureModelFileBlocking(): File {
        modelDir.mkdirs()
        val target = File(modelDir, spec.fileName)
        if (ModelFileVerifier.matches(target, spec)) return target

        val temp = File(modelDir, "${spec.fileName}.tmp")
        temp.delete()
        try {
            source.open().use { input ->
                temp.outputStream().buffered().use { output -> input.copyTo(output, 1024 * 1024) }
            }
            if (!ModelFileVerifier.matches(temp, spec)) {
                throw AppError.ModelLoadFailure(IllegalStateException("Bundled model integrity check failed"))
            }
            if (target.exists() && !target.delete()) {
                throw AppError.ModelLoadFailure(IllegalStateException("Could not replace invalid model"))
            }
            if (!temp.renameTo(target)) {
                temp.copyTo(target, overwrite = true)
                if (!temp.delete()) temp.deleteOnExit()
            }
            if (!ModelFileVerifier.matches(target, spec)) {
                target.delete()
                throw AppError.ModelLoadFailure(IllegalStateException("Provisioned model verification failed"))
            }
            return target
        } finally {
            if (temp.exists()) temp.delete()
        }
    }
}
