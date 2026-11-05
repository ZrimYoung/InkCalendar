package com.inkcalendar.app;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.Spanned;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.TextView;
import org.junit.Before;
import org.junit.After;
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
public class ThemeSettingsTest {
    ActivityController<MainActivity> controller;MainActivity activity;SharedPreferences prefs;
    @Before public void setup(){Context c=RuntimeEnvironment.getApplication();c.deleteDatabase("calendar.db");prefs=c.getSharedPreferences("display",0);prefs.edit().clear().putString("language","en").commit();controller=Robolectric.buildActivity(MainActivity.class).setup();activity=controller.get();}
    @After public void close(){controller.pause().stop().destroy();}
    private View main(){return activity.getWindow().getDecorView();}
    private List<View> all(View root){List<View> out=new ArrayList<>();out.add(root);if(root instanceof ViewGroup)for(int i=0;i<((ViewGroup)root).getChildCount();i++)out.addAll(all(((ViewGroup)root).getChildAt(i)));return out;}
    private View desc(View root,String name){for(View v:all(root))if(name.contentEquals(v.getContentDescription()==null?"":v.getContentDescription()))return v;throw new AssertionError("Missing action: "+name);}
    private View text(View root,String name){for(View v:all(root))if(v instanceof TextView&&name.contentEquals(((TextView)v).getText()))return v;throw new AssertionError("Missing label: "+name);}
    private View dialog(){List<Dialog> dialogs=ShadowDialog.getShownDialogs();for(int i=dialogs.size()-1;i>=0;i--)if(dialogs.get(i).isShowing())return dialogs.get(i).getWindow().getDecorView();throw new AssertionError("No dialog");}
    private Object field(String name)throws Exception{Field field=MainActivity.class.getDeclaredField(name);field.setAccessible(true);return field.get(activity);}
    private void settings(){desc(main(),"Settings").performClick();}
    private void appearance(){desc(dialog(),"Appearance and themes").performClick();}
    private void save(){desc(dialog(),"Save").performClick();}
    @Test public void themeAndFontAreIndependentAndPersistWithLatestFeatures()throws Exception{
        settings();appearance();assertEquals(6,all(dialog()).stream().filter(v->v instanceof ThemePreview).count());
        assertEquals(7,all(dialog()).stream().filter(v->v instanceof RadioButton&&v.getTag() instanceof String).count());
        desc(dialog(),"Geist").performClick();desc(dialog(),"Ticket").performClick();save();save();
        assertEquals("geist",prefs.getString("theme_font",""));assertEquals("ticket",prefs.getString("theme_style",""));
        ThemeConfig config=(ThemeConfig)field("theme");assertTrue(config.sans());assertTrue(config.is("calendar","plain"));
        desc(main(),"Future events").performClick();text(dialog(),"30 days").performClick();text(dialog(),"Back").performClick();
        desc(main(),"Milestones").performClick();text(dialog(),"Back").performClick();controller.recreate();activity=controller.get();
        assertEquals("geist",((ThemeConfig)field("theme")).font);assertEquals(30,prefs.getInt("future_days",7));
    }
    @Test public void nestedCancelAndOuterCancelDoNotApplyDrafts(){
        settings();appearance();desc(dialog(),"Quiet Space").performClick();desc(dialog(),"Geist").performClick();desc(dialog(),"Cancel").performClick();save();
        assertEquals("original",ThemeConfig.read(prefs).style);assertEquals("original",ThemeConfig.read(prefs).font);
        settings();appearance();desc(dialog(),"Ticket").performClick();save();desc(dialog(),"Cancel").performClick();assertEquals("original",ThemeConfig.read(prefs).style);
    }
    @Test public void componentDraftAndFontSurviveRecreationAndSave()throws Exception{
        settings();appearance();desc(dialog(),"Geist").performClick();text(dialog(),"Customize components").performClick();desc(dialog(),"Date on right").performClick();
        controller.recreate();activity=controller.get();assertTrue(((ThemeConfig)field("componentThemeDraft")).is("date","reverse"));
        save();save();save();ThemeConfig saved=ThemeConfig.read(prefs);assertEquals("geist",saved.font);assertTrue(saved.is("date","reverse"));
        assertEquals("original",saved.style);assertFalse(saved.overrides.isEmpty());
    }
    @Test public void eventDotHasItsOwnSlotInEveryPreset()throws Exception{
        EventStore store=(EventStore)field("store");String day=Dates.today();store.save(new Event(0,day,"Marker · 事项点",""));
        for(String style:ThemeConfig.STYLES){
            prefs.edit().putString("theme_style",style).putString("theme_font","geist").putBoolean("large",true).commit();controller.recreate();activity=controller.get();
            View root=(View)field("root");root.measure(View.MeasureSpec.makeMeasureSpec(320,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));root.layout(0,0,320,root.getMeasuredHeight());
            CalendarDayView cell=root.findViewWithTag(day);assertNotNull(style,cell);assertEquals(View.VISIBLE,cell.marker.getVisibility());assertEquals(4,cell.marker.getWidth());assertEquals(4,cell.marker.getHeight());
            assertTrue(style,cell.marker.getTop()>=cell.number.getBottom()+4);assertTrue(style,cell.detail.getTop()>=cell.marker.getBottom()+2);assertTrue(style,cell.detail.getBottom()<=cell.getHeight());
            assertFalse(cell.number.getText().toString().contains("·"));desc(main(),"Add event");desc(main(),"Future events");desc(main(),"Milestones");
        }
    }
    @Test public void pairingUsesActualDifferentFontsAndPreservesMixedText(){
        ThemeFonts fonts=new ThemeFonts(activity.getAssets());assertSame(fonts.chinese("original"),fonts.chinese("fraunces"));assertSame(fonts.chinese("geist"),fonts.chinese("space"));assertNotSame(fonts.chinese("original"),fonts.chinese("geist"));
        for(String id:ThemeConfig.FONTS){assertNotNull(fonts.latin(id,false));assertNotNull(fonts.latin(id,true));}
        String original="Project 项目 09:30";Spanned mixed=(Spanned)ThemeFonts.mixed(original,fonts.chinese("geist"),false);assertEquals(original,mixed.toString());
        ThemeFonts.ChineseSpan[] spans=mixed.getSpans(0,mixed.length(),ThemeFonts.ChineseSpan.class);assertEquals(1,spans.length);assertEquals("项目",mixed.subSequence(mixed.getSpanStart(spans[0]),mixed.getSpanEnd(spans[0])).toString());
    }
    @Test public void trackingRetainsReadableWordsAndHasDifferentWidths(){
        String heading="Ink Calendar 墨日历";Spanned spaced=(Spanned)TextTracking.apply(heading,.09f),tight=(Spanned)TextTracking.apply(heading,-.035f);
        assertEquals(heading,spaced.toString());assertEquals(2,spaced.getSpans(0,spaced.length(),TextTracking.Word.class).length);
        android.graphics.Paint paint=new android.graphics.Paint();paint.setTextSize(24);TextTracking.Word wide=spaced.getSpans(0,spaced.length(),TextTracking.Word.class)[0],narrow=tight.getSpans(0,tight.length(),TextTracking.Word.class)[0];
        assertTrue(wide.getSize(paint,heading,0,3,null)>narrow.getSize(paint,heading,0,3,null));
    }
}
