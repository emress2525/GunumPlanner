package com.emre.gunumplanner

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object ExtrasRepository {
    data class Attachment(
        val id: Long,
        val itemId: Long,
        val path: String,
        val mime: String,
        val name: String,
        val kind: String,
        val createdAt: Long
    )

    data class LocationRule(
        val itemId: Long,
        val label: String,
        val address: String,
        val lat: Double,
        val lng: Double,
        val radius: Float,
        val transition: String,
        val enabled: Boolean,
        val createdAt: Long,
        val updatedAt: Long
    )

    data class PendingAttachment(
        val path: String,
        val mime: String,
        val name: String,
        val kind: String
    )

    fun ensureSchema(db: Db) {
        val sql = db.writableDatabase
        sql.execSQL(
            "CREATE TABLE IF NOT EXISTS attachments (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "item_id INTEGER NOT NULL," +
                "path TEXT NOT NULL," +
                "mime TEXT NOT NULL DEFAULT 'application/octet-stream'," +
                "name TEXT NOT NULL DEFAULT ''," +
                "kind TEXT NOT NULL DEFAULT 'FILE'," +
                "created_at INTEGER NOT NULL)"
        )
        sql.execSQL("CREATE INDEX IF NOT EXISTS idx_attachments_item ON attachments(item_id)")
        sql.execSQL(
            "CREATE TABLE IF NOT EXISTS location_rules (" +
                "item_id INTEGER PRIMARY KEY," +
                "label TEXT NOT NULL DEFAULT ''," +
                "address TEXT NOT NULL DEFAULT ''," +
                "lat REAL NOT NULL," +
                "lng REAL NOT NULL," +
                "radius REAL NOT NULL DEFAULT 200," +
                "transition TEXT NOT NULL DEFAULT 'ENTER'," +
                "enabled INTEGER NOT NULL DEFAULT 1," +
                "created_at INTEGER NOT NULL," +
                "updated_at INTEGER NOT NULL)"
        )
    }

    fun addAttachment(db: Db, itemId: Long, pending: PendingAttachment): Long {
        ensureSchema(db)
        val now = System.currentTimeMillis()
        val v = ContentValues().apply {
            put("item_id", itemId)
            put("path", pending.path)
            put("mime", pending.mime)
            put("name", pending.name)
            put("kind", pending.kind)
            put("created_at", now)
        }
        return db.writableDatabase.insertOrThrow("attachments", null, v)
    }

    fun getAttachments(db: Db, itemId: Long): List<Attachment> {
        ensureSchema(db)
        val out = mutableListOf<Attachment>()
        db.readableDatabase.rawQuery(
            "SELECT id,item_id,path,mime,name,kind,created_at FROM attachments WHERE item_id=? ORDER BY created_at DESC",
            arrayOf(itemId.toString())
        ).use { c ->
            while (c.moveToNext()) {
                out += Attachment(
                    c.getLong(0), c.getLong(1), c.getString(2), c.getString(3),
                    c.getString(4), c.getString(5), c.getLong(6)
                )
            }
        }
        return out
    }

    fun deleteAttachment(db: Db, attachment: Attachment) {
        ensureSchema(db)
        runCatching { File(attachment.path).delete() }
        db.writableDatabase.delete("attachments", "id=?", arrayOf(attachment.id.toString()))
    }

    fun saveLocation(db: Db, rule: LocationRule) {
        ensureSchema(db)
        val now = System.currentTimeMillis()
        val existing = getLocation(db, rule.itemId)
        val v = ContentValues().apply {
            put("item_id", rule.itemId)
            put("label", rule.label)
            put("address", rule.address)
            put("lat", rule.lat)
            put("lng", rule.lng)
            put("radius", rule.radius)
            put("transition", rule.transition)
            put("enabled", if (rule.enabled) 1 else 0)
            put("created_at", existing?.createdAt ?: now)
            put("updated_at", now)
        }
        db.writableDatabase.insertWithOnConflict("location_rules", null, v, android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun getLocation(db: Db, itemId: Long): LocationRule? {
        ensureSchema(db)
        db.readableDatabase.rawQuery(
            "SELECT item_id,label,address,lat,lng,radius,transition,enabled,created_at,updated_at FROM location_rules WHERE item_id=?",
            arrayOf(itemId.toString())
        ).use { c ->
            if (!c.moveToFirst()) return null
            return LocationRule(
                c.getLong(0), c.getString(1), c.getString(2), c.getDouble(3), c.getDouble(4),
                c.getFloat(5), c.getString(6), c.getInt(7) == 1, c.getLong(8), c.getLong(9)
            )
        }
    }

    fun getEnabledLocations(db: Db): List<LocationRule> {
        ensureSchema(db)
        val out = mutableListOf<LocationRule>()
        db.readableDatabase.rawQuery(
            "SELECT item_id,label,address,lat,lng,radius,transition,enabled,created_at,updated_at FROM location_rules WHERE enabled=1",
            null
        ).use { c ->
            while (c.moveToNext()) {
                out += LocationRule(
                    c.getLong(0), c.getString(1), c.getString(2), c.getDouble(3), c.getDouble(4),
                    c.getFloat(5), c.getString(6), c.getInt(7) == 1, c.getLong(8), c.getLong(9)
                )
            }
        }
        return out
    }

    fun deleteLocation(db: Db, itemId: Long) {
        ensureSchema(db)
        db.writableDatabase.delete("location_rules", "item_id=?", arrayOf(itemId.toString()))
    }

    fun copyUriToPrivateStorage(context: Context, uri: Uri, kind: String): PendingAttachment? {
        val resolver = context.contentResolver
        val originalName = queryDisplayName(context, uri).ifBlank { "dosya" }
        val mime = resolver.getType(uri) ?: guessMime(originalName)
        val ext = extensionFor(originalName, mime)
        val dir = File(context.filesDir, "attachments").apply { mkdirs() }
        val target = File(dir, UUID.randomUUID().toString() + if (ext.isBlank()) "" else ".$ext")
        return runCatching {
            resolver.openInputStream(uri)?.use { input ->
                FileOutputStream(target).use { output -> input.copyTo(output) }
            } ?: return null
            PendingAttachment(target.absolutePath, mime, originalName, kind)
        }.getOrNull()
    }

    fun cameraFile(context: Context): File {
        val dir = File(context.filesDir, "attachments").apply { mkdirs() }
        return File(dir, "IMG_${System.currentTimeMillis()}_${UUID.randomUUID()}.jpg")
    }

    fun cameraPending(file: File): PendingAttachment =
        PendingAttachment(file.absolutePath, "image/jpeg", file.name, "PHOTO")

    private fun queryDisplayName(context: Context, uri: Uri): String {
        return runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) c.getString(0) ?: "" else ""
            } ?: ""
        }.getOrDefault("")
    }

    private fun extensionFor(name: String, mime: String): String {
        val fromName = name.substringAfterLast('.', "").lowercase()
        if (fromName.length in 1..8) return fromName
        return MimeTypeMap.getSingleton().getExtensionFromMimeType(mime) ?: ""
    }

    private fun guessMime(name: String): String {
        val ext = name.substringAfterLast('.', "").lowercase()
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "application/octet-stream"
    }
}
