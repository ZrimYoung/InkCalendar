package com.inkcalendar.app;

import android.content.SharedPreferences;
import android.os.Bundle;
import java.util.HashMap;
import java.util.Map;

/** Small persisted recipe; changing a layout never changes the chosen font or calendar data. */
final class ThemeConfig {
    static final String[] FONTS=SharedThemeCatalog.FONTS,FONT_NAMES=SharedThemeCatalog.FONT_NAMES;
    static final String DEFAULT_STYLE=SharedThemeCatalog.DEFAULT_STYLE;
    static final String[] STYLES=SharedThemeCatalog.STYLES,KEYS=SharedThemeCatalog.KEYS;
    static final int[] STYLE_LABELS=SharedThemeCatalog.STYLE_LABELS,KEY_LABELS=SharedThemeCatalog.KEY_LABELS;
    static final String[][] OPTIONS=SharedThemeCatalog.OPTIONS;
    static final int[][] OPTION_LABELS=SharedThemeCatalog.OPTION_LABELS;
    private static final String[][] RECIPES=SharedThemeCatalog.RECIPES;
    String font=SharedThemeCatalog.DEFAULT_FONT,style=DEFAULT_STYLE;
    final Map<String,String> overrides=new HashMap<>();
    ThemeConfig() {}
    ThemeConfig(ThemeConfig other){font=other.font;chooseStyle(other.style);overrides.putAll(other.overrides);}
    static int index(String[] items,String value){for(int i=0;i<items.length;i++)if(items[i].equals(value))return i;return -1;}
    String component(String key){int i=index(KEYS,key);if(i<0)throw new IllegalArgumentException(key);String value=overrides.get(key);return index(OPTIONS[i],value)>=0?value:RECIPES[Math.max(0,index(STYLES,style))][i];}
    void chooseStyle(String value){if("original".equals(value))value=DEFAULT_STYLE;if(index(STYLES,value)>=0){style=value;overrides.clear();}}
    void choose(String key,String value){int i=index(KEYS,key);if(i>=0&&index(OPTIONS[i],value)>=0)overrides.put(key,value);}
    boolean is(String key,String value){return value.equals(component(key));}
    boolean sans(){return SharedThemeCatalog.FONT_SANS[Math.max(0,index(FONTS,font))];}
    int ruleWidth(){return SharedThemeCatalog.RULE_WIDTHS[Math.max(0,index(STYLES,style))];}
    int label(){return STYLE_LABELS[Math.max(0,index(STYLES,style))];}
    static ThemeConfig read(SharedPreferences prefs){
        ThemeConfig value=new ThemeConfig();String font=prefs.getString("theme_font",SharedThemeCatalog.DEFAULT_FONT),style=prefs.getString("theme_style",DEFAULT_STYLE);
        if(index(FONTS,font)>=0)value.font=font;value.chooseStyle(style);
        for(String key:KEYS)value.choose(key,prefs.getString("theme_"+key,""));return value;
    }
    void save(SharedPreferences.Editor edit){edit.putString("theme_font",font).putString("theme_style",style);for(String key:KEYS){if(overrides.containsKey(key))edit.putString("theme_"+key,overrides.get(key));else edit.remove("theme_"+key);}}
    void bundle(Bundle out,String prefix){out.putString(prefix+"font",font);out.putString(prefix+"style",style);for(String key:KEYS)if(overrides.containsKey(key))out.putString(prefix+key,overrides.get(key));}
    static ThemeConfig fromBundle(Bundle in,String prefix){ThemeConfig value=new ThemeConfig();if(index(FONTS,in.getString(prefix+"font"))>=0)value.font=in.getString(prefix+"font");value.chooseStyle(in.getString(prefix+"style"));for(String key:KEYS)value.choose(key,in.getString(prefix+key));return value;}
}
