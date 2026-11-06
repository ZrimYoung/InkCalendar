package com.inkcalendar.app;

import android.content.SharedPreferences;
import android.os.Bundle;

/** Display preferences are edited as a draft and applied together on Save. */
final class DisplayOptions {
    int scale=100;
    String view="month",layout="classic",orientation="auto",language="zh";
    boolean awake=true,full=true,lunar=true,terms=true,holidays=true,hideDone,desk;

    static DisplayOptions read(SharedPreferences prefs){
        DisplayOptions o=new DisplayOptions();
        o.scale=clamp(prefs.getInt("text_scale",prefs.getBoolean("large",false)?118:100));
        o.view=choice(prefs.getString("calendar_view","month"),"month","week");
        o.layout=choice(prefs.getString("landscape_layout","classic"),"classic","balanced");
        o.orientation=choice(prefs.getString("orientation","auto"),"auto","landscape","portrait");
        o.language=choice(prefs.getString("language","zh"),"zh","en");
        o.awake=prefs.getBoolean("awake",true);o.full=prefs.getBoolean("full",true);
        o.lunar=prefs.getBoolean("lunar",true);o.terms=prefs.getBoolean("terms",true);
        o.holidays=prefs.getBoolean("holidays",true);o.hideDone=prefs.getBoolean("hide_done",false);o.desk=prefs.getBoolean("desk",false);
        return o;
    }
    static DisplayOptions restore(SharedPreferences prefs,Bundle state){
        DisplayOptions o=read(prefs);if(state==null)return o;
        o.scale=clamp(state.getInt("settings.scale",o.scale));
        o.view=choice(state.getString("settings.view",o.view),"month","week");
        o.layout=choice(state.getString("settings.layout",o.layout),"classic","balanced");
        o.orientation=choice(state.getString("settings.orientation",o.orientation),"auto","landscape","portrait");
        o.language=choice(state.getString("settings.language",o.language),"zh","en");
        o.awake=state.getBoolean("settings.awake",o.awake);o.full=state.getBoolean("settings.full",o.full);
        o.lunar=state.getBoolean("settings.lunar",o.lunar);o.terms=state.getBoolean("settings.terms",o.terms);
        o.holidays=state.getBoolean("settings.holidays",o.holidays);o.hideDone=state.getBoolean("settings.hide_done",o.hideDone);o.desk=state.getBoolean("settings.desk",o.desk);
        return o;
    }
    void bundle(Bundle out){
        out.putInt("settings.scale",scale);out.putString("settings.view",view);out.putString("settings.layout",layout);
        out.putString("settings.orientation",orientation);out.putString("settings.language",language);
        out.putBoolean("settings.awake",awake);out.putBoolean("settings.full",full);out.putBoolean("settings.lunar",lunar);
        out.putBoolean("settings.terms",terms);out.putBoolean("settings.holidays",holidays);out.putBoolean("settings.hide_done",hideDone);out.putBoolean("settings.desk",desk);
    }
    void save(SharedPreferences.Editor edit){
        edit.putInt("text_scale",clamp(scale)).putBoolean("large",scale>100)
            .putString("calendar_view",view).putString("landscape_layout",layout)
            .putString("orientation",orientation).putString("language",language)
            .putBoolean("awake",awake).putBoolean("full",full).putBoolean("lunar",lunar)
            .putBoolean("terms",terms).putBoolean("holidays",holidays).putBoolean("hide_done",hideDone).putBoolean("desk",desk);
    }
    static int clamp(int value){return Math.max(90,Math.min(130,value));}
    private static String choice(String value,String... choices){for(String c:choices)if(c.equals(value))return c;return choices[0];}
}
