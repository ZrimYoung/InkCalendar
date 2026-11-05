package com.inkcalendar.app;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=28)
public class EventStoreTest {
    Context context; EventStore store;
    @Before public void setup(){context=RuntimeEnvironment.getApplication();context.deleteDatabase("calendar.db");}
    @After public void close(){if(store!=null)store.close();context.deleteDatabase("calendar.db");}
    @Test @Config(sdk={19,28}) public void upgrade034KeepsEveryExistingFieldAndDefaultsIncomplete() {
        SQLiteDatabase old=context.openOrCreateDatabase("calendar.db",Context.MODE_PRIVATE,null);
        old.execSQL("CREATE TABLE events (_id INTEGER PRIMARY KEY AUTOINCREMENT, day TEXT NOT NULL, title TEXT NOT NULL, time TEXT NOT NULL DEFAULT '')");
        old.execSQL("CREATE INDEX events_day ON events(day,time)");
        old.execSQL("INSERT INTO events VALUES(41,'2026-11-05','原来的事项','09:30')");old.setVersion(1);old.close();
        store=new EventStore(context);Event event=store.get(41);
        assertEquals("原来的事项",event.title);assertEquals("09:30",event.time);assertEquals("2026-11-05",event.date);assertFalse(event.completed);
        assertEquals(2,store.getReadableDatabase().getVersion());assertTrue(store.milestones().isEmpty());
        store.complete(41,true);store.close();store=new EventStore(context);assertTrue(store.get(41).completed);
        store.save(new Event(41,event.date,"Edited",event.time,true));assertTrue(store.get(41).completed);
    }
    @Test public void eventCompletionAndOrderingPersist(){
        store=new EventStore(context);store.save(new Event(0,"2026-11-05","Timed","12:00"));store.save(new Event(0,"2026-11-05","All day",""));
        assertEquals("All day",store.between("2026-11-05","2026-11-05").get(0).title);
        long id=store.between("2026-11-05","2026-11-05").get(0).id;store.complete(id,true);assertTrue(store.get(id).completed);
        store.complete(id,false);assertFalse(store.get(id).completed);store.delete(id);assertNull(store.get(id));
    }
    @Test public void pinLimitRollsBackAndAllowsEditingPinnedRecord(){
        store=new EventStore(context);
        store.saveMilestone(new Milestone(0,"2020-01-01","One",0,false,true));
        store.saveMilestone(new Milestone(0,"2020-02-01","Two",0,true,true));
        try{store.saveMilestone(new Milestone(0,"2030-01-01","Three",1,false,true));fail();}catch(IllegalArgumentException expected){assertEquals("pin_limit",expected.getMessage());}
        assertEquals(2,store.milestones().size());
        Milestone first=store.milestones().get(0);store.saveMilestone(new Milestone(first.id,first.date,"Renamed",0,false,true));
        assertEquals("Renamed",store.milestones().get(0).title);
        store.saveMilestone(new Milestone(first.id,first.date,"Renamed",0,false,false));
        store.saveMilestone(new Milestone(0,"2030-01-01","Three",1,false,true));assertEquals(3,store.milestones().size());
        store.close();store=new EventStore(context);assertEquals(3,store.milestones().size());
        store.deleteMilestone(first.id);assertEquals(2,store.milestones().size());
    }
}
