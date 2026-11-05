package com.inkcalendar.app;

import org.junit.Test;
import java.util.Calendar;
import java.util.TimeZone;
import static org.junit.Assert.*;

public class DatesTest {
    @Test public void leapYearAndYearRollover() {
        assertEquals("2024-02-29", Dates.addDays("2024-02-28", 1));
        assertEquals("2025-03-01", Dates.addDays("2025-02-28", 1));
        assertEquals("2027-01-01", Dates.addDays("2026-12-31", 1));
    }
    @Test public void monthNavigationClampsDay() {
        assertEquals("2024-02-29", Dates.addMonths("2024-01-31", 1));
        assertEquals("2025-02-28", Dates.addMonths("2025-01-31", 1));
        assertEquals("2025-12-01", Dates.addMonths("2026-01-01", -1));
    }
    @Test public void gridIsMondayFirstAndContains42ConsecutiveDays() {
        String[] grid = Dates.grid("2026-11-01");
        assertEquals(42, grid.length);
        assertEquals("2026-10-26", grid[0]);
        assertEquals("2026-12-06", grid[41]);
        for (int i = 1; i < 42; i++) assertEquals(Dates.addDays(grid[i - 1], 1), grid[i]);
        assertEquals("2021-02-01", Dates.grid("2021-02-12")[0]);
    }
    @Test public void boundaryGridsIncludeAdjacentYear() {
        assertEquals("1900-01-01", Dates.grid("1900-01-01")[0]);
        assertEquals("2101-01-09", Dates.grid("2100-12-01")[41]);
    }
    @Test public void rejectsInvalidDatesAndRanges() {
        for (String value : new String[]{"2025-02-29", "2026-13-01", "2026-04-31", "2026-00-10", "2026-11-00", "2026-1-1", "1899-01-01", "2101-01-01"}) {
            try { Dates.parse(value); fail(value); } catch (IllegalArgumentException expected) { }
        }
    }
    @Test public void dateOnlyArithmeticIsStableAcrossDeviceTimeZones() {
        TimeZone original = TimeZone.getDefault();
        try {
            for (String zone : new String[]{"Asia/Shanghai", "America/Los_Angeles", "Pacific/Apia"}) {
                TimeZone.setDefault(TimeZone.getTimeZone(zone));
                assertEquals("2026-11-02", Dates.addDays("2026-11-01", 1));
                assertEquals("四", Dates.weekday("2026-11-05"));
            }
        } finally { TimeZone.setDefault(original); }
    }
    @Test public void midnightSchedulingRespectsDaylightSaving() {
        TimeZone zone = TimeZone.getTimeZone("America/New_York");
        Calendar start = Calendar.getInstance(zone); start.clear(); start.set(2026, Calendar.MARCH, 8, 0, 0, 0);
        assertEquals(23 * 3600000L + 250, Dates.untilNextDay(start.getTimeInMillis(), zone));
        start.clear(); start.set(2026, Calendar.NOVEMBER, 1, 0, 0, 0);
        assertEquals(25 * 3600000L + 250, Dates.untilNextDay(start.getTimeInMillis(), zone));
    }
}
