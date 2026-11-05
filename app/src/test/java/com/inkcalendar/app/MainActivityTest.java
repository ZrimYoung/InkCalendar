package com.inkcalendar.app;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.shadows.ShadowDialog;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=28,qualifiers="w320dp-h640dp-port")
public class MainActivityTest {
    ActivityController<MainActivity> controller;MainActivity activity;
    @Before public void setup(){
        Context context=RuntimeEnvironment.getApplication();context.deleteDatabase("calendar.db");context.getSharedPreferences("display",0).edit().clear().putString("language","en").commit();
        controller=Robolectric.buildActivity(MainActivity.class).setup();activity=controller.get();
    }
    @After public void close(){if(controller!=null)controller.pause().stop().destroy();}
    private View main(){return activity.getWindow().getDecorView();}
    private View dialog(){List<Dialog> dialogs=ShadowDialog.getShownDialogs();for(int i=dialogs.size()-1;i>=0;i--)if(dialogs.get(i).isShowing())return dialogs.get(i).getWindow().getDecorView();throw new AssertionError("No open dialog");}
    private List<View> all(View root){List<View> result=new ArrayList<>();result.add(root);if(root instanceof ViewGroup)for(int i=0;i<((ViewGroup)root).getChildCount();i++)result.addAll(all(((ViewGroup)root).getChildAt(i)));return result;}
    private View label(View root,String label){for(View v:all(root))if(v instanceof TextView && label.contentEquals(((TextView)v).getText()))return v;throw new AssertionError("Missing text: "+label);}
    private View description(View root,String label){for(View v:all(root))if(label.contentEquals(v.getContentDescription()==null?"":v.getContentDescription()))return v;throw new AssertionError("Missing description: "+label);}
    private void click(View root,String label){View view=label(root,label);while(!view.isClickable() && view.getParent() instanceof View)view=(View)view.getParent();assertTrue("Not clickable: "+label,view.isClickable());view.performClick();}
    private <T> T field(String name)throws Exception{Field f=MainActivity.class.getDeclaredField(name);f.setAccessible(true);return (T)f.get(activity);}
    private EventStore store()throws Exception{return field("store");}
    @Test @Config(sdk={19,28}) public void dateJumpValidatesInputAndSelectsDate()throws Exception {
        description(main(),"Jump to date").performClick();EditText input=(EditText)description(dialog(),"Date · YYYY-MM-DD");
        input.setText("2026-02-30");click(dialog(),"Save");label(dialog(),"Choose a valid date between 1900 and 2100.");
        input.setText("2026-02-17");click(dialog(),"Save");assertEquals("2026-02-17",field("selected"));assertEquals("2026-02-01",field("displayedMonth"));
    }
    @Test public void holidaySwitchAndChineseOnlyAnnotations()throws Exception {
        description(main(),"Settings").performClick();click(dialog(),"简体中文");click(dialog(),"Save");
        description(main(),"跳转日期").performClick();((EditText)description(dialog(),"日期 · 年-月-日")).setText("2026-11-07");click(dialog(),"保存");
        label(main(),"立冬");
        description(main(),"设置").performClick();((CheckBox)label(dialog(),"节假日与调休")).performClick();click(dialog(),"English");click(dialog(),"保存");
        for(View view:all(main()))if(view instanceof TextView)assertFalse(((TextView)view).getText().toString().contains("立冬"));
        assertFalse(activity.getSharedPreferences("display",0).getBoolean("holidays",true));
    }
    @Test public void lunarTermsAndHolidaysHaveIndependentPersistentSwitches()throws Exception {
        description(main(),"Settings").performClick();click(dialog(),"简体中文");click(dialog(),"Save");
        description(main(),"跳转日期").performClick();((EditText)description(dialog(),"日期 · 年-月-日")).setText("2026-11-07");click(dialog(),"保存");
        assertEquals("立冬",((TextView)main().findViewWithTag("almanac:2026-11-07")).getText().toString());
        description(main(),"设置").performClick();click(dialog(),"节气（仅中文）");click(dialog(),"保存");
        assertEquals("廿九",((TextView)main().findViewWithTag("almanac:2026-11-07")).getText().toString());
        description(main(),"设置").performClick();click(dialog(),"农历（仅中文）");click(dialog(),"节气（仅中文）");click(dialog(),"节假日与调休");click(dialog(),"保存");
        assertEquals("立冬",((TextView)main().findViewWithTag("almanac:2026-11-07")).getText().toString());
        assertEquals("",((TextView)main().findViewWithTag("almanac:2026-11-06")).getText().toString());
        description(main(),"设置").performClick();click(dialog(),"English");click(dialog(),"保存");
        assertEquals("",((TextView)main().findViewWithTag("almanac:2026-11-07")).getText().toString());
        assertFalse(activity.getSharedPreferences("display",0).getBoolean("lunar",true));assertTrue(activity.getSharedPreferences("display",0).getBoolean("terms",false));assertFalse(activity.getSharedPreferences("display",0).getBoolean("holidays",true));
        controller.recreate();activity=controller.get();
        description(main(),"Settings").performClick();click(dialog(),"简体中文");click(dialog(),"Save");
        assertEquals("立冬",((TextView)main().findViewWithTag("almanac:2026-11-07")).getText().toString());
    }
    @Test public void completeEditAndFutureRange()throws Exception {
        String today=Dates.today();store().save(new Event(0,today,"First",""));store().save(new Event(0,Dates.addDays(today,15),"Later",""));
        description(main(),"Future events").performClick();label(dialog(),"First");
        click(dialog(),"30 days");label(dialog(),"Later");
        description(dialog(),"Mark complete: First").performClick();assertTrue(store().between(today,today).get(0).completed);
        click(dialog(),"First");((EditText)description(dialog(),"Event")).setText("Edited");click(dialog(),"Save");assertTrue(store().between(today,today).get(0).completed);
        click(dialog(),"Hide completed events");for(View view:all(dialog()))if(view instanceof TextView)assertNotEquals("Edited",((TextView)view).getText().toString());
    }
    @Test public void milestoneCreateEditDeleteAndDisplayMode()throws Exception {
        description(main(),"Milestones").performClick();description(dialog(),"Add milestone").performClick();
        ((EditText)description(dialog(),"Name")).setText("Birthday");click(dialog(),"Save");assertEquals(1,store().milestones().size());
        click(dialog(),"Birthday");((EditText)description(dialog(),"Name")).setText("Renamed");click(dialog(),"Save");assertEquals("Renamed",store().milestones().get(0).title);
        click(dialog(),"Back");description(main(),"Settings").performClick();click(dialog(),"Desk display mode");click(dialog(),"Save");
        assertEquals(View.GONE,((View)field("monthGrid")).getParent() instanceof View?((View)((View)field("monthGrid")).getParent()).getVisibility():-1);
        label(main(),"Renamed");description(main(),"Milestones").performClick();click(dialog(),"Renamed");description(dialog(),"Delete").performClick();click(dialog(),"Cancel");
        description(dialog(),"Delete").performClick();click(dialog(),"Delete");assertTrue(store().milestones().isEmpty());
    }
    @Test public void milestoneDraftSurvivesActivityRecreation()throws Exception {
        description(main(),"Milestones").performClick();description(dialog(),"Add milestone").performClick();((EditText)description(dialog(),"Name")).setText("Unsaved");click(dialog(),"Countdown");
        controller.recreate();activity=controller.get();assertEquals("Unsaved",((EditText)description(dialog(),"Name")).getText().toString());
        assertTrue(((RadioButton)label(dialog(),"Countdown")).isChecked());click(dialog(),"Save");assertEquals(Milestone.COUNTDOWN,store().milestones().get(0).kind);
    }
}
