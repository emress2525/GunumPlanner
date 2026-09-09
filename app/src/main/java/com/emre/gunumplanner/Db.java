package com.emre.gunumplanner;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

public class Db extends SQLiteOpenHelper {
    private static final String DB_NAME = "gunum.db";
    private static final int DB_VERSION = 2;

    public static final String TYPE_TASK = "TASK";
    public static final String TYPE_NOTE = "NOTE";
    public static final String STATUS_OPEN = "OPEN";
    public static final String STATUS_DONE = "DONE";
    private static final String ITEM_COLS = "id,type,title,body,day_key,due_at,status,topic_id,recurrence,duration_min,priority,created_at,updated_at";

    public Db(Context context) { super(context, DB_NAME, null, DB_VERSION); }

    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE topics (id INTEGER PRIMARY KEY AUTOINCREMENT,title TEXT NOT NULL,locked INTEGER NOT NULL DEFAULT 0,auto_group INTEGER NOT NULL DEFAULT 1,created_at INTEGER NOT NULL,updated_at INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE items (id INTEGER PRIMARY KEY AUTOINCREMENT,type TEXT NOT NULL,title TEXT NOT NULL,body TEXT NOT NULL DEFAULT '',day_key TEXT NOT NULL,due_at INTEGER NOT NULL DEFAULT 0,status TEXT NOT NULL DEFAULT 'OPEN',topic_id INTEGER NOT NULL DEFAULT 0,recurrence TEXT NOT NULL DEFAULT '',duration_min INTEGER NOT NULL DEFAULT 0,priority INTEGER NOT NULL DEFAULT 2,created_at INTEGER NOT NULL,updated_at INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE events (id INTEGER PRIMARY KEY AUTOINCREMENT,item_id INTEGER NOT NULL DEFAULT 0,event_type TEXT NOT NULL,day_key TEXT NOT NULL,title_snapshot TEXT NOT NULL DEFAULT '',details TEXT NOT NULL DEFAULT '',created_at INTEGER NOT NULL)");
        db.execSQL("CREATE INDEX idx_items_day ON items(day_key)");
        db.execSQL("CREATE INDEX idx_items_topic ON items(topic_id)");
        db.execSQL("CREATE INDEX idx_items_status ON items(status)");
        db.execSQL("CREATE INDEX idx_events_day ON events(day_key)");
    }

    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE items ADD COLUMN recurrence TEXT NOT NULL DEFAULT ''");
            db.execSQL("ALTER TABLE items ADD COLUMN duration_min INTEGER NOT NULL DEFAULT 0");
            db.execSQL("ALTER TABLE items ADD COLUMN priority INTEGER NOT NULL DEFAULT 2");
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_items_status ON items(status)");
        }
    }

    public long insertItem(String type,String title,String body,String dayKey,long dueAt,long topicId){return insertItem(type,title,body,dayKey,dueAt,topicId,"",0,2);}
    public long insertItem(String type,String title,String body,String dayKey,long dueAt,long topicId,String recurrence,int durationMinutes,int priority){
        long now=System.currentTimeMillis(); ContentValues v=new ContentValues();
        v.put("type",type);v.put("title",title);v.put("body",body==null?"":body);v.put("day_key",dayKey);v.put("due_at",dueAt);v.put("status",STATUS_OPEN);v.put("topic_id",topicId);v.put("recurrence",recurrence==null?"":recurrence);v.put("duration_min",Math.max(0,durationMinutes));v.put("priority",Math.max(0,Math.min(2,priority)));v.put("created_at",now);v.put("updated_at",now);
        long id=getWritableDatabase().insertOrThrow("items",null,v); String detail=dueAt>0?"Hatırlatma ayarlı":""; if(recurrence!=null&&!recurrence.isEmpty())detail+=(detail.isEmpty()?"":" • ")+NaturalLanguageParser.recurrenceLabel(recurrence); logEvent(id,TYPE_NOTE.equals(type)?"NOTE_CREATED":"CREATED",dayKey,title,detail); return id;
    }

    public Item getItem(long id){try(Cursor c=getReadableDatabase().rawQuery("SELECT "+ITEM_COLS+" FROM items WHERE id=?",new String[]{String.valueOf(id)})){if(c.moveToFirst())return readItem(c);}return null;}
    public List<Item> getItemsForDay(String dayKey){return queryItems("SELECT "+ITEM_COLS+" FROM items WHERE day_key=? ORDER BY CASE WHEN due_at=0 THEN 1 ELSE 0 END,due_at,priority,created_at",new String[]{dayKey});}
    public List<Item> getOpenTasksUpTo(String dayKey){return queryItems("SELECT "+ITEM_COLS+" FROM items WHERE type='TASK' AND status='OPEN' AND day_key<=? ORDER BY priority,day_key,CASE WHEN due_at=0 THEN 1 ELSE 0 END,due_at,created_at",new String[]{dayKey});}
    public List<Item> getOpenReminderItems(){return queryItems("SELECT "+ITEM_COLS+" FROM items WHERE type='TASK' AND status='OPEN' AND due_at>? ORDER BY due_at",new String[]{String.valueOf(System.currentTimeMillis())});}
    public List<Item> getNotesForTopic(long topicId){return queryItems("SELECT "+ITEM_COLS+" FROM items WHERE type='NOTE' AND topic_id=? ORDER BY created_at DESC",new String[]{String.valueOf(topicId)});}
    public List<Item> getItemsForTopic(long topicId){return queryItems("SELECT "+ITEM_COLS+" FROM items WHERE topic_id=? ORDER BY created_at DESC",new String[]{String.valueOf(topicId)});}
    public List<Item> searchItems(String query){String q=query==null?"":query.trim();if(q.isEmpty())return new ArrayList<>();String like="%"+q+"%";return queryItems("SELECT "+ITEM_COLS+" FROM items WHERE title LIKE ? COLLATE NOCASE OR body LIKE ? COLLATE NOCASE ORDER BY updated_at DESC LIMIT 100",new String[]{like,like});}
    private List<Item> queryItems(String sql,String[] args){List<Item> out=new ArrayList<>();try(Cursor c=getReadableDatabase().rawQuery(sql,args)){while(c.moveToNext())out.add(readItem(c));}return out;}

    public long completeItem(long id){
        Item item=getItem(id);if(item==null||STATUS_DONE.equals(item.status))return 0;ContentValues v=new ContentValues();v.put("status",STATUS_DONE);v.put("updated_at",System.currentTimeMillis());getWritableDatabase().update("items",v,"id=?",new String[]{String.valueOf(id)});logEvent(id,"COMPLETED",item.dayKey,item.title,"Tamamlandı");
        if(TYPE_TASK.equals(item.type)&&item.recurrence!=null&&!item.recurrence.isEmpty()){
            LocalDate current;try{current=LocalDate.parse(item.dayKey);}catch(Exception e){current=LocalDate.now();} LocalDate next=nextOccurrence(current,item.recurrence),today=LocalDate.now();int guard=0;while(next.isBefore(today)&&guard++<500)next=nextOccurrence(next,item.recurrence);long nextDue=0;
            if(item.dueAt>0){LocalTime time=Instant.ofEpochMilli(item.dueAt).atZone(ZoneId.systemDefault()).toLocalTime();nextDue=LocalDateTime.of(next,time).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();}
            long nextId=insertItem(TYPE_TASK,item.title,item.body,next.toString(),nextDue,item.topicId,item.recurrence,item.durationMinutes,item.priority);logEvent(nextId,"RECUR_CREATED",next.toString(),item.title,"Tekrarlanan görev");return nextId;
        }return 0;
    }

    public void reopenItem(long id){Item item=getItem(id);if(item==null)return;ContentValues v=new ContentValues();v.put("status",STATUS_OPEN);v.put("updated_at",System.currentTimeMillis());getWritableDatabase().update("items",v,"id=?",new String[]{String.valueOf(id)});logEvent(id,"REOPENED",item.dayKey,item.title,"Tekrar açıldı");}
    public void postponeItem(long id,String newDayKey,long newDueAt){Item item=getItem(id);if(item==null)return;String oldDay=item.dayKey;logEvent(id,"POSTPONED",oldDay,item.title,"→ "+newDayKey);ContentValues v=new ContentValues();v.put("day_key",newDayKey);v.put("due_at",newDueAt);v.put("updated_at",System.currentTimeMillis());getWritableDatabase().update("items",v,"id=?",new String[]{String.valueOf(id)});logEvent(id,"MOVED_IN",newDayKey,item.title,"← "+oldDay);}
    public void rescheduleItem(long id,String newDayKey,long newDueAt){Item item=getItem(id);if(item==null)return;String oldDay=item.dayKey;ContentValues v=new ContentValues();v.put("day_key",newDayKey);v.put("due_at",newDueAt);v.put("updated_at",System.currentTimeMillis());getWritableDatabase().update("items",v,"id=?",new String[]{String.valueOf(id)});if(!oldDay.equals(newDayKey)){logEvent(id,"POSTPONED",oldDay,item.title,"Akıllı plan → "+newDayKey);logEvent(id,"MOVED_IN",newDayKey,item.title,"Akıllı plan ← "+oldDay);}else logEvent(id,"SCHEDULED",newDayKey,item.title,"Akıllı plan ile saatlendi");}

    public void updateItem(long id,String title,String body,String dayKey,long dueAt){Item before=getItem(id);if(before==null)return;updateItem(id,title,body,dayKey,dueAt,before.topicId,before.recurrence,before.durationMinutes,before.priority);}
    public void updateItem(long id,String title,String body,String dayKey,long dueAt,long topicId,String recurrence,int durationMinutes,int priority){Item before=getItem(id);if(before==null)return;ContentValues v=new ContentValues();v.put("title",title);v.put("body",body==null?"":body);v.put("day_key",dayKey);v.put("due_at",dueAt);v.put("topic_id",topicId);v.put("recurrence",recurrence==null?"":recurrence);v.put("duration_min",Math.max(0,durationMinutes));v.put("priority",Math.max(0,Math.min(2,priority)));v.put("updated_at",System.currentTimeMillis());getWritableDatabase().update("items",v,"id=?",new String[]{String.valueOf(id)});logEvent(id,"EDITED",dayKey,title,"Düzenlendi");}
    public void deleteItem(long id){Item item=getItem(id);if(item==null)return;logEvent(id,"DELETED",item.dayKey,item.title,"Silindi; geçmiş kaydı korundu");getWritableDatabase().delete("items","id=?",new String[]{String.valueOf(id)});}
    public void moveNoteToTopic(long itemId,long topicId){moveItemToTopic(itemId,topicId);}
    public void moveItemToTopic(long itemId,long topicId){Item item=getItem(itemId);if(item==null)return;ContentValues v=new ContentValues();v.put("topic_id",topicId);v.put("updated_at",System.currentTimeMillis());getWritableDatabase().update("items",v,"id=?",new String[]{String.valueOf(itemId)});Topic t=getTopic(topicId);logEvent(itemId,"TOPIC_MOVED",item.dayKey,item.title,"Konu: "+(t==null?"-":t.title));}

    public long createTopic(String title){long now=System.currentTimeMillis();ContentValues v=new ContentValues();v.put("title",title);v.put("locked",0);v.put("auto_group",1);v.put("created_at",now);v.put("updated_at",now);return getWritableDatabase().insertOrThrow("topics",null,v);}
    public Topic getTopic(long id){try(Cursor c=getReadableDatabase().rawQuery("SELECT id,title,locked,auto_group,created_at,updated_at FROM topics WHERE id=?",new String[]{String.valueOf(id)})){if(c.moveToFirst())return readTopic(c);}return null;}
    public List<Topic> getTopics(){List<Topic> out=new ArrayList<>();try(Cursor c=getReadableDatabase().rawQuery("SELECT id,title,locked,auto_group,created_at,updated_at FROM topics ORDER BY updated_at DESC",null)){while(c.moveToNext())out.add(readTopic(c));}return out;}
    public void renameTopic(long id,String title,boolean lock){ContentValues v=new ContentValues();v.put("title",title);v.put("locked",lock?1:0);v.put("updated_at",System.currentTimeMillis());getWritableDatabase().update("topics",v,"id=?",new String[]{String.valueOf(id)});}
    public void setTopicAutoGroup(long id,boolean enabled){ContentValues v=new ContentValues();v.put("auto_group",enabled?1:0);v.put("updated_at",System.currentTimeMillis());getWritableDatabase().update("topics",v,"id=?",new String[]{String.valueOf(id)});}
    public void mergeTopics(long sourceId,long targetId){if(sourceId==targetId)return;ContentValues v=new ContentValues();v.put("topic_id",targetId);v.put("updated_at",System.currentTimeMillis());getWritableDatabase().update("items",v,"topic_id=?",new String[]{String.valueOf(sourceId)});getWritableDatabase().delete("topics","id=?",new String[]{String.valueOf(sourceId)});ContentValues touch=new ContentValues();touch.put("updated_at",System.currentTimeMillis());getWritableDatabase().update("topics",touch,"id=?",new String[]{String.valueOf(targetId)});}

    public List<Event> getEventsForDay(String dayKey){List<Event> out=new ArrayList<>();try(Cursor c=getReadableDatabase().rawQuery("SELECT id,item_id,event_type,day_key,title_snapshot,details,created_at FROM events WHERE day_key=? ORDER BY created_at DESC",new String[]{dayKey})){while(c.moveToNext())out.add(readEvent(c));}return out;}
    public DayStats getDayStats(String dayKey){DayStats s=new DayStats();try(Cursor c=getReadableDatabase().rawQuery("SELECT type,status,COUNT(*) FROM items WHERE day_key=? GROUP BY type,status",new String[]{dayKey})){while(c.moveToNext()){String type=c.getString(0),status=c.getString(1);int count=c.getInt(2);if(TYPE_NOTE.equals(type))s.notes+=count;else if(TYPE_TASK.equals(type)&&STATUS_DONE.equals(status))s.done+=count;else if(TYPE_TASK.equals(type))s.open+=count;}}try(Cursor c=getReadableDatabase().rawQuery("SELECT COUNT(*) FROM events WHERE day_key=? AND event_type='POSTPONED'",new String[]{dayKey})){if(c.moveToFirst())s.postponed=c.getInt(0);}return s;}
    public String getTopicCorpus(long topicId){StringBuilder sb=new StringBuilder();Topic t=getTopic(topicId);if(t!=null)sb.append(t.title).append(' ');try(Cursor c=getReadableDatabase().rawQuery("SELECT title,body FROM items WHERE topic_id=? ORDER BY created_at DESC LIMIT 30",new String[]{String.valueOf(topicId)})){while(c.moveToNext())sb.append(c.getString(0)).append(' ').append(c.getString(1)).append(' ');}return sb.toString();}
    public int countNotesInTopic(long topicId){return countByTopic(topicId,"type='NOTE'");}
    public int countItemsInTopic(long topicId){return countByTopic(topicId,"1=1");}
    private int countByTopic(long topicId,String extra){try(Cursor c=getReadableDatabase().rawQuery("SELECT COUNT(*) FROM items WHERE topic_id=? AND "+extra,new String[]{String.valueOf(topicId)})){if(c.moveToFirst())return c.getInt(0);}return 0;}

    private LocalDate nextOccurrence(LocalDate current,String rule){if(NaturalLanguageParser.DAILY.equals(rule))return current.plusDays(1);if(NaturalLanguageParser.WEEKLY.equals(rule))return current.plusWeeks(1);if(NaturalLanguageParser.MONTHLY.equals(rule))return current.plusMonths(1);if(NaturalLanguageParser.WEEKDAYS.equals(rule)){LocalDate d=current.plusDays(1);while(d.getDayOfWeek().getValue()>=6)d=d.plusDays(1);return d;}return current.plusDays(1);}
    private void logEvent(long itemId,String type,String dayKey,String title,String details){ContentValues v=new ContentValues();v.put("item_id",itemId);v.put("event_type",type);v.put("day_key",dayKey);v.put("title_snapshot",title==null?"":title);v.put("details",details==null?"":details);v.put("created_at",System.currentTimeMillis());getWritableDatabase().insert("events",null,v);}
    private Item readItem(Cursor c){Item i=new Item();i.id=c.getLong(0);i.type=c.getString(1);i.title=c.getString(2);i.body=c.getString(3);i.dayKey=c.getString(4);i.dueAt=c.getLong(5);i.status=c.getString(6);i.topicId=c.getLong(7);i.recurrence=c.getString(8);i.durationMinutes=c.getInt(9);i.priority=c.getInt(10);i.createdAt=c.getLong(11);i.updatedAt=c.getLong(12);return i;}
    private Topic readTopic(Cursor c){Topic t=new Topic();t.id=c.getLong(0);t.title=c.getString(1);t.locked=c.getInt(2)==1;t.autoGroup=c.getInt(3)==1;t.createdAt=c.getLong(4);t.updatedAt=c.getLong(5);return t;}
    private Event readEvent(Cursor c){Event e=new Event();e.id=c.getLong(0);e.itemId=c.getLong(1);e.eventType=c.getString(2);e.dayKey=c.getString(3);e.titleSnapshot=c.getString(4);e.details=c.getString(5);e.createdAt=c.getLong(6);return e;}

    public static class Item{public long id;public String type,title,body,dayKey,status,recurrence;public long dueAt,topicId,createdAt,updatedAt;public int durationMinutes,priority;}
    public static class Topic{public long id;public String title;public boolean locked,autoGroup;public long createdAt,updatedAt;}
    public static class Event{public long id,itemId,createdAt;public String eventType,dayKey,titleSnapshot,details;}
    public static class DayStats{public int open,done,notes,postponed;}
}
