package com.vocalisolator.app.output
import java.io.File
import java.util.UUID

class SessionFiles private constructor(val root: File) {
    val normalizedDir = File(root, "normalized")
    val stemsDir = File(root, "stems")
    val videoDir = File(root, "video")
    init {
        require(root.mkdirs() || root.isDirectory)
        require(normalizedDir.mkdirs() || normalizedDir.isDirectory)
        require(stemsDir.mkdirs() || stemsDir.isDirectory)
        require(videoDir.mkdirs() || videoDir.isDirectory)
    }
    fun cleanup() { root.deleteRecursively() }
    companion object {
        fun create(baseCacheDir: File): SessionFiles {
            require(baseCacheDir.mkdirs() || baseCacheDir.isDirectory)
            return SessionFiles(File(baseCacheDir, "vocal_session_${UUID.randomUUID()}"))
        }
    }
}
