package com.emre.gunumplanner

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import java.io.File
import java.util.UUID

object FullRepository {
    data class Subtask(val id: Long, val itemId: Long, val title: String, val done: Boolean, val sortOrder: Int)
    data class SavedPlace(val id: Long, val name: String, val address: String, val lat: Double, val lng: Double, val radius: Float)
    data class TrashItem(
        val trashId: Long,
        val originalId: Long,
        val type: String,
        val title: String,
        val body: String,
        val dayKey: String,
        val dueAt: Long,
        val status: String,
        val topicId: Long,
        val recurrence: String,
        val duration: Int,
        val priority: Int,
        val deletedAt: Long
    )

    fun ensureSchema(db: Db) {
        val sql = db.writableDatabase
        sql.execSQL("CREATE TABLE IF NOT EXISTS subtasks (id INTEGER PRIMARY KEY AUTOINCREMENT,item_id INTEGER NOT NULL,title TEXT NOT NULL,done INTEGER NOT NULL DEFAULT 0,sort_order INTEGER NOT NULL DEFAULT 0,created_at INTEGER NOT NULL,updated_at INTEGER NOT NULL)")
        sql.execSQL("CREATE INDEX IF NOT EXISTS idx_subtasks_item ON subtasks(item_id)")
        sql.execSQL("CREATE TABLE IF NOT EXISTS saved_places (id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL,address TEXT NOT NULL DEFAULT '',lat REAL NOT NULL,lng REAL NOT NULL,radius REAL NOT NULL DEFAULT 200,created_at INTEGER NOT NULL,updated_at INTEGER NOT NULL)")
        sql.execSQL("CREATE TABLE IF NOT EXISTS trash_items (trash_id INTEGER PRIMARY KEY AUTOINCREMENT,original_id INTEGER NOT NULL,type TEXT NOT NULL,title TEXT NOT NULL,body TEXT NOT NULL DEFAULT '',day_key TEXT NOT NULL,due_at INTEGER NOT NULL DEFAULT 0,status TEXT NOT NULL,topic_id INTEGER NOT NULL DEFAULT 0,recurrence TEXT NOT NULL DEFAULT '',duration_min INTEGER NOT NULL DEFAULT 0,priority INTEGER NOT NULL DEFAULT 2,deleted_at INTEGER NOT NULL)")
        sql.execSQL("CREATE INDEX IF NOT EXISTS idx_trash_deleted ON trash_items(deleted_at DESC)")
        sql.execSQL("CREATE TABLE IF NOT EXISTS task_series (item_id INTEGER PRIMARY KEY,series_key TEXT NOT NULL,created_at INTEGER NOT NULL)")
        sql.execSQL("CREATE INDEX IF NOT EXISTS idx_task_series_key ON task_series(series_key)")
        sql.execSQL("CREATE TABLE IF NOT EXISTS focus_sessions (id INTEGER PRIMARY KEY AUTOINCREMENT,item_id INTEGER NOT NULL,started_at INTEGER NOT NULL,ended_at INTEGER NOT NULL,duration_sec INTEGER NOT NULL)")
        ExtrasRepository.ensureSchema(db)
    }

    fun hasAnyItems(db: Db): Boolean {
        ensureSchema(db)
        db.readableDatabase.rawQuery("SELECT 1 FROM items LIMIT 1", null).use { return it.moveToFirst() }
    }

    fun subtasks(db: Db, itemId: Long): List<Subtask> {
        ensureSchema(db)
        val out = mutableListOf<Subtask>()
        db.readableDatabase.rawQuery("SELECT id,item_id,title,done,sort_order FROM subtasks WHERE item_id=? ORDER BY sort_order,id", arrayOf(itemId.toString())).use { c ->
            while (c.moveToNext()) out += Subtask(c.getLong(0), c.getLong(1), c.getString(2), c.getInt(3) == 1, c.getInt(4))
        }
        return out
    }

    fun addSubtask(db: Db, itemId: Long, title: String) {
        ensureSchema(db)
        val now = System.currentTimeMillis()
        var order = 0
        db.readableDatabase.rawQuery("SELECT COALESCE(MAX(sort_order),-1)+1 FROM subtasks WHERE item_id=?", arrayOf(itemId.toString())).use { if (it.moveToFirst()) order = it.getInt(0) }
        val v = ContentValues().apply { put("item_id", itemId); put("title", title.trim()); put("done", 0); put("sort_order", order); put("created_at", now); put("updated_at", now) }
        db.writableDatabase.insert("subtasks", null, v)
    }

    fun toggleSubtask(db: Db, id: Long, done: Boolean) {
        val v = ContentValues().apply { put("done", if (done) 1 else 0); put("updated_at", System.currentTimeMillis()) }
        db.writableDatabase.update("subtasks", v, "id=?", arrayOf(id.toString()))
    }

    fun deleteSubtask(db: Db, id: Long) { db.writableDatabase.delete("subtasks", "id=?", arrayOf(id.toString())) }

