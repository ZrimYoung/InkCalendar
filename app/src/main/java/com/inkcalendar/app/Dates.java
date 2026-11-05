package com.inkcalendar.app;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

/** Date-only keys are independent of the device time zone. */
final class Dates {
    private Dates() {}
    static String key(Calendar c) {
        return String.format(Locale.US, "%04d-%02d-%02d", c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH));
    }
    static Calendar parse(String value) {
        if (value == null || !value.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")) throw new IllegalArgumentException("日期格式应为 YYYY-MM-DD");
        int year = Integer.parseInt(value.substring(0, 4));
        if (year < 1900 || year > 2100) throw new IllegalArgumentException("日期范围为 1900—2100 年");
        Calendar c = new GregorianCalendar(TimeZone.getTimeZone("UTC"), Locale.US);
        c.clear();
        c.setLenient(false);
        c.set(year, Integer.parseInt(value.substring(5, 7)) - 1, Integer.parseInt(value.substring(8, 10)), 12, 0, 0);
        c.getTimeInMillis();
        return c;
    }
    static String today() { return key(Calendar.getInstance()); }
    static String month(String date) { return date.substring(0, 7) + "-01"; }
    static String addDays(String date, int days) {
        Calendar c = parse(date); c.add(Calendar.DAY_OF_MONTH, days); return key(c);
    }
    static String addMonths(String date, int months) {
        Calendar c = parse(date); c.add(Calendar.MONTH, months); return key(c);
    }
    static String[] grid(String month) {
        Calendar c = parse(month(month));
        int offset = (c.get(Calendar.DAY_OF_WEEK) + 5) % 7;
        c.add(Calendar.DAY_OF_MONTH, -offset);
        String[] days = new String[42];
        for (int i = 0; i < days.length; i++) { days[i] = key(c); c.add(Calendar.DAY_OF_MONTH, 1); }
        return days;
    }
    static String weekday(String date) { return new String[]{"日", "一", "二", "三", "四", "五", "六"}[parse(date).get(Calendar.DAY_OF_WEEK) - 1]; }
    static String label(String date) { return Integer.parseInt(date.substring(5, 7)) + "月" + Integer.parseInt(date.substring(8, 10)) + "日"; }
    static long untilNextDay(long now, TimeZone zone) {
        Calendar c = Calendar.getInstance(zone);
        c.setTimeInMillis(now);
        c.add(Calendar.DAY_OF_MONTH, 1);
        c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0); c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0);
        return Math.max(1000L, c.getTimeInMillis() - now + 250L);
    }
}
