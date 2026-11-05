package com.inkcalendar.app;

import org.junit.Test;
import static org.junit.Assert.*;

public class EventTest {
    @Test public void allDayAndTimedEventsAreSupported() {
        Event allDay = new Event(0, "2026-11-05", "  读书  ", "");
        assertEquals("读书", allDay.title); assertEquals("", allDay.time);
        assertEquals("09:30", new Event(1, "2026-11-06", "开会", "09:30").time);
        assertEquals("23:59", new Event(2, "2026-11-06", "休息", "23:59").time);
    }
    @Test public void invalidTimesDoNotSilentlyChange() {
        for (String time : new String[]{"24:00", "12:60", "9:30", "-1:00", "hello"}) {
            try { new Event(0, "2026-11-05", "测试", time); fail(time); } catch (IllegalArgumentException expected) { }
        }
    }
    @Test(expected = IllegalArgumentException.class) public void emptyTitleIsRejected() { new Event(0, "2026-11-05", "   ", ""); }
    @Test(expected = IllegalArgumentException.class) public void impossibleDateIsRejected() { new Event(0, "2026-02-30", "测试", ""); }
    @Test(expected = IllegalArgumentException.class) public void longTitleIsRejected() {
        StringBuilder title = new StringBuilder(); for (int i = 0; i < 121; i++) title.append('字');
        new Event(0, "2026-11-05", title.toString(), "");
    }
}
