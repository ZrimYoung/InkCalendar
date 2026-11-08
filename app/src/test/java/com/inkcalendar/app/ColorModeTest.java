package com.inkcalendar.app;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowDialog;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk={19,28},qualifiers="w320dp-h640dp-port-mdpi")
public class ColorModeTest {
    private ActivityController<MainActivity> controller;
    private MainActivity activity;
    private SharedPreferences prefs;
    @Before public void setup(){
        Context c=RuntimeEnvironment.getApplication();c.deleteDatabase("calendar.db");
        prefs=c.getSharedPreferences("display",0);prefs.edit().clear().putString("language","en").commit();
        controller=Robolectric.buildActivity(MainActivity.class).setup();activity=controller.get();
    }
    @After public void close(){controller.pause().stop().destroy();}
    private void recreate(){controller.recreate();activity=controller.get();}
    private View main(){return activity.getWindow().getDecorView();}
    private List<View> all(View root){List<View> views=new ArrayList<>();views.add(root);if(root instanceof ViewGroup)for(int i=0;i<((ViewGroup)root).getChildCount();i++)views.addAll(all(((ViewGroup)root).getChildAt(i)));return views;}
    private View action(View root,String name){for(View v:all(root))if(name.contentEquals(v.getContentDescription()==null?"":v.getContentDescription())||v instanceof TextView&&name.contentEquals(((TextView)v).getText()))return v;throw new AssertionError("Missing action "+name);}
    private View dialog(){List<Dialog> dialogs=ShadowDialog.getShownDialogs();for(int i=dialogs.size()-1;i>=0;i--)if(dialogs.get(i).isShowing())return dialogs.get(i).getWindow().getDecorView();throw new AssertionError("No dialog");}
    private void settings(){action(main(),"Settings").performClick();}
    private void closePage(){action(dialog(),"Close").performClick();}
    private Object field(String name)throws Exception{Field f=MainActivity.class.getDeclaredField(name);f.setAccessible(true);return f.get(activity);}
    private void choose(String mode){((RadioButton)dialog().findViewWithTag(mode)).performClick();}

    @Test public void oldInstallStartsInDayAndChangingModeImmediatelyPersists(){
        assertFalse(prefs.contains("color_mode"));assertEquals(Color.BLACK,InkColors.from(activity).ink);
        settings();choose("night");
        assertEquals("night",prefs.getString("color_mode",""));assertEquals(Color.BLACK,InkColors.from(activity).paper);
        recreate();assertTrue(((RadioButton)dialog().findViewWithTag("night")).isChecked());
        closePage();assertEquals("night",DisplayOptions.read(prefs).colorMode);
        settings();choose("day");assertEquals(Color.WHITE,InkColors.from(activity).paper);closePage();recreate();
        assertEquals("day",prefs.getString("color_mode",""));
    }
    @Test public void savedModeColorsCalendarAndEditorsAndCanReturnToDay()throws Exception{
        EventStore store=(EventStore)field("store");store.save(new Event(0,Dates.today(),"Keep this event","09:30"));
        prefs.edit().putString("theme_style","grid").putInt("text_scale",118).commit();recreate();
        settings();choose("night");closePage();
        assertEquals(Color.WHITE,((TextView)field("dayNumber")).getCurrentTextColor());
        List<View> cells=all(main());boolean sawCalendar=false;
        for(View view:cells)if(view instanceof CalendarDayView){CalendarDayView cell=(CalendarDayView)view;sawCalendar=true;assertEquals(Color.WHITE,cell.number.getCurrentTextColor());}
        assertTrue(sawCalendar);
        action(main(),"Add event").performClick();
        EditText title=(EditText)field("titleInput");assertEquals(Color.WHITE,title.getCurrentTextColor());
        assertEquals(Color.WHITE,title.getCurrentHintTextColor());
        action(dialog(),"Cancel").performClick();
        settings();choose("day");closePage();
        assertEquals(Color.BLACK,((TextView)field("dayNumber")).getCurrentTextColor());
        assertEquals(Color.WHITE,InkColors.from(activity).paper);
        assertEquals(118,prefs.getInt("text_scale",0));assertEquals("grid",prefs.getString("theme_style",""));
        assertEquals("Keep this event",((EventStore)field("store")).between(Dates.today(),Dates.today()).get(0).title);
    }
    @Test public void nightCalendarKeepsSelectedDatesReadableInBothViews(){
        for(String style:new String[]{"editorial","minimal","grid"})for(String view:new String[]{"month","week"}){
            prefs.edit().putString("color_mode","night").putString("theme_style",style).putString("calendar_view",view).commit();recreate();
            boolean selected=false;
            for(View node:all(main()))if(node instanceof CalendarDayView){
                CalendarDayView cell=(CalendarDayView)node;
                boolean inverse=cell.isSelected()&&!"grid".equals(style);
                assertEquals(inverse?Color.BLACK:Color.WHITE,cell.number.getCurrentTextColor());
                selected|=cell.isSelected();
            }
            assertTrue("Selected date must remain visible in "+view,selected);
        }
    }
}
