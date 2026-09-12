package com.unifiedai.app.data.provider.local

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

class OnDeviceModelManager(
    context: Context,
    private val client: OkHttpClient,
) : LocalModelController {
    private val modelDir = File(context.applicationContext.filesDir, "models").apply { mkdirs() }
    private val modelFile = File(modelDir, StarterLocalModel.FILE_NAME)
    private val partialFile = File(modelDir, "${StarterLocalModel.FILE_NAME}.part")
    private val mutableState = MutableStateFlow<LocalModelInstallState>(initialState())
    override val state: StateFlow<LocalModelInstallState> = mutableState.asStateFlow()

    override fun modelPathOrNull(): String? =
        modelFile.takeIf { it.isFile && it.length() == StarterLocalModel.SIZE_BYTES }?.absolutePath

    override suspend fun refresh() = withContext(Dispatchers.IO) {
        mutableState.value = initialState()
    }

    override suspend fun download() = withContext(Dispatchers.IO) {
        if (modelPathOrNull() != null) {
            mutableState.value = LocalModelInstallState.Ready(modelFile.absolutePath)
            return@withContext
        }

        runCatching {
            if (partialFile.length() > StarterLocalModel.SIZE_BYTES) {
                partialFile.delete()
            }

            val downloader = ResumableModelDownloader(
                client = client,
                url = StarterLocalModel.DOWNLOAD_URL,
                expectedSize = StarterLocalModel.SIZE_BYTES,
                chunkSizeBytes = DOWNLOAD_CHUNK_SIZE_BYTES,
                maxAttemptsPerChunk = DOWNLOAD_ATTEMPTS_PER_CHUNK,
            )

            var lastPercent = -1
            downloader.download(partialFile) { downloaded ->
                val percent = ((downloaded * 100L) / StarterLocalModel.SIZE_BYTES)
                    .toInt()
                    .coerceIn(0, 99)
                if (percent != lastPercent) {
                    lastPercent = percent
                    mutableState.value = LocalModelInstallState.Downloading(percent, downloaded)
                }
            }

            require(partialFile.length() == StarterLocalModel.SIZE_BYTES) {
                "Model boyutu doğrulanamadı (${partialFile.length()} / ${StarterLocalModel.SIZE_BYTES})"
            }

            if (!sha256(partialFile).equals(StarterLocalModel.SHA256, ignoreCase = true)) {
                partialFile.delete()
                error("Model bütünlük kontrolü başarısız")
            }

            if (modelFile.exists()) modelFile.delete()
            require(partialFile.renameTo(modelFile)) { "Model dosyası kaydedilemedi" }
            mutableState.value = LocalModelInstallState.Ready(modelFile.absolutePath)
        }.onFailure { error ->
            if (partialFile.length() > StarterLocalModel.SIZE_BYTES) {
                partialFile.delete()
            }
            mutableState.value = LocalModelInstallState.Error(error.message ?: "Model indirilemedi")
        }
    }

    override suspend fun delete() = withContext(Dispatchers.IO) {
        partialFile.delete()
        modelFile.delete()
        mutableState.value = LocalModelInstallState.Missing
    }

    private fun initialState(): LocalModelInstallState =
        modelPathOrNull()?.let(LocalModelInstallState::Ready) ?: LocalModelInstallState.Missing

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE * 4)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private companion object {
        const val DOWNLOAD_CHUNK_SIZE_BYTES = 8L * 1024L * 1024L
        const val DOWNLOAD_ATTEMPTS_PER_CHUNK = 3
    }
}
