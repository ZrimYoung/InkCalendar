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
    EventStore(Context context) { super(context, "calendar.db", null, 1); }
    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE events (_id INTEGER PRIMARY KEY AUTOINCREMENT, day TEXT NOT NULL, title TEXT NOT NULL, time TEXT NOT NULL DEFAULT '')");
        db.execSQL("CREATE INDEX events_day ON events(day, time)");
    }
    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        throw new IllegalStateException("未定义数据库升级路径");
    }
    void save(Event event) {
        ContentValues v = new ContentValues();
        v.put("day", event.date); v.put("title", event.title); v.put("time", event.time);
        if (event.id == 0) getWritableDatabase().insertOrThrow("events", null, v);
        else if (getWritableDatabase().update("events", v, "_id=?", new String[]{Long.toString(event.id)}) != 1) throw new IllegalStateException("事项已不存在");
    }
    void delete(long id) { getWritableDatabase().delete("events", "_id=?", new String[]{Long.toString(id)}); }
    List<Event> between(String start, String end) {
        List<Event> out = new ArrayList<>();
        try (Cursor c = getReadableDatabase().query("events", new String[]{"_id", "day", "title", "time"}, "day>=? AND day<=?", new String[]{start, end}, null, null, "day, time, _id")) {
            while (c.moveToNext()) out.add(new Event(c.getLong(0), c.getString(1), c.getString(2), c.getString(3)));
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
