package com.vocalisolator.app.separation

import com.vocalisolator.app.model.AppError

object NativeStatus {
    const val OK = 0
    const val MODEL_LOAD_FAILED = 1
    const val SEPARATION_FAILED = 2
    const val CANCELLED = 3
    const val INVALID_WAV = 4
}

object NativeStatusMapper {
    fun toError(status: Int): AppError? = when (status) {
        NativeStatus.OK -> null
        NativeStatus.MODEL_LOAD_FAILED -> AppError.ModelLoadFailure()
        NativeStatus.CANCELLED -> AppError.Cancelled()
        NativeStatus.INVALID_WAV -> AppError.DecoderFailure()
        else -> AppError.SeparationFailure()
    }
}
