package com.vocalisolator.app.media

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.provider.OpenableColumns
import com.vocalisolator.app.model.AppError
import com.vocalisolator.app.model.MediaKind
import com.vocalisolator.app.model.SelectedMedia
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface MediaInspector { suspend fun inspect(uri: Uri): Result<SelectedMedia> }

class AndroidMediaInspector(private val context: Context) : MediaInspector {
    override suspend fun inspect(uri: Uri): Result<SelectedMedia> = withContext(Dispatchers.IO) {
        runCatching {
            val resolver = context.contentResolver
            val displayName = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                val i = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (i >= 0 && c.moveToFirst()) c.getString(i) else null
            } ?: uri.lastPathSegment?.substringAfterLast('/') ?: "media"
            val mimeType = resolver.getType(uri).orEmpty()
            val policyKind = MediaInspectorPolicy.kindFor(displayName, mimeType) ?: throw AppError.UnsupportedMedia(displayName)
            val extractor = MediaExtractor()
            try {
                extractor.setDataSource(context, uri, null)
                var hasAudio = false
                var hasVideo = false
                var durationUs = 0L
                for (index in 0 until extractor.trackCount) {
                    val format = extractor.getTrackFormat(index)
                    val trackMime = format.getString(MediaFormat.KEY_MIME).orEmpty()
                    if (trackMime.startsWith("audio/")) hasAudio = true
                    if (trackMime.startsWith("video/")) hasVideo = true
                    if (format.containsKey(MediaFormat.KEY_DURATION)) durationUs = maxOf(durationUs, format.getLong(MediaFormat.KEY_DURATION))
                }
                if (!hasAudio) throw AppError.NoAudioTrack()
                SelectedMedia(uri, displayName, durationUs / 1000L, mimeType, if (hasVideo || policyKind == MediaKind.VIDEO) MediaKind.VIDEO else MediaKind.AUDIO)
            } finally { extractor.release() }
        }.recoverCatching { cause ->
            when (cause) { is AppError -> throw cause; else -> throw AppError.CannotReadMedia(cause) }
        }
    }
}
