package com.vocalisolator.app.media
import com.vocalisolator.app.model.MediaKind
import java.util.Locale
object MediaInspectorPolicy {
    private val audioExt = setOf("mp3", "wav", "m4a")
    private val videoExt = setOf("mp4", "mkv", "mov")
    private val audioMime = setOf("audio/mpeg", "audio/mp3", "audio/wav", "audio/x-wav", "audio/mp4", "audio/m4a", "audio/aac")
    private val videoMime = setOf("video/mp4", "video/x-matroska", "video/quicktime")
    fun kindFor(displayName: String, mimeType: String?): MediaKind? {
        val ext = displayName.substringAfterLast('.', "").lowercase(Locale.ROOT)
        val mime = mimeType?.substringBefore(';')?.trim()?.lowercase(Locale.ROOT).orEmpty()
        return when {
            mime in videoMime || ext in videoExt -> MediaKind.VIDEO
            mime in audioMime || ext in audioExt -> MediaKind.AUDIO
            else -> null
        }
    }
}
