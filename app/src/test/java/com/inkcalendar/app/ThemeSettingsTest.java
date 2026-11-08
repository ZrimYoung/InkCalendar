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
    private void closePage(){desc(dialog(),"Close").performClick();}
    @Test public void themeAndFontAreIndependentAndPersistWithLatestFeatures()throws Exception{
        settings();appearance();assertEquals(5,all(dialog()).stream().filter(v->v instanceof ThemePreview).count());
        assertEquals(7,all(dialog()).stream().filter(v->v instanceof RadioButton&&v.getTag() instanceof String).count());
        desc(dialog(),"Geist").performClick();desc(dialog(),"Ticket").performClick();closePage();closePage();
        assertEquals("geist",prefs.getString("theme_font",""));assertEquals("ticket",prefs.getString("theme_style",""));
        ThemeConfig config=(ThemeConfig)field("theme");assertTrue(config.sans());assertTrue(config.is("calendar","plain"));
        desc(main(),"Future events").performClick();text(dialog(),"30 days").performClick();text(dialog(),"Back").performClick();
        desc(main(),"Milestones").performClick();text(dialog(),"Close").performClick();controller.recreate();activity=controller.get();
        assertEquals("geist",((ThemeConfig)field("theme")).font);assertEquals(30,prefs.getInt("future_days",7));
    }
    @Test public void selectionsApplyImmediatelyAndClosingKeepsThem(){
        settings();appearance();desc(dialog(),"Quiet Space").performClick();desc(dialog(),"Geist").performClick();
        assertEquals("minimal",ThemeConfig.read(prefs).style);assertEquals("geist",ThemeConfig.read(prefs).font);
        assertTrue(all(dialog()).stream().noneMatch(v->"Save".contentEquals(v.getContentDescription()==null?"":v.getContentDescription())));
        closePage();closePage();assertEquals("minimal",ThemeConfig.read(prefs).style);
        settings();appearance();desc(dialog(),"Ticket").performClick();
        assertEquals("ticket",ThemeConfig.read(prefs).style);closePage();closePage();assertEquals("ticket",ThemeConfig.read(prefs).style);
    }
    @Test public void fontCardsShowActualAaAboveNameAndKeepExactlyOneSelection()throws Exception{
        prefs.edit().putBoolean("large",true).commit();controller.recreate();activity=controller.get();
        settings();appearance();View root=dialog();ThemeFonts fonts=new ThemeFonts(activity.getAssets());
        root.measure(View.MeasureSpec.makeMeasureSpec(320,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));
        root.layout(0,0,320,root.getMeasuredHeight());
        List<ThemePreview> thumbnails=new ArrayList<>();for(View v:all(root))if(v instanceof ThemePreview)thumbnails.add((ThemePreview)v);
        assertEquals(5,thumbnails.size());assertTrue(thumbnails.get(0).getWidth()>0);
        assertEquals(thumbnails.get(0).getWidth(),thumbnails.get(4).getWidth());
        for(int i=0;i<ThemeConfig.FONTS.length;i++){
            String id=ThemeConfig.FONTS[i];View card=desc(root,ThemeConfig.FONT_NAMES[i]);
            TextView sample=card.findViewWithTag("font-sample:"+id),name=card.findViewWithTag("font-name:"+id);
            assertEquals("Aa",sample.getText().toString());assertSame(fonts.latin(id,false),sample.getTypeface());
            assertEquals(ThemeConfig.FONT_NAMES[i],name.getText().toString());
            assertTrue(id,name.getTop()>=((View)sample.getParent()).getBottom());
            assertTrue(id,name.getBottom()<=card.getHeight()-card.getPaddingBottom());
            assertNotNull(name.getLayout());
            for(int line=0;line<name.getLineCount();line++)assertTrue(id,name.getLayout().getLineMax(line)<=name.getWidth()-name.getCompoundPaddingLeft()-name.getCompoundPaddingRight()+1);
            card.performClick();root=dialog();measure(root,320);assertEquals(id,((ThemeConfig)field("theme")).font);
            for(String other:ThemeConfig.FONTS){
                assertEquals(other,id.equals(other),root.findViewWithTag("font:"+other).isSelected());
                assertEquals(other,id.equals(other),((RadioButton)root.findViewWithTag(other)).isChecked());
            }
            assertEquals(id,ThemeConfig.read(prefs).font);
        }
    }
    @Test public void thumbnailHeightFollowsWidthWithoutDistortingTheAspectRatio(){
        for(String component:new String[]{null,"calendar"})for(int width:new int[]{100,188,320,600}){
            ThemePreview preview=new ThemePreview(activity,new ThemeConfig(),component);
            preview.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));
            float expected=component==null?148f/188:96f/148;
            assertEquals(width,preview.getMeasuredWidth());assertEquals(width*expected,preview.getMeasuredHeight(),.51f);
        }
    }
    private void measure(View root,int width){
        root.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));
        root.layout(0,0,width,root.getMeasuredHeight());
    }
    private void boundedChoices(View root){
        for(View view:all(root))if(view instanceof ChoiceGrid){
            ViewGroup grid=(ViewGroup)view;
            for(int i=0;i<grid.getChildCount();i++){
                View card=grid.getChildAt(i);assertTrue(card.getWidth()>0);assertTrue(card.getWidth()<=208);
                assertTrue(card.getLeft()>=0);assertTrue(card.getRight()<=grid.getWidth());assertTrue(card.getBottom()<=grid.getHeight());
                for(int j=0;j<i;j++){
                    View other=grid.getChildAt(j);
                    assertTrue(card.getLeft()>=other.getRight()||card.getRight()<=other.getLeft()||card.getTop()>=other.getBottom()||card.getBottom()<=other.getTop());
                }
                for(View child:all(card))if(child instanceof TextView){
                    TextView text=(TextView)child;
                    if(text.getLayout()!=null)for(int line=0;line<text.getLineCount();line++)assertTrue(text.getText().toString(),text.getLayout().getLineMax(line)<=text.getWidth()-text.getCompoundPaddingLeft()-text.getCompoundPaddingRight()+2);
                }
            }
        }
    }
    @Test public void choicesReflowAcrossWidthsAndKeepAppliedSelections()throws Exception{
        prefs.edit().putBoolean("large",true).commit();controller.recreate();activity=controller.get();
        settings();appearance();desc(dialog(),"Geist").performClick();int narrowColumns=0;
        try{
            for(int width:new int[]{320,572,773,1400}){
                RuntimeEnvironment.setQualifiers("w"+width+"dp-h"+(width>=640?572:773)+"dp-"+(width>=640?"land":"port")+"-mdpi");
                controller.recreate();activity=controller.get();View root=dialog();measure(root,width);boundedChoices(root);
                assertEquals("geist",((ThemeConfig)field("theme")).font);
                assertTrue(((RadioButton)root.findViewWithTag("geist")).isChecked());
                ViewGroup fonts=root.findViewWithTag("choices:fonts");java.util.Set<Integer> positions=new java.util.HashSet<>();
                for(int i=0;i<fonts.getChildCount();i++)positions.add(fonts.getChildAt(i).getLeft());
                if(width==320)narrowColumns=positions.size();else if(width>=773)assertTrue(positions.size()>narrowColumns);
            }
            ViewGroup fonts=dialog().findViewWithTag("choices:fonts");View first=fonts.getChildAt(0);
            for(int width:new int[]{320,1400,248,773,320}){measure(fonts,width);boundedChoices(fonts);assertSame(first,fonts.getChildAt(0));}
            text(dialog(),"Customize components").performClick();measure(dialog(),1400);boundedChoices(dialog());
            assertEquals(7,all(dialog()).stream().filter(v->v instanceof ChoiceGrid).count());
            desc(dialog(),"Date on right").performClick();
            RuntimeEnvironment.setQualifiers("w320dp-h773dp-port-mdpi");controller.recreate();activity=controller.get();measure(dialog(),320);boundedChoices(dialog());
            assertEquals("geist",((ThemeConfig)field("theme")).font);assertTrue(((ThemeConfig)field("theme")).is("date","reverse"));
            assertEquals("geist",ThemeConfig.read(prefs).font);closePage();closePage();closePage();assertEquals("geist",ThemeConfig.read(prefs).font);assertTrue(ThemeConfig.read(prefs).is("date","reverse"));
        }finally{RuntimeEnvironment.setQualifiers("w320dp-h640dp-port-mdpi");}
    }
    @Test public void pagePreviewChangesFrameWhenRotatedWhileLocalComponentsKeepTheirShape(){
        try{
            for(boolean landscape:new boolean[]{false,true}){
                RuntimeEnvironment.setQualifiers(landscape?"w773dp-h572dp-land-mdpi":"w572dp-h773dp-port-mdpi");
                ThemePreview page=new ThemePreview(activity,new ThemeConfig(),null),calendar=new ThemePreview(activity,new ThemeConfig(),"calendar");
                measure(page,240);measure(calendar,240);
                assertEquals(240*(landscape?148f/240:148f/188),page.getMeasuredHeight(),.51f);
                assertEquals(240*96f/148,calendar.getMeasuredHeight(),.51f);
            }
        }finally{RuntimeEnvironment.setQualifiers("w320dp-h640dp-port-mdpi");}
    }
    @Test public void componentAndFontApplyImmediatelyAndSurviveRecreation()throws Exception{
        settings();appearance();desc(dialog(),"Geist").performClick();text(dialog(),"Customize components").performClick();desc(dialog(),"Date on right").performClick();
        controller.recreate();activity=controller.get();assertTrue(((ThemeConfig)field("theme")).is("date","reverse"));
        closePage();closePage();closePage();ThemeConfig saved=ThemeConfig.read(prefs);assertEquals("geist",saved.font);assertTrue(saved.is("date","reverse"));
        assertEquals("editorial",saved.style);assertFalse(saved.overrides.isEmpty());
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
    @Test public void orientationAndAppliedThemeSurviveTogether()throws Exception{
        settings();text(dialog(),"Force portrait").performClick();appearance();desc(dialog(),"Geist").performClick();
        controller.recreate();activity=controller.get();closePage();closePage();
        assertEquals("geist",ThemeConfig.read(prefs).font);assertEquals("portrait",prefs.getString("orientation",""));
        assertEquals(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,activity.getRequestedOrientation());
        settings();text(dialog(),"Force landscape").performClick();closePage();
        assertEquals("landscape",prefs.getString("orientation",""));assertEquals("geist",ThemeConfig.read(prefs).font);
    }
    @Test public void legacyClassicSettingsAndDraftsMapToEditorialWithoutLosingCustomizations(){
        prefs.edit().putString("theme_style","original").putString("theme_font","geist").putString("theme_date","reverse").commit();
        ThemeConfig migrated=ThemeConfig.read(prefs);assertEquals("editorial",migrated.style);assertEquals("geist",migrated.font);
        assertTrue(migrated.is("date","reverse"));assertTrue(migrated.is("calendar","ruled"));
        android.os.Bundle state=new android.os.Bundle();state.putString("draft.style","original");state.putString("draft.font","space");state.putString("draft.agenda","cards");
        ThemeConfig restored=ThemeConfig.fromBundle(state,"draft.");assertEquals("editorial",restored.style);assertEquals("space",restored.font);assertTrue(restored.is("agenda","cards"));
        SharedPreferences.Editor edit=prefs.edit();migrated.save(edit);edit.commit();
        assertEquals("editorial",prefs.getString("theme_style",""));assertEquals("reverse",prefs.getString("theme_date",""));assertEquals("geist",prefs.getString("theme_font",""));
        prefs.edit().remove("theme_style").commit();assertEquals("editorial",ThemeConfig.read(prefs).style);
        prefs.edit().putString("theme_style","unknown").commit();assertEquals("editorial",ThemeConfig.read(prefs).style);
    }

}