    fun savePlace(db: Db, name: String, address: String, lat: Double, lng: Double, radius: Float): Long {
        ensureSchema(db)
        val now = System.currentTimeMillis()
        val v = ContentValues().apply { put("name", name.trim().ifBlank { "Konum" }); put("address", address.trim()); put("lat", lat); put("lng", lng); put("radius", radius); put("created_at", now); put("updated_at", now) }
        return db.writableDatabase.insert("saved_places", null, v)
    }

    fun savedPlaces(db: Db): List<SavedPlace> {
        ensureSchema(db)
        val out = mutableListOf<SavedPlace>()
        db.readableDatabase.rawQuery("SELECT id,name,address,lat,lng,radius FROM saved_places ORDER BY updated_at DESC", null).use { c ->
            while (c.moveToNext()) out += SavedPlace(c.getLong(0), c.getString(1), c.getString(2), c.getDouble(3), c.getDouble(4), c.getFloat(5))
        }
        return out
    }

    fun deletePlace(db: Db, id: Long) { db.writableDatabase.delete("saved_places", "id=?", arrayOf(id.toString())) }

    fun moveToTrash(db: Db, item: Db.Item) {
        ensureSchema(db)
        val v = ContentValues().apply {
            put("original_id", item.id); put("type", item.type); put("title", item.title); put("body", item.body); put("day_key", item.dayKey); put("due_at", item.dueAt); put("status", item.status); put("topic_id", item.topicId); put("recurrence", item.recurrence); put("duration_min", item.durationMinutes); put("priority", item.priority); put("deleted_at", System.currentTimeMillis())
        }
        db.writableDatabase.insert("trash_items", null, v)
        db.deleteItem(item.id)
    }

    fun trash(db: Db): List<TrashItem> {
        ensureSchema(db)
        val out = mutableListOf<TrashItem>()
        db.readableDatabase.rawQuery("SELECT trash_id,original_id,type,title,body,day_key,due_at,status,topic_id,recurrence,duration_min,priority,deleted_at FROM trash_items ORDER BY deleted_at DESC", null).use { c ->
            while (c.moveToNext()) out += TrashItem(c.getLong(0),c.getLong(1),c.getString(2),c.getString(3),c.getString(4),c.getString(5),c.getLong(6),c.getString(7),c.getLong(8),c.getString(9),c.getInt(10),c.getInt(11),c.getLong(12))
        }
        return out
    }

    fun restoreTrash(db: Db, t: TrashItem): Long {
        ensureSchema(db)
        val newId = db.insertItem(t.type,t.title,t.body,t.dayKey,t.dueAt,t.topicId,t.recurrence,t.duration,t.priority)
        if (t.status == Db.STATUS_DONE) db.completeItem(newId)
        db.writableDatabase.execSQL("UPDATE attachments SET item_id=? WHERE item_id=?", arrayOf(newId,t.originalId))
        db.writableDatabase.execSQL("UPDATE location_rules SET item_id=? WHERE item_id=?", arrayOf(newId,t.originalId))
        db.writableDatabase.execSQL("UPDATE subtasks SET item_id=? WHERE item_id=?", arrayOf(newId,t.originalId))
        db.writableDatabase.execSQL("UPDATE task_series SET item_id=? WHERE item_id=?", arrayOf(newId,t.originalId))
        db.writableDatabase.delete("trash_items","trash_id=?",arrayOf(t.trashId.toString()))
        return newId
    }

    fun deleteTrashForever(db: Db, t: TrashItem) {
        ensureSchema(db)
        ExtrasRepository.getAttachments(db,t.originalId).forEach { runCatching { File(it.path).delete() } }
        db.writableDatabase.delete("attachments","item_id=?",arrayOf(t.originalId.toString()))
        db.writableDatabase.delete("location_rules","item_id=?",arrayOf(t.originalId.toString()))
        db.writableDatabase.delete("subtasks","item_id=?",arrayOf(t.originalId.toString()))
        db.writableDatabase.delete("task_series","item_id=?",arrayOf(t.originalId.toString()))
        db.writableDatabase.delete("trash_items","trash_id=?",arrayOf(t.trashId.toString()))
    }

    fun ensureSeries(db: Db, itemId: Long): String {
        ensureSchema(db)
        db.readableDatabase.rawQuery("SELECT series_key FROM task_series WHERE item_id=?", arrayOf(itemId.toString())).use { if (it.moveToFirst()) return it.getString(0) }
        val key = UUID.randomUUID().toString()
        val v = ContentValues().apply { put("item_id", itemId); put("series_key", key); put("created_at", System.currentTimeMillis()) }
        db.writableDatabase.insertWithOnConflict("task_series",null,v,android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE)
        return key
    }

