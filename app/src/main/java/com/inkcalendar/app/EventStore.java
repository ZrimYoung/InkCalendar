package com.inkcalendar.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class EventStore extends SQLiteOpenHelper {
    EventStore(Context context) { super(context, "calendar.db", null, 2); }
    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE events (_id INTEGER PRIMARY KEY AUTOINCREMENT, day TEXT NOT NULL, title TEXT NOT NULL, time TEXT NOT NULL DEFAULT '', completed INTEGER NOT NULL DEFAULT 0)");
        db.execSQL("CREATE INDEX events_day ON events(day, time)");
        createMilestones(db);
    }
    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE events ADD COLUMN completed INTEGER NOT NULL DEFAULT 0");
            createMilestones(db);
        }
    }
    void save(Event event) {
        ContentValues v = new ContentValues();
        v.put("completed", event.completed ? 1 : 0);
        v.put("day", event.date); v.put("title", event.title); v.put("time", event.time);
        if (event.id == 0) getWritableDatabase().insertOrThrow("events", null, v);
        else if (getWritableDatabase().update("events", v, "_id=?", new String[]{Long.toString(event.id)}) != 1) throw new IllegalStateException("事项已不存在");
    }
    private void createMilestones(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE milestones (_id INTEGER PRIMARY KEY AUTOINCREMENT, day TEXT NOT NULL, title TEXT NOT NULL, kind INTEGER NOT NULL, lunar INTEGER NOT NULL DEFAULT 0, pinned INTEGER NOT NULL DEFAULT 1)");
    }
    Event get(long id) {
        try (Cursor c = getReadableDatabase().rawQuery("SELECT _id,day,title,time,completed FROM events WHERE _id=?", new String[]{Long.toString(id)})) {
            return c.moveToFirst() ? new Event(c.getLong(0),c.getString(1),c.getString(2),c.getString(3),c.getInt(4)!=0) : null;
        }
    }
    void complete(long id, boolean completed) {
        ContentValues values = new ContentValues(); values.put("completed", completed ? 1 : 0);
        if (getWritableDatabase().update("events", values, "_id=?", new String[]{Long.toString(id)}) != 1) throw new IllegalStateException("Missing event");
    }
    List<Milestone> milestones() {
        List<Milestone> result = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery("SELECT _id,day,title,kind,lunar,pinned FROM milestones ORDER BY pinned DESC,day,_id", null)) {
            while(c.moveToNext()) result.add(new Milestone(c.getLong(0),c.getString(1),c.getString(2),c.getInt(3),c.getInt(4)!=0,c.getInt(5)!=0));
        }
        return result;
    }
    void saveMilestone(Milestone item) {
        SQLiteDatabase db = getWritableDatabase(); db.beginTransaction();
        try {
            if (item.pinned) try (Cursor c = db.rawQuery("SELECT COUNT(*) FROM milestones WHERE pinned=1 AND _id<>?",new String[]{Long.toString(item.id)})) {
                c.moveToFirst(); if(c.getInt(0)>=2) throw new IllegalArgumentException("pin_limit");
            }
            ContentValues v=new ContentValues();v.put("day",item.date);v.put("title",item.title);v.put("kind",item.kind);v.put("lunar",item.lunar?1:0);v.put("pinned",item.pinned?1:0);
            if(item.id==0) db.insertOrThrow("milestones",null,v);
            else if(db.update("milestones",v,"_id=?",new String[]{Long.toString(item.id)})!=1) throw new IllegalStateException("Missing milestone");
            db.setTransactionSuccessful();
        } finally { db.endTransaction(); }
    }
    void deleteMilestone(long id) { getWritableDatabase().delete("milestones","_id=?",new String[]{Long.toString(id)}); }
    void delete(long id) { getWritableDatabase().delete("events", "_id=?", new String[]{Long.toString(id)}); }
    List<Event> between(String start, String end) {
        List<Event> out = new ArrayList<>();
        try (Cursor c = getReadableDatabase().query("events", new String[]{"_id", "day", "title", "time", "completed"}, "day>=? AND day<=?", new String[]{start, end}, null, null, "day, time, _id")) {
            while (c.moveToNext()) out.add(new Event(c.getLong(0), c.getString(1), c.getString(2), c.getString(3), c.getInt(4) != 0));
        }
        return out;
    }
    Set<String> markedDays(String start, String end) {
        Set<String> out = new HashSet<>();
        try (Cursor c = getReadableDatabase().query(true, "events", new String[]{"day"}, "day>=? AND day<=?", new String[]{start, end}, null, null, null, null)) {
            while (c.moveToNext()) out.add(c.getString(0));
        }
        return out;
    }
}
