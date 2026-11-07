package com.inkcalendar.app;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import android.view.ViewGroup;
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
@Config(sdk={19,28},qualifiers="w320dp-h640dp-port-mdpi")
public class WeeklySettingsTest {
    private ActivityController<MainActivity> controller;
    private MainActivity activity;
    private SharedPreferences prefs;
    @Before public void setup(){Context c=RuntimeEnvironment.getApplication();c.deleteDatabase("calendar.db");prefs=c.getSharedPreferences("display",0);prefs.edit().clear().putString("language","en").commit();controller=Robolectric.buildActivity(MainActivity.class).setup();activity=controller.get();}
    @After public void close(){controller.pause().stop().destroy();RuntimeEnvironment.setQualifiers("w320dp-h640dp-port-mdpi");}
    private void recreate(){controller.recreate();activity=controller.get();}
    private Object field(String name)throws Exception{Field f=MainActivity.class.getDeclaredField(name);f.setAccessible(true);return f.get(activity);}
    private void setField(String name,Object value)throws Exception{Field f=MainActivity.class.getDeclaredField(name);f.setAccessible(true);f.set(activity,value);}
    private void render()throws Exception{java.lang.reflect.Method m=MainActivity.class.getDeclaredMethod("render");m.setAccessible(true);m.invoke(activity);}
    private View main(){return activity.getWindow().getDecorView();}
    private List<View> all(View root){List<View> views=new ArrayList<>();views.add(root);if(root instanceof ViewGroup)for(int i=0;i<((ViewGroup)root).getChildCount();i++)views.addAll(all(((ViewGroup)root).getChildAt(i)));return views;}
    private View action(View root,String desc){for(View v:all(root))if(desc.contentEquals(v.getContentDescription()==null?"":v.getContentDescription()))return v;throw new AssertionError("Missing action "+desc);}
    private View dialog(){List<Dialog> list=ShadowDialog.getShownDialogs();for(int i=list.size()-1;i>=0;i--)if(list.get(i).isShowing())return list.get(i).getWindow().getDecorView();throw new AssertionError("No dialog");}
    private void settings(){action(main(),"Settings").performClick();}
    private void save(){action(dialog(),"Save").performClick();}
    private String path(View node){if(node.getParent() instanceof ViewGroup){ViewGroup parent=(ViewGroup)node.getParent();return path(parent)+"/"+parent.indexOfChild(node);}return "";}
    private void measure(int width)throws Exception{View root=(View)field("root");root.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));root.layout(0,0,width,root.getMeasuredHeight());}

    @Test public void textButtonsClampAndDraftSurvivesRotationWithoutApplying()throws Exception{
        assertEquals("month",DisplayOptions.read(prefs).view);assertEquals("classic",DisplayOptions.read(prefs).layout);
        settings();View panel=dialog();panel.measure(View.MeasureSpec.makeMeasureSpec(320,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));panel.layout(0,0,320,panel.getMeasuredHeight());
        ViewGroup orientations=panel.findViewWithTag("setting:orientation");int rowHeight=orientations.getChildAt(0).getHeight();assertTrue(rowHeight>=48);
        for(int i=1;i<orientations.getChildCount();i++)assertEquals("Multiline choices must share a height",rowHeight,orientations.getChildAt(i).getHeight());
        action(dialog(),"Increase text size").performClick();action(dialog(),"Increase text size").performClick();
        assertEquals("110%",((TextView)dialog().findViewWithTag("setting:scale-value")).getText().toString());assertFalse(prefs.contains("text_scale"));
        recreate();assertEquals("110%",((TextView)dialog().findViewWithTag("setting:scale-value")).getText().toString());
        for(int i=0;i<10;i++)action(dialog(),"Increase text size").performClick();assertFalse(action(dialog(),"Increase text size").isEnabled());
        assertEquals("130%",((TextView)dialog().findViewWithTag("setting:scale-value")).getText().toString());
        for(int i=0;i<12;i++)action(dialog(),"Decrease text size").performClick();assertFalse(action(dialog(),"Decrease text size").isEnabled());
        save();assertEquals(90,prefs.getInt("text_scale",0));settings();action(dialog(),"Increase text size").performClick();action(dialog(),"Cancel").performClick();assertEquals(90,prefs.getInt("text_scale",0));
    }
    @Test public void legacyLargeTextIsKeptUntilUserChangesIt(){prefs.edit().putBoolean("large",true).commit();recreate();settings();assertEquals("118%",((TextView)dialog().findViewWithTag("setting:scale-value")).getText().toString());save();assertEquals(118,prefs.getInt("text_scale",0));}
    @Test public void weekAndLayoutDraftSaveAndCancelTogether(){
        settings();((RadioButton)dialog().findViewWithTag("week")).performClick();((RadioButton)dialog().findViewWithTag("balanced")).performClick();
        recreate();assertTrue(((RadioButton)dialog().findViewWithTag("week")).isChecked());assertTrue(((RadioButton)dialog().findViewWithTag("balanced")).isChecked());
        assertEquals("month",prefs.getString("calendar_view","month"));save();assertEquals("week",prefs.getString("calendar_view",""));assertEquals("balanced",prefs.getString("landscape_layout",""));
        settings();((RadioButton)dialog().findViewWithTag("month")).performClick();((RadioButton)dialog().findViewWithTag("classic")).performClick();action(dialog(),"Cancel").performClick();assertEquals("week",prefs.getString("calendar_view",""));assertEquals("balanced",prefs.getString("landscape_layout",""));
    }
    @Test public void weekOnlyReplacesCalendarForEveryThemeAndOrientation()throws Exception{
        for(String qualifiers:new String[]{"w320dp-h640dp-port-mdpi","w773dp-h572dp-land-mdpi"}){
            RuntimeEnvironment.setQualifiers(qualifiers);
            for(String style:ThemeConfig.STYLES){
                prefs.edit().putString("theme_style",style).putString("calendar_view","month").commit();recreate();
                String identity=path(main().findViewWithTag("home:identity")),agenda=path(main().findViewWithTag("home:agenda")),calendar=path(main().findViewWithTag("home:calendar"));
                String current=((TextView)field("dayNumber")).getText().toString();long monthly=all(main()).stream().filter(v->v instanceof CalendarDayView).count();assertEquals(style,42,monthly);
                prefs.edit().putString("calendar_view","week").commit();recreate();
                assertEquals(style,identity,path(main().findViewWithTag("home:identity")));assertEquals(style,agenda,path(main().findViewWithTag("home:agenda")));assertEquals(style,calendar,path(main().findViewWithTag("home:calendar")));
                assertEquals(style,current,((TextView)field("dayNumber")).getText().toString());assertEquals(style,7,all(main()).stream().filter(v->v instanceof WeekDayView).count());
                action(main(),"Next week").performClick();assertEquals(style,current,((TextView)field("dayNumber")).getText().toString());
            }
        }
    }
    @Test public void equalColumnsAreOptionalAndPortraitKeepsTheOriginalOrder()throws Exception{
        RuntimeEnvironment.setQualifiers("w773dp-h572dp-land-mdpi");prefs.edit().putString("landscape_layout","balanced").commit();recreate();measure(773);
        View calendar=main().findViewWithTag("home:calendar"),identity=main().findViewWithTag("home:identity");assertEquals(calendar.getWidth(),identity.getWidth(),1);
        assertTrue(calendar.getLeft()<((View)identity.getParent()).getLeft());
        RuntimeEnvironment.setQualifiers("w320dp-h640dp-port-mdpi");recreate();ViewGroup root=(ViewGroup)field("root");assertSame(root,main().findViewWithTag("home:identity").getParent());assertSame(root,main().findViewWithTag("home:calendar").getParent());
    }
    @Test public void landscapeMonthFitsTheViewportAndKeepsDatePlacement()throws Exception{
        RuntimeEnvironment.setQualifiers("w773dp-h572dp-land-mdpi");prefs.edit().putInt("text_scale",130).putString("landscape_layout","classic").commit();recreate();
        View decor=main();
        decor.measure(View.MeasureSpec.makeMeasureSpec(773,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(572,View.MeasureSpec.EXACTLY));decor.layout(0,0,773,572);
        int[] before=new int[2];main().findViewWithTag("home:identity").getLocationInWindow(before);
        for(int i=0;i<3;i++){
            decor.getViewTreeObserver().dispatchOnGlobalLayout();
            decor.measure(View.MeasureSpec.makeMeasureSpec(773,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(572,View.MeasureSpec.EXACTLY));decor.layout(0,0,773,572);
        }
        MonthFitLayout calendar=(MonthFitLayout)main().findViewWithTag("home:calendar");int[] at=new int[2],after=new int[2];calendar.getLocationInWindow(at);main().findViewWithTag("home:identity").getLocationInWindow(after);
        assertTrue(calendar.contentScale()<1);assertTrue(calendar.contentScale()>=.55f);assertTrue(at[1]+calendar.getHeight()<=572);
        View surface=calendar.getChildAt(0);assertEquals(calendar.getWidth()-calendar.getPaddingLeft()-calendar.getPaddingRight(),surface.getWidth()*surface.getScaleX(),1);
        assertArrayEquals(before,after);assertEquals(42,all(calendar).stream().filter(v->v instanceof CalendarDayView).count());
        prefs.edit().putString("calendar_view","week").commit();recreate();assertEquals(1,((MonthFitLayout)main().findViewWithTag("home:calendar")).contentScale(),.001);
        RuntimeEnvironment.setQualifiers("w320dp-h640dp-port-mdpi");prefs.edit().putString("calendar_view","month").commit();recreate();assertEquals(1,((MonthFitLayout)main().findViewWithTag("home:calendar")).contentScale(),.001);
    }
    @Test public void selectingDaysKeepsTodayAndDateBoundariesRemainUsable()throws Exception{
        prefs.edit().putString("calendar_view","week").commit();recreate();String number=((TextView)field("dayNumber")).getText().toString();
        String other=Dates.addDays(Dates.weekStart(Dates.today()),Dates.today().equals(Dates.weekStart(Dates.today()))?1:0);main().findViewWithTag(other).performClick();assertEquals(other,field("selected"));assertEquals(number,((TextView)field("dayNumber")).getText().toString());
        setField("selected","1900-01-01");render();assertFalse(action(main(),"Previous week").isEnabled());
        setField("selected","2100-12-31");render();assertFalse(action(main(),"Next week").isEnabled());assertFalse(main().findViewWithTag("2101-01-01").isEnabled());
        action(main(),"Back to today").performClick();assertEquals(Dates.today(),field("selected"));
    }
}
