package com.vocalisolator.app.model

sealed class AppError(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class UnsupportedMedia(val name: String) : AppError("Unsupported media: $name")
    class CannotReadMedia(cause: Throwable? = null) : AppError("Cannot read media", cause)
    class NoAudioTrack : AppError("The selected media has no audio track")
    class DecoderFailure(cause: Throwable? = null) : AppError("Audio decoding failed", cause)
    class ModelLoadFailure(cause: Throwable? = null) : AppError("AI model could not be loaded", cause)
    class UnsupportedArchitecture : AppError("This build requires a 64-bit ARM device")
    class InsufficientMemory : AppError("Not enough memory to process this media")
    class InsufficientStorage : AppError("Not enough temporary storage")
    class SeparationFailure(cause: Throwable? = null) : AppError("Vocal separation failed", cause)
    class ExportFailure(cause: Throwable? = null) : AppError("Output could not be saved", cause)
    class VideoMuxFailure(cause: Throwable? = null) : AppError("Vocal-only video could not be created", cause)
    class Cancelled : AppError("Processing cancelled")
}
