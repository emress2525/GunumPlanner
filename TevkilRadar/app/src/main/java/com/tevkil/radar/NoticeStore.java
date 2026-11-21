package com.tevkil.radar;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class NoticeStore extends SQLiteOpenHelper {
    private static final Pattern URL = Pattern.compile("https?://[^\\s<>\\\"]+", Pattern.CASE_INSENSITIVE);

    public static final class Notice {
        public long id, time;
        public String source, title, body, url, status;
        public Notice(long id, long time, String source, String title, String body, String url, String status) {
            this.id=id; this.time=time; this.source=source; this.title=title; this.body=body; this.url=url; this.status=status;
        }
    }
    public NoticeStore(Context context) {
        super(context, "tevkil_ilanlar.db", null, 1);
    }
    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE notices (id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "time INTEGER NOT NULL, source TEXT NOT NULL, title TEXT NOT NULL, " +
                "body TEXT NOT NULL, url TEXT NOT NULL, status TEXT NOT NULL DEFAULT 'new', " +
                "fingerprint TEXT NOT NULL UNIQUE)");
        db.execSQL("CREATE INDEX idx_time ON notices(time DESC)");
    }
    @Override public void onUpgrade(SQLiteDatabase db, int from, int to) {}

    public synchronized long add(String source, String title, String body) {
        if (source == null || body == null || body.trim().isEmpty()) return -1;
        long now = System.currentTimeMillis();
        String raw = source + "|" + title + "|" + body + "|" + (now / 86400000L);
        String fingerprint = digest(raw);
        ContentValues v = new ContentValues();
        v.put("time", now); v.put("source", source); v.put("title", title == null ? "" : title);
        v.put("body", body); v.put("url", firstUrl(body)); v.put("fingerprint", fingerprint);
        return getWritableDatabase().insertWithOnConflict("notices", null, v, SQLiteDatabase.CONFLICT_IGNORE);
    }
    /** Stable, remote-ID based deduplication; unlike local notification dedup it does not expire daily. */
    public synchronized long addRemote(String source, String title, String body, String permalink, String remoteId) {
        if (source == null || body == null || remoteId == null || remoteId.trim().isEmpty()) return -1;
        ContentValues v = new ContentValues();
        v.put("time", System.currentTimeMillis()); v.put("source", source);
        v.put("title", title == null ? "" : title); v.put("body", body);
        v.put("url", permalink == null ? "" : permalink);
        v.put("fingerprint", digest("remote|" + source + "|" + remoteId));
        long result = getWritableDatabase().insertWithOnConflict("notices", null, v, SQLiteDatabase.CONFLICT_IGNORE);
        return result > 0 ? result : -1;
    }
    public synchronized List<Notice> list(int limit) {
        List<Notice> result = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery("SELECT id,time,source,title,body,url,status FROM notices ORDER BY time DESC LIMIT ?", new String[]{String.valueOf(limit)})) {
            while (c.moveToNext()) result.add(new Notice(c.getLong(0), c.getLong(1), c.getString(2), c.getString(3), c.getString(4), c.getString(5), c.getString(6)));
        }
        return result;
    }
    public synchronized int count() { return countSql("SELECT COUNT(*) FROM notices"); }
    public synchronized int unread() { return countSql("SELECT COUNT(*) FROM notices WHERE status='new'"); }
    private int countSql(String sql) {
        try (Cursor c = getReadableDatabase().rawQuery(sql, null)) { return c.moveToFirst() ? c.getInt(0) : 0; }
    }
    public synchronized void mark(long id, String status) {
        ContentValues v = new ContentValues(); v.put("status", status);
        getWritableDatabase().update("notices", v, "id=?", new String[]{Long.toString(id)});
    }
    public synchronized void clear() { getWritableDatabase().delete("notices", null, null); }
    public static String firstUrl(String text) {
        if (text == null) return "";
        Matcher matcher = URL.matcher(text);
        return matcher.find() ? matcher.group().replaceAll("[),.;]+$", "") : "";
    }
    private static String digest(String content) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(content.getBytes(StandardCharsets.UTF_8));
            StringBuilder s = new StringBuilder();
            for (byte b : hash) s.append(String.format(Locale.ROOT, "%02x", b & 0xff));
            return s.toString();
        } catch (Exception ex) { return Integer.toHexString(content.hashCode()); }
    }
}
