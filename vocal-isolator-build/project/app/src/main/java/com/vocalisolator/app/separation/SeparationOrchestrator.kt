package com.vocalisolator.app.separation

import com.vocalisolator.app.media.PcmNormalizer
import com.vocalisolator.app.model.*
import com.vocalisolator.app.output.SessionFiles
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

interface SeparationOrchestrator {
    suspend fun process(media: SelectedMedia, onProgress: (ProcessingProgress) -> Unit = {}): Result<SeparationResult>
    fun cancel()
}

fun interface StorageProbe { fun availableBytes(path: File): Long }

class DefaultSeparationOrchestrator(
    private val cacheDir: File,
    private val normalizer: PcmNormalizer,
    private val modelProvisioner: ModelProvisioner,
    private val engineFactory: (File) -> SeparationEngine,
    private val storageProbe: StorageProbe = StorageProbe { it.usableSpace },
) : SeparationOrchestrator {
    private val cancelled = AtomicBoolean(false)
    @Volatile private var engine: SeparationEngine? = null
    private var session: SessionFiles? = null

    override suspend fun process(media: SelectedMedia, onProgress: (ProcessingProgress) -> Unit): Result<SeparationResult> {
        cancelled.set(false)

        if (!cacheDir.exists() && !cacheDir.mkdirs()) {
            return Result.failure(AppError.InsufficientStorage())
        }

        val needed = StorageRequirements.requiredTempBytes(media.durationMs)
        val available = storageProbe.availableBytes(cacheDir)
        if (available <= 0L || available < needed) {
            return Result.failure(AppError.InsufficientStorage())
        }

        val s = runCatching { SessionFiles.create(cacheDir) }
            .getOrElse { return Result.failure(AppError.InsufficientStorage()) }

        session = s
        var success = false

        fun check() {
            if (cancelled.get()) throw AppError.Cancelled()
        }

        fun emit(stage: ProcessingStage, p: Float, message: String) {
            check()
            onProgress(ProcessingProgress(stage, p.coerceIn(0f, 1f), message))
        }

        return try {
            emit(ProcessingStage.PREPARING, 0f, "Preparing")
            val normalized = normalizer.normalize(media, s.normalizedDir) { p ->
                emit(ProcessingStage.PREPARING, p * 0.15f, "Preparing audio")
            }.getOrThrow()

            check()

            val model = modelProvisioner.ensureModelFile().getOrThrow()
            val e = engineFactory(model)
            engine = e
            e.initialize().getOrThrow()

            emit(ProcessingStage.SEPARATING, 0.15f, "Separating vocals")
            val result = e.separate(normalized, s.stemsDir) { p, msg ->
                emit(
                    ProcessingStage.SEPARATING,
                    0.15f + p * 0.75f,
                    msg.ifBlank { "Separating vocals" },
                )
            }.getOrThrow()

            emit(ProcessingStage.EXPORTING, 1f, "Finished")
            success = true
            Result.success(result)
        } catch (oom: OutOfMemoryError) {
            Result.failure(AppError.InsufficientMemory())
        } catch (t: Throwable) {
            Result.failure(
                if (cancelled.get()) AppError.Cancelled()
                else if (t is AppError) t
                else AppError.SeparationFailure(t)
            )
        } finally {
            engine?.release()
            engine = null
            if (!success) s.cleanup()
        }
    }

    override fun cancel() {
        cancelled.set(true)
        engine?.cancel()
    }

    fun cleanupCompletedSession() {
        session?.cleanup()
        session = null
    }
}
