package com.inkcalendar.app;

import org.junit.BeforeClass;
import org.junit.Test;
import java.io.FileReader;
import java.util.HashSet;
import java.util.Set;
import static org.junit.Assert.*;

public class AlmanacTest {
    static Almanac calendar;
    @BeforeClass public static void load() throws Exception {
        calendar=new Almanac(new FileReader("src/main/assets/calendar/months.csv"),new FileReader("src/main/assets/calendar/terms.csv"));
    }
    @Test public void official2026FestivalDates() {
        // Hong Kong Observatory Gregorian-Lunar conversion table for 2026.
        assertLunar("2026-02-17",2026,1,1);
        assertLunar("2026-06-19",2026,5,5);
        assertLunar("2026-09-25",2026,8,15);
        assertEquals("除夕",calendar.festival("2026-02-16",false));
        assertEquals("Mid-Autumn Festival",calendar.festival("2026-09-25",true));
    }
    @Test public void official2026SolarTerms() {
        String[] dates={"01-05","01-20","02-04","02-18","03-05","03-20","04-05","04-20","05-05","05-21","06-05","06-21","07-07","07-23","08-07","08-23","09-07","09-23","10-08","10-23","11-07","11-22","12-07","12-22"};
        Set<String> names=new HashSet<>();for(String day:dates){String term=calendar.term("2026-"+day);assertFalse(day,term.isEmpty());names.add(term);}
        assertEquals(24,names.size());assertEquals("立冬",calendar.term("2026-11-07"));assertEquals("",calendar.term("2026-11-05"));
    }
    @Test public void leapMonthDoesNotDuplicateFestival() {
        assertLunar("2025-07-25",2025,-6,1);
        assertEquals("闰六月",calendar.lunar("2025-07-25").shortName());
        assertEquals("",calendar.festival("2025-07-29",false));
    }
    @Test public void tricky2033LeapEleventhMonth() {
        assertLunar("2033-12-22",2033,-11,1);
        assertLunar("2034-02-19",2034,1,1);
    }
    @Test public void supportedBoundariesAndLabels() {
        assertNotNull(calendar.lunar("1900-01-01"));assertNotNull(calendar.lunar("2100-12-31"));
        assertNull(calendar.lunar("1899-12-31"));assertNull(calendar.lunar("2101-01-01"));
        for(int day=1;day<=30;day++)assertFalse(new Almanac.LunarDate(2026,1,day,30).dayName().isEmpty());
        assertEquals("初十",new Almanac.LunarDate(2026,1,10,30).dayName());
    }
    @Test public void allDatesAreContinuousAndAllYearsHave24Terms() {
        String date="1900-01-01";Almanac.LunarDate previous=null;int terms=0;
        while(date.compareTo("2100-12-31")<=0) {
            Almanac.LunarDate l=calendar.lunar(date);assertNotNull(date,l);assertTrue(l.day>=1 && l.day<=l.length);
            if(previous!=null) {
                if(previous.day<previous.length){assertEquals(previous.day+1,l.day);assertEquals(previous.month,l.month);assertEquals(previous.year,l.year);}
                else assertEquals(1,l.day);
            }
            if(!calendar.term(date).isEmpty())terms++;
            if(date.endsWith("12-31")){assertEquals(date,24,terms);terms=0;}
            previous=l;date=Dates.addDays(date,1);
        }
    }
    private static void assertLunar(String date,int year,int month,int day) {
        Almanac.LunarDate l=calendar.lunar(date);assertNotNull(l);assertEquals(year,l.year);assertEquals(month,l.month);assertEquals(day,l.day);
    }
}
