package com.vocalisolator.app.media
import com.vocalisolator.app.model.MediaKind
object VideoMuxPolicy {
    fun canCreate(kind: MediaKind, vocalsFileExists: Boolean): Boolean = kind == MediaKind.VIDEO && vocalsFileExists
}