    fun propagateSeries(db: Db, oldId: Long, newId: Long) {
        if (newId <= 0) return
        val key = ensureSeries(db,oldId)
        val v = ContentValues().apply { put("item_id",newId); put("series_key",key); put("created_at",System.currentTimeMillis()) }
        db.writableDatabase.insertWithOnConflict("task_series",null,v,android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun updateSeriesOpen(db: Db, itemId: Long, title: String, body: String, recurrence: String, duration: Int, priority: Int, includeCurrentAndFuture: Boolean) {
        ensureSchema(db)
        val key = ensureSeries(db,itemId)
        val current = db.getItem(itemId) ?: return
        val args = arrayOf(key,current.dayKey)
        val ids = mutableListOf<Long>()
        val sql = if (includeCurrentAndFuture)
            "SELECT ts.item_id FROM task_series ts JOIN items i ON i.id=ts.item_id WHERE ts.series_key=? AND i.status='OPEN' AND i.day_key>=?"
        else "SELECT ts.item_id FROM task_series ts JOIN items i ON i.id=ts.item_id WHERE ts.series_key=? AND i.status='OPEN'"
        db.readableDatabase.rawQuery(sql,args).use { c -> while(c.moveToNext()) ids += c.getLong(0) }
        if (ids.isEmpty()) ids += itemId
        ids.forEach { id ->
            val old = db.getItem(id) ?: return@forEach
            db.updateItem(id,title,body,old.dayKey,old.dueAt,old.topicId,recurrence,duration,priority)
        }
    }

    fun recordFocus(db: Db, itemId: Long, startedAt: Long, endedAt: Long) {
        ensureSchema(db)
        val sec = ((endedAt-startedAt)/1000L).coerceAtLeast(0)
        val v = ContentValues().apply { put("item_id",itemId); put("started_at",startedAt); put("ended_at",endedAt); put("duration_sec",sec) }
        db.writableDatabase.insert("focus_sessions",null,v)
    }

    fun averageFocusMinutes(db: Db, itemId: Long): Int {
        ensureSchema(db)
        db.readableDatabase.rawQuery("SELECT AVG(duration_sec) FROM focus_sessions WHERE item_id=?",arrayOf(itemId.toString())).use { if(it.moveToFirst()&&!it.isNull(0)) return (it.getDouble(0)/60.0).toInt() }
        return 0
    }

    fun allItemsFiltered(db: Db, query: String, type: String?, status: String?, priority: Int?, from: String?, to: String?): List<Db.Item> {
        ensureSchema(db)
        val where = mutableListOf<String>()
        val args = mutableListOf<String>()
        if (query.isNotBlank()) { where += "(title LIKE ? COLLATE NOCASE OR body LIKE ? COLLATE NOCASE)"; args += "%$query%"; args += "%$query%" }
        if (!type.isNullOrBlank()) { where += "type=?"; args += type }
        if (!status.isNullOrBlank()) { where += "status=?"; args += status }
        if (priority != null) { where += "priority=?"; args += priority.toString() }
        if (!from.isNullOrBlank()) { where += "day_key>=?"; args += from }
        if (!to.isNullOrBlank()) { where += "day_key<=?"; args += to }
        val sql = "SELECT id,type,title,body,day_key,due_at,status,topic_id,recurrence,duration_min,priority,created_at,updated_at FROM items" + if(where.isEmpty()) "" else " WHERE "+where.joinToString(" AND ") + " ORDER BY updated_at DESC LIMIT 300"
        val out = mutableListOf<Db.Item>()
        db.readableDatabase.rawQuery(sql,args.toTypedArray()).use { c -> while(c.moveToNext()) out += readItem(c) }
        return out
    }

    private fun readItem(c: Cursor): Db.Item = Db.Item().also { i ->
        i.id=c.getLong(0); i.type=c.getString(1); i.title=c.getString(2); i.body=c.getString(3); i.dayKey=c.getString(4); i.dueAt=c.getLong(5); i.status=c.getString(6); i.topicId=c.getLong(7); i.recurrence=c.getString(8); i.durationMinutes=c.getInt(9); i.priority=c.getInt(10); i.createdAt=c.getLong(11); i.updatedAt=c.getLong(12)
    }

    object Prefs {
        private const val NAME = "gunum_full_prefs"
        fun prefs(c: Context) = c.getSharedPreferences(NAME,Context.MODE_PRIVATE)
        fun theme(c: Context) = prefs(c).getString("theme","system") ?: "system"
        fun setTheme(c: Context,v:String) = prefs(c).edit().putString("theme",v).apply()
        fun defaultDuration(c: Context) = prefs(c).getInt("default_duration",30)
        fun setDefaultDuration(c: Context,v:Int) = prefs(c).edit().putInt("default_duration",v.coerceIn(5,480)).apply()
        fun weekStartsMonday(c: Context) = prefs(c).getBoolean("week_monday",true)
        fun setWeekStartsMonday(c: Context,v:Boolean) = prefs(c).edit().putBoolean("week_monday",v).apply()
        fun autoPlanApproval(c: Context) = prefs(c).getBoolean("plan_approval",true)
        fun setAutoPlanApproval(c: Context,v:Boolean) = prefs(c).edit().putBoolean("plan_approval",v).apply()
        fun onboardingSeen(c: Context) = prefs(c).getBoolean("onboarding_seen",false)
        fun setOnboardingSeen(c: Context,v:Boolean) = prefs(c).edit().putBoolean("onboarding_seen",v).apply()
    }
}
