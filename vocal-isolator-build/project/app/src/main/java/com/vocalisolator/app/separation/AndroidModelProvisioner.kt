package com.vocalisolator.app.separation

import android.content.Context
import com.vocalisolator.app.model.AppError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class AndroidModelProvisioner(private val context: Context) : ModelProvisioner {
    override suspend fun ensureModelFile(): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val dir = File(context.filesDir, "models")
            val source = ModelAssetSource { context.assets.open(ModelMetadata.ASSET_PATH) }
            FileModelProvisioner(dir, source).ensureModelFileBlocking()
        }.recoverCatching { cause ->
            when (cause) {
                is AppError -> throw cause
                else -> throw AppError.ModelLoadFailure(cause)
            }
        }
    }
}
