package com.inkcalendar.app;
import org.junit.Test;
import static org.junit.Assert.*;
public class HolidaysTest {
    @Test public void official2026RestAndWorkTotals() {
        int rest=0,work=0;
        for(String day="2026-01-01";day.compareTo("2026-12-31")<=0;day=Dates.addDays(day,1)) {
            if(Holidays.status(day)==1)rest++;if(Holidays.status(day)==-1)work++;
        }
        assertEquals(33,rest);assertEquals(6,work);
        assertEquals(-1,Holidays.status("2026-02-14"));assertEquals(1,Holidays.status("2026-02-15"));
        assertEquals(1,Holidays.status("2026-02-23"));assertEquals(0,Holidays.status("2026-02-24"));
        assertEquals(-1,Holidays.status("2026-09-20"));assertEquals(-1,Holidays.status("2026-10-10"));
    }
    @Test public void otherYearsDoNotInventWorkdayRules(){assertFalse(Holidays.supported("2027-01-01"));assertEquals(0,Holidays.status("2027-01-01"));}
    @Test public void translationsUseSameDates(){assertEquals("Off",Holidays.label("2026-10-01",true));assertEquals("休",Holidays.label("2026-10-01",false));}
}
