package com.inkcalendar.app;

final class Event {
    final long id;
    final boolean completed;
    final String date, title, time;
    Event(long id, String date, String title, String time) {
        this(id, date, title, time, false);
    }
    Event(long id, String date, String title, String time, boolean completed) {
        this.completed = completed;
        Dates.parse(date);
        if (title == null || title.trim().isEmpty()) throw new IllegalArgumentException("title_required");
        if (title.trim().length() > 120) throw new IllegalArgumentException("title_too_long");
        if (time == null) time = "";
        time = time.trim();
        if (!time.isEmpty() && !time.matches("([01][0-9]|2[0-3]):[0-5][0-9]")) throw new IllegalArgumentException("time_invalid");
        this.id = id; this.date = date; this.title = title.trim(); this.time = time;
    }
}
