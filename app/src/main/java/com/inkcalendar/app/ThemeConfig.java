package com.inkcalendar.app;

import android.content.SharedPreferences;
import android.os.Bundle;
import java.util.HashMap;
import java.util.Map;

/** Small persisted recipe; changing a layout never changes the chosen font or calendar data. */
final class ThemeConfig {
    static final String[] FONTS={"original","geist","baskerville","fraunces","slab","space","atkinson"};
    static final String[] FONT_NAMES={"Source Serif 4 / EB Garamond","Geist","Libre Baskerville","Fraunces","Roboto Slab","Space Grotesk","Atkinson Hyperlegible Next"};
    static final String[] STYLES={"original","minimal","editorial","grid","ticket","today"};
    static final int[] STYLE_LABELS={R.string.theme_original,R.string.theme_minimal,R.string.theme_editorial,R.string.theme_grid,R.string.theme_ticket,R.string.theme_today};
    static final String[] KEYS={"date","calendar","agenda","buttons","flow","tracking","caps"};
    static final int[] KEY_LABELS={R.string.theme_date,R.string.theme_calendar,R.string.theme_agenda,R.string.theme_buttons,R.string.theme_flow,R.string.theme_tracking,R.string.theme_caps};
    static final String[][] OPTIONS={{"split","masthead","tiles","reverse","center"},{"open","ruled","plain"},{"timeline","stacked","cards","ledger"},{"bare","outline","solid"},{"calendar-first","agenda-first"},{"normal","tight","spaced"},{"off","on"}};
    static final int[][] OPTION_LABELS={
        {R.string.theme_split,R.string.theme_masthead,R.string.theme_tiles,R.string.theme_reverse,R.string.theme_center},
        {R.string.theme_open,R.string.theme_ruled,R.string.theme_plain},
        {R.string.theme_timeline,R.string.theme_stacked,R.string.theme_cards,R.string.theme_ledger},
        {R.string.theme_bare,R.string.theme_outline,R.string.theme_solid},
        {R.string.theme_calendar_first,R.string.theme_agenda_first},
        {R.string.theme_normal,R.string.theme_tight,R.string.theme_spaced},
        {R.string.theme_case_normal,R.string.theme_case_upper}
    };
    private static final String[][] RECIPES={
        {"split","ruled","stacked","outline","calendar-first","normal","off"},
        {"split","open","timeline","bare","calendar-first","normal","off"},
        {"masthead","ruled","stacked","outline","calendar-first","spaced","on"},
        {"tiles","ruled","cards","solid","calendar-first","tight","off"},
        {"reverse","plain","ledger","outline","calendar-first","spaced","on"},
        {"center","open","timeline","bare","agenda-first","tight","off"}
    };
    String font="original",style="original";
    final Map<String,String> overrides=new HashMap<>();
    ThemeConfig() {}
    ThemeConfig(ThemeConfig other){font=other.font;style=other.style;overrides.putAll(other.overrides);}
    static int index(String[] items,String value){for(int i=0;i<items.length;i++)if(items[i].equals(value))return i;return -1;}
    String component(String key){int i=index(KEYS,key);if(i<0)throw new IllegalArgumentException(key);String value=overrides.get(key);return index(OPTIONS[i],value)>=0?value:RECIPES[Math.max(0,index(STYLES,style))][i];}
    void chooseStyle(String value){if(index(STYLES,value)>=0){style=value;overrides.clear();}}
    void choose(String key,String value){int i=index(KEYS,key);if(i>=0&&index(OPTIONS[i],value)>=0)overrides.put(key,value);}
    boolean is(String key,String value){return value.equals(component(key));}
    boolean sans(){return "geist".equals(font)||"space".equals(font)||"atkinson".equals(font);}
    int ruleWidth(){return "minimal".equals(style)?1:"grid".equals(style)?3:"ticket".equals(style)||"today".equals(style)?2:4;}
    int label(){return STYLE_LABELS[Math.max(0,index(STYLES,style))];}
    static ThemeConfig read(SharedPreferences prefs){
        ThemeConfig value=new ThemeConfig();String font=prefs.getString("theme_font","original"),style=prefs.getString("theme_style","original");
        if(index(FONTS,font)>=0)value.font=font;if(index(STYLES,style)>=0)value.style=style;
        for(String key:KEYS)value.choose(key,prefs.getString("theme_"+key,""));return value;
    }
    void save(SharedPreferences.Editor edit){edit.putString("theme_font",font).putString("theme_style",style);for(String key:KEYS){if(overrides.containsKey(key))edit.putString("theme_"+key,overrides.get(key));else edit.remove("theme_"+key);}}
    void bundle(Bundle out,String prefix){out.putString(prefix+"font",font);out.putString(prefix+"style",style);for(String key:KEYS)if(overrides.containsKey(key))out.putString(prefix+key,overrides.get(key));}
    static ThemeConfig fromBundle(Bundle in,String prefix){ThemeConfig value=new ThemeConfig();if(index(FONTS,in.getString(prefix+"font"))>=0)value.font=in.getString(prefix+"font");value.chooseStyle(in.getString(prefix+"style"));for(String key:KEYS)value.choose(key,in.getString(prefix+key));return value;}
}
