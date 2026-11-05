package com.inkcalendar.app;

import java.util.Calendar;
import java.util.Locale;

/** A countdown has one target date; an anniversary keeps its original anchor. */
final class Milestone {
    static final int ANNIVERSARY=0, COUNTDOWN=1;
    final long id;
    final String date, title;
    final int kind;
    final boolean lunar, pinned;
    Milestone(long id, String date, String title, int kind, boolean lunar, boolean pinned) {
        Event validated=new Event(0,date,title,"");
        if(kind!=ANNIVERSARY && kind!=COUNTDOWN) throw new IllegalArgumentException("kind_invalid");
        this.id=id;this.date=date;this.title=validated.title;this.kind=kind;this.lunar=lunar && kind==ANNIVERSARY;this.pinned=pinned;
    }
    static long distance(String from,String to) { return (Dates.parse(to).getTimeInMillis()-Dates.parse(from).getTimeInMillis())/86400000L; }
    String nextDate(String today, Almanac almanac) {
        if(kind==COUNTDOWN || date.compareTo(today)>=0) return date;
        if(lunar) return almanac.nextAnniversary(date,today);
        int year=Integer.parseInt(today.substring(0,4));
        for(int y=year;y<=2100;y++) {
            Calendar c=Dates.parse(String.format(Locale.US,"%04d-%s-01",y,date.substring(5,7)));
            c.set(Calendar.DAY_OF_MONTH,Math.min(Integer.parseInt(date.substring(8)),c.getActualMaximum(Calendar.DAY_OF_MONTH)));
            String candidate=Dates.key(c); if(candidate.compareTo(today)>=0) return candidate;
        }
        return null;
    }
    int yearsAt(String occurrence, Almanac almanac) {
        return lunar ? almanac.lunar(occurrence).year-almanac.lunar(date).year
            : Integer.parseInt(occurrence.substring(0,4))-Integer.parseInt(date.substring(0,4));
    }
}
