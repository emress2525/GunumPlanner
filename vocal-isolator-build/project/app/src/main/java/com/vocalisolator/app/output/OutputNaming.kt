package com.vocalisolator.app.output
object OutputNaming {
    private val unsafe = Regex("[/\\\\:*?\"<>|]")
    fun stem(displayName: String): String {
        val safe = displayName.replace(unsafe, "_")
        val lastDot = safe.lastIndexOf('.')
        val base = if (lastDot > 0) safe.substring(0, lastDot) else safe
        return base.trim('_', ' ', '.').ifBlank { "output" }
    }
    fun vocals(displayName: String) = "${stem(displayName)}_vocals.wav"
    fun instrumental(displayName: String) = "${stem(displayName)}_instrumental.wav"
    fun vocalVideo(displayName: String) = "${stem(displayName)}_vocals_video.mp4"
}
