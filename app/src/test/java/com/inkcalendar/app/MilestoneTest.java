package com.inkcalendar.app;

import java.io.FileReader;
import java.util.TimeZone;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

public class MilestoneTest {
    static Almanac calendar;
    @BeforeClass public static void load() throws Exception {calendar=new Almanac(new FileReader("src/main/assets/calendar/months.csv"),new FileReader("src/main/assets/calendar/terms.csv"));}
    private Milestone anniversary(String date,boolean lunar){return new Milestone(1,date,"Birthday",0,lunar,true);}
    @Test public void anniversaryTodayAndRollover() {
        Milestone m=anniversary("2020-11-05",false);
        assertEquals("2026-11-05",m.nextDate("2026-11-05",calendar));
        assertEquals(6,m.yearsAt("2026-11-05",calendar));
        assertEquals("2027-11-05",m.nextDate("2026-11-06",calendar));
    }
    @Test public void february29ClampsOnlyInCommonYears() {
        Milestone m=anniversary("2020-02-29",false);
        assertEquals("2026-02-28",m.nextDate("2026-02-01",calendar));
        assertEquals("2028-02-29",m.nextDate("2028-02-01",calendar));
        assertEquals("2027-02-28",m.nextDate("2026-03-01",calendar));
    }
    @Test public void countdownRemainsOnOriginalDayAfterExpiry() {
        Milestone m=new Milestone(1,"2026-11-01","Trip",1,true,false);
        assertEquals("2026-11-01",m.nextDate("2026-11-05",calendar));assertFalse(m.lunar);
        assertEquals(-4,Milestone.distance("2026-11-05",m.date));
    }
    @Test public void futureAnniversaryDoesNotGoBackBeforeAnchor() {
        assertEquals("2030-05-01",anniversary("2030-05-01",false).nextDate("2026-11-05",calendar));
    }
    @Test public void lunarAnniversaryAndLeapMonthFallback() {
        assertEquals("2026-02-17",anniversary("2025-01-29",true).nextDate("2026-01-01",calendar));
        assertEquals("2026-07-14",anniversary("2025-07-25",true).nextDate("2026-01-01",calendar));
    }
    @Test public void lunarDay30ClampsTo29() {
        // 2024 lunar fifth month has 30 days; 2026 fifth month has 29 days.
        assertEquals("2026-07-13",anniversary("2024-07-05",true).nextDate("2026-01-01",calendar));
    }
    @Test public void outOfRangeNextAnniversaryIsExplicit() {
        assertNull(anniversary("2000-01-01",false).nextDate("2100-12-31",calendar));
    }
    @Test public void dayCountingIsTimezoneIndependentAcrossDst() {
        TimeZone saved=TimeZone.getDefault();
        try {TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"));assertEquals(2,Milestone.distance("2026-03-07","2026-03-09"));}
        finally {TimeZone.setDefault(saved);}
    }
    @Test(expected=IllegalArgumentException.class)public void blankNamesRejected(){new Milestone(0,"2026-11-05","  ",0,false,true);}
}
