package com.inkcalendar.app;

import android.content.res.AssetManager;
import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.MetricAffectingSpan;
import java.util.HashMap;
import java.util.Map;

/** Static fonts work on API 19. Cache each asset, and apply the Chinese partner per text run. */
final class ThemeFonts {
    private static final Map<String,Typeface> CACHE=new HashMap<>();
    private final AssetManager assets;
    ThemeFonts(AssetManager assets){this.assets=assets;}
    private Typeface asset(String name){synchronized(CACHE){Typeface face=CACHE.get(name);if(face==null){face=Typeface.createFromAsset(assets,"fonts/"+name);CACHE.put(name,face);}return face;}}
    Typeface latin(String id,boolean bold){int i=Math.max(0,ThemeConfig.index(ThemeConfig.FONTS,id));return asset((bold?SharedThemeCatalog.FONT_BOLD:SharedThemeCatalog.FONT_REGULAR)[i]);}
    Typeface label(String id){return "original".equals(id)?asset("NewsprintDisplay-Semibold.ttf"):latin(id,true);}
    Typeface reading(String id){return "original".equals(id)?asset("NewsprintReading-Regular.ttf"):latin(id,false);}
    Typeface chinese(String id){ThemeConfig config=new ThemeConfig();config.font=id;return asset(config.sans()?"NotoSansSC-Regular.otf":"NotoSerifSC-Regular.otf");}
    static CharSequence mixed(CharSequence value,Typeface chinese,boolean bold){
        if(value==null||chinese==null)return value;
        SpannableString result=new SpannableString(value);
        for(ChineseSpan span:result.getSpans(0,result.length(),ChineseSpan.class))result.removeSpan(span);
        for(int i=0;i<value.length();){if(!cjk(value.charAt(i))){i++;continue;}int start=i++;while(i<value.length()&&cjk(value.charAt(i)))i++;result.setSpan(new ChineseSpan(chinese,bold),start,i,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);}
        return result;
    }
    private static boolean cjk(char c){return c>=0x2e80;}
    static final class ChineseSpan extends MetricAffectingSpan {
        private final Typeface face;private final boolean bold;
        ChineseSpan(Typeface face,boolean bold){this.face=face;this.bold=bold;}
        private void apply(TextPaint paint){paint.setTypeface(face);paint.setFakeBoldText(bold);}
        @Override public void updateDrawState(TextPaint paint){apply(paint);}
        @Override public void updateMeasureState(TextPaint paint){apply(paint);}
    }
}
