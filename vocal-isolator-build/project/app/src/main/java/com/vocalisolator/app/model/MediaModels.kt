package com.vocalisolator.app.model

import android.net.Uri

enum class MediaKind { AUDIO, VIDEO }

data class SelectedMedia(
    val uri: Uri,
    val displayName: String,
    val durationMs: Long,
    val mimeType: String,
    val kind: MediaKind,
)
