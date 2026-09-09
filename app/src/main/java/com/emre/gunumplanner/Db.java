package com.emre.gunumplanner;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class Db extends SQLiteOpenHelper {
    private static final String DB_NAME = "gunum.db";
    private static final int DB_VERSION = 1;

    public static final String TYPE_TASK = "TASK";
    public static final String TYPE_NOTE = "NOTE";
    public static final String STATUS_OPEN = "OPEN";
    public static final String STATUS_DONE = "DONE";

    public Db(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE topics (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "title TEXT NOT NULL," +
                "locked INTEGER NOT NULL DEFAULT 0," +
                "auto_group INTEGER NOT NULL DEFAULT 1," +
                "created_at INTEGER NOT NULL," +
                "updated_at INTEGER NOT NULL)");

        db.execSQL("CREATE TABLE items (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "type TEXT NOT NULL," +
                "title TEXT NOT NULL," +
                "body TEXT NOT NULL DEFAULT ''," +
                "day_key TEXT NOT NULL," +
                "due_at INTEGER NOT NULL DEFAULT 0," +
                "status TEXT NOT NULL DEFAULT 'OPEN'," +
                "topic_id INTEGER NOT NULL DEFAULT 0," +
                "created_at INTEGER NOT NULL," +
                "updated_at INTEGER NOT NULL)");

        db.execSQL("CREATE TABLE events (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "item_id INTEGER NOT NULL DEFAULT 0," +
                "event_type TEXT NOT NULL," +
                "day_key TEXT NOT NULL," +
                "title_snapshot TEXT NOT NULL DEFAULT ''," +
                "details TEXT NOT NULL DEFAULT ''," +
                "created_at INTEGER NOT NULL)");

        db.execSQL("CREATE INDEX idx_items_day ON items(day_key)");
        db.execSQL("CREATE INDEX idx_items_topic ON items(topic_id)");
        db.execSQL("CREATE INDEX idx_events_day ON events(day_key)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Future migrations will go here.
    }

    public long insertItem(String type, String title, String body, String dayKey, long dueAt, long topicId) {
        long now = System.currentTimeMillis();
        ContentValues v = new ContentValues();
        v.put("type", type);
        v.put("title", title);
        v.put("body", body == null ? "" : body);
        v.put("day_key", dayKey);
        v.put("due_at", dueAt);
        v.put("status", STATUS_OPEN);
        v.put("topic_id", topicId);
        v.put("created_at", now);
        v.put("updated_at", now);
        long id = getWritableDatabase().insertOrThrow("items", null, v);
        logEvent(id, type.equals(TYPE_NOTE) ? "NOTE_CREATED" : "CREATED", dayKey, title, dueAt > 0 ? "Hatırlatma ayarlı" : "");
        return id;
    }

    public Item getItem(long id) {
        try (Cursor c = getReadableDatabase().rawQuery("SELECT id,type,title,body,day_key,due_at,status,topic_id,created_at,updated_at FROM items WHERE id=?", new String[]{String.valueOf(id)})) {
            if (c.moveToFirst()) return readItem(c);
        }
        return null;
    }

    public List<Item> getItemsForDay(String dayKey) {
        List<Item> out = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT id,type,title,body,day_key,due_at,status,topic_id,created_at,updated_at FROM items WHERE day_key=? ORDER BY CASE WHEN due_at=0 THEN 1 ELSE 0 END, due_at, created_at",
                new String[]{dayKey})) {
            while (c.moveToNext()) out.add(readItem(c));
        }
        return out;
    }

    public List<Item> getOpenReminderItems() {
        List<Item> out = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT id,type,title,body,day_key,due_at,status,topic_id,created_at,updated_at FROM items WHERE type='TASK' AND status='OPEN' AND due_at>? ORDER BY due_at",
                new String[]{String.valueOf(System.currentTimeMillis())})) {
            while (c.moveToNext()) out.add(readItem(c));
        }
        return out;
    }

    public List<Item> getNotesForTopic(long topicId) {
        List<Item> out = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT id,type,title,body,day_key,due_at,status,topic_id,created_at,updated_at FROM items WHERE type='NOTE' AND topic_id=? ORDER BY created_at DESC",
                new String[]{String.valueOf(topicId)})) {
            while (c.moveToNext()) out.add(readItem(c));
        }
        return out;
    }

    public void completeItem(long id) {
        Item item = getItem(id);
        if (item == null) return;
        ContentValues v = new ContentValues();
        v.put("status", STATUS_DONE);
        v.put("updated_at", System.currentTimeMillis());
        getWritableDatabase().update("items", v, "id=?", new String[]{String.valueOf(id)});
        logEvent(id, "COMPLETED", item.dayKey, item.title, "Tamamlandı");
    }

    public void reopenItem(long id) {
        Item item = getItem(id);
        if (item == null) return;
        ContentValues v = new ContentValues();
        v.put("status", STATUS_OPEN);
        v.put("updated_at", System.currentTimeMillis());
        getWritableDatabase().update("items", v, "id=?", new String[]{String.valueOf(id)});
        logEvent(id, "REOPENED", item.dayKey, item.title, "Tekrar açıldı");
    }

    public void postponeItem(long id, String newDayKey, long newDueAt) {
        Item item = getItem(id);
        if (item == null) return;
        String oldDay = item.dayKey;
        logEvent(id, "POSTPONED", oldDay, item.title, "→ " + newDayKey);
        ContentValues v = new ContentValues();
        v.put("day_key", newDayKey);
        v.put("due_at", newDueAt);
        v.put("updated_at", System.currentTimeMillis());
        getWritableDatabase().update("items", v, "id=?", new String[]{String.valueOf(id)});
        logEvent(id, "MOVED_IN", newDayKey, item.title, "← " + oldDay);
    }

    public void updateItem(long id, String title, String body, String dayKey, long dueAt) {
        Item before = getItem(id);
        if (before == null) return;
        ContentValues v = new ContentValues();
        v.put("title", title);
        v.put("body", body == null ? "" : body);
        v.put("day_key", dayKey);
        v.put("due_at", dueAt);
        v.put("updated_at", System.currentTimeMillis());
        getWritableDatabase().update("items", v, "id=?", new String[]{String.valueOf(id)});
        logEvent(id, "EDITED", dayKey, title, "Düzenlendi");
    }

    public void deleteItem(long id) {
        Item item = getItem(id);
        if (item == null) return;
        logEvent(id, "DELETED", item.dayKey, item.title, "Silindi; geçmiş kaydı korundu");
        getWritableDatabase().delete("items", "id=?", new String[]{String.valueOf(id)});
    }

    public void moveNoteToTopic(long itemId, long topicId) {
        Item item = getItem(itemId);
        if (item == null) return;
        ContentValues v = new ContentValues();
        v.put("topic_id", topicId);
        v.put("updated_at", System.currentTimeMillis());
        getWritableDatabase().update("items", v, "id=?", new String[]{String.valueOf(itemId)});
        Topic t = getTopic(topicId);
        logEvent(itemId, "TOPIC_MOVED", item.dayKey, item.title, "Konu: " + (t == null ? "-" : t.title));
    }

    public long createTopic(String title) {
        long now = System.currentTimeMillis();
        ContentValues v = new ContentValues();
        v.put("title", title);
        v.put("locked", 0);
        v.put("auto_group", 1);
        v.put("created_at", now);
        v.put("updated_at", now);
        return getWritableDatabase().insertOrThrow("topics", null, v);
    }

    public Topic getTopic(long id) {
        try (Cursor c = getReadableDatabase().rawQuery("SELECT id,title,locked,auto_group,created_at,updated_at FROM topics WHERE id=?", new String[]{String.valueOf(id)})) {
            if (c.moveToFirst()) return readTopic(c);
        }
        return null;
    }

    public List<Topic> getTopics() {
        List<Topic> out = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery("SELECT id,title,locked,auto_group,created_at,updated_at FROM topics ORDER BY updated_at DESC", null)) {
            while (c.moveToNext()) out.add(readTopic(c));
        }
        return out;
    }

    public void renameTopic(long id, String title, boolean lock) {
        ContentValues v = new ContentValues();
        v.put("title", title);
        v.put("locked", lock ? 1 : 0);
        v.put("updated_at", System.currentTimeMillis());
        getWritableDatabase().update("topics", v, "id=?", new String[]{String.valueOf(id)});
    }

    public void setTopicAutoGroup(long id, boolean enabled) {
        ContentValues v = new ContentValues();
        v.put("auto_group", enabled ? 1 : 0);
        v.put("updated_at", System.currentTimeMillis());
        getWritableDatabase().update("topics", v, "id=?", new String[]{String.valueOf(id)});
    }

    public void mergeTopics(long sourceId, long targetId) {
        if (sourceId == targetId) return;
        ContentValues v = new ContentValues();
        v.put("topic_id", targetId);
        v.put("updated_at", System.currentTimeMillis());
        getWritableDatabase().update("items", v, "topic_id=?", new String[]{String.valueOf(sourceId)});
        getWritableDatabase().delete("topics", "id=?", new String[]{String.valueOf(sourceId)});
        ContentValues touch = new ContentValues();
        touch.put("updated_at", System.currentTimeMillis());
        getWritableDatabase().update("topics", touch, "id=?", new String[]{String.valueOf(targetId)});
    }

    public List<Event> getEventsForDay(String dayKey) {
        List<Event> out = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT id,item_id,event_type,day_key,title_snapshot,details,created_at FROM events WHERE day_key=? ORDER BY created_at DESC",
                new String[]{dayKey})) {
            while (c.moveToNext()) {
                Event e = new Event();
                e.id = c.getLong(0);
                e.itemId = c.getLong(1);
                e.eventType = c.getString(2);
                e.dayKey = c.getString(3);
                e.titleSnapshot = c.getString(4);
                e.details = c.getString(5);
                e.createdAt = c.getLong(6);
                out.add(e);
            }
        }
        return out;
    }

    public String getTopicCorpus(long topicId) {
        StringBuilder sb = new StringBuilder();
        Topic t = getTopic(topicId);
        if (t != null) sb.append(t.title).append(' ');
        try (Cursor c = getReadableDatabase().rawQuery("SELECT title,body FROM items WHERE type='NOTE' AND topic_id=? ORDER BY created_at DESC LIMIT 20", new String[]{String.valueOf(topicId)})) {
            while (c.moveToNext()) sb.append(c.getString(0)).append(' ').append(c.getString(1)).append(' ');
        }
        return sb.toString();
    }

    public int countNotesInTopic(long topicId) {
        try (Cursor c = getReadableDatabase().rawQuery("SELECT COUNT(*) FROM items WHERE type='NOTE' AND topic_id=?", new String[]{String.valueOf(topicId)})) {
            if (c.moveToFirst()) return c.getInt(0);
        }
        return 0;
    }

    private void logEvent(long itemId, String type, String dayKey, String title, String details) {
        ContentValues v = new ContentValues();
        v.put("item_id", itemId);
        v.put("event_type", type);
        v.put("day_key", dayKey);
        v.put("title_snapshot", title == null ? "" : title);
        v.put("details", details == null ? "" : details);
        v.put("created_at", System.currentTimeMillis());
        getWritableDatabase().insert("events", null, v);
    }

    private Item readItem(Cursor c) {
        Item i = new Item();
        i.id = c.getLong(0);
        i.type = c.getString(1);
        i.title = c.getString(2);
        i.body = c.getString(3);
        i.dayKey = c.getString(4);
        i.dueAt = c.getLong(5);
        i.status = c.getString(6);
        i.topicId = c.getLong(7);
        i.createdAt = c.getLong(8);
        i.updatedAt = c.getLong(9);
        return i;
    }

    private Topic readTopic(Cursor c) {
        Topic t = new Topic();
        t.id = c.getLong(0);
        t.title = c.getString(1);
        t.locked = c.getInt(2) == 1;
        t.autoGroup = c.getInt(3) == 1;
        t.createdAt = c.getLong(4);
        t.updatedAt = c.getLong(5);
        return t;
    }

    public static class Item {
        public long id;
        public String type;
        public String title;
        public String body;
        public String dayKey;
        public long dueAt;
        public String status;
        public long topicId;
        public long createdAt;
        public long updatedAt;
    }

    public static class Topic {
        public long id;
        public String title;
        public boolean locked;
        public boolean autoGroup;
        public long createdAt;
        public long updatedAt;
    }

    public static class Event {
        public long id;
        public long itemId;
        public String eventType;
        public String dayKey;
        public String titleSnapshot;
        public String details;
        public long createdAt;
    }
}
