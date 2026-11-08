package com.inkcalendar.app;

import android.content.SharedPreferences;
import android.os.Bundle;

/** Display preferences are persisted whenever a settings control changes. */
final class DisplayOptions {
    int scale=SharedThemeCatalog.DEFAULT_SCALE;
    String view=SharedThemeCatalog.DEFAULT_VIEW,layout=SharedThemeCatalog.DEFAULT_LAYOUT,orientation=SharedThemeCatalog.DEFAULT_ORIENTATION,language=SharedThemeCatalog.DEFAULT_LANGUAGE,colorMode=SharedThemeCatalog.DEFAULT_COLOR_MODE;
    boolean awake=SharedThemeCatalog.DEFAULT_AWAKE,full=SharedThemeCatalog.DEFAULT_FULL,lunar=SharedThemeCatalog.DEFAULT_LUNAR,terms=SharedThemeCatalog.DEFAULT_TERMS,holidays=SharedThemeCatalog.DEFAULT_HOLIDAYS,hideDone=SharedThemeCatalog.DEFAULT_HIDE_DONE,desk=SharedThemeCatalog.DEFAULT_DESK;

    static DisplayOptions read(SharedPreferences prefs){
        DisplayOptions o=new DisplayOptions();
        o.scale=clamp(prefs.getInt("text_scale",prefs.getBoolean("large",false)?SharedThemeCatalog.LEGACY_LARGE_SCALE:o.scale));
        o.view=choice(prefs.getString("calendar_view",o.view),"month","week");
        o.layout=choice(prefs.getString("landscape_layout",o.layout),"classic","balanced");
        o.orientation=choice(prefs.getString("orientation",o.orientation),"auto","landscape","portrait");
        o.language=choice(prefs.getString("language",o.language),"zh","en");
        o.colorMode=choice(prefs.getString("color_mode",o.colorMode),SharedThemeCatalog.COLOR_MODES);
        o.awake=prefs.getBoolean("awake",o.awake);o.full=prefs.getBoolean("full",o.full);
        o.lunar=prefs.getBoolean("lunar",o.lunar);o.terms=prefs.getBoolean("terms",o.terms);
        o.holidays=prefs.getBoolean("holidays",o.holidays);o.hideDone=prefs.getBoolean("hide_done",o.hideDone);o.desk=prefs.getBoolean("desk",o.desk);
        return o;
    }
    static DisplayOptions restore(SharedPreferences prefs,Bundle state){
        DisplayOptions o=read(prefs);if(state==null)return o;
        o.scale=clamp(state.getInt("settings.scale",o.scale));
        o.view=choice(state.getString("settings.view",o.view),"month","week");
        o.layout=choice(state.getString("settings.layout",o.layout),"classic","balanced");
        o.orientation=choice(state.getString("settings.orientation",o.orientation),"auto","landscape","portrait");
        o.language=choice(state.getString("settings.language",o.language),"zh","en");
        o.colorMode=choice(state.getString("settings.color_mode",o.colorMode),SharedThemeCatalog.COLOR_MODES);
        o.awake=state.getBoolean("settings.awake",o.awake);o.full=state.getBoolean("settings.full",o.full);
        o.lunar=state.getBoolean("settings.lunar",o.lunar);o.terms=state.getBoolean("settings.terms",o.terms);
        o.holidays=state.getBoolean("settings.holidays",o.holidays);o.hideDone=state.getBoolean("settings.hide_done",o.hideDone);o.desk=state.getBoolean("settings.desk",o.desk);
        return o;
    }
    void bundle(Bundle out){
        out.putInt("settings.scale",scale);out.putString("settings.view",view);out.putString("settings.layout",layout);
        out.putString("settings.orientation",orientation);out.putString("settings.language",language);
        out.putString("settings.color_mode",colorMode);
        out.putBoolean("settings.awake",awake);out.putBoolean("settings.full",full);out.putBoolean("settings.lunar",lunar);
        out.putBoolean("settings.terms",terms);out.putBoolean("settings.holidays",holidays);out.putBoolean("settings.hide_done",hideDone);out.putBoolean("settings.desk",desk);
    }
    void save(SharedPreferences.Editor edit){
        edit.putInt("text_scale",clamp(scale)).putBoolean("large",scale>100)
            .putString("calendar_view",view).putString("landscape_layout",layout)
            .putString("orientation",orientation).putString("language",language).putString("color_mode",colorMode)
            .putBoolean("awake",awake).putBoolean("full",full).putBoolean("lunar",lunar)
            .putBoolean("terms",terms).putBoolean("holidays",holidays).putBoolean("hide_done",hideDone).putBoolean("desk",desk);
    }
    static int clamp(int value){return Math.max(SharedThemeCatalog.SCALE_MIN,Math.min(SharedThemeCatalog.SCALE_MAX,value));}
    private static String choice(String value,String... choices){for(String c:choices)if(c.equals(value))return c;return choices[0];}
}
