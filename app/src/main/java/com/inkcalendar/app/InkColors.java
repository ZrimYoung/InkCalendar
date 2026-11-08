package com.inkcalendar.app;

import android.content.Context;
import android.graphics.Color;
import android.util.TypedValue;

/** A palette belongs to a themed context, so dialogs and previews stay independent. */
final class InkColors {
    final int paper,ink,track,edge,disabled;
    private InkColors(int paper,int ink){
        this.paper=paper;this.ink=ink;
        boolean night=paper==Color.BLACK;
        track=night?0xff222222:0xffeeeeee;
        edge=night?0xff777777:0xffbbbbbb;
        disabled=night?0xff777777:0xff999999;
    }
    static InkColors forMode(String mode){
        int index=0;
        for(int i=0;i<SharedThemeCatalog.COLOR_MODES.length;i++)if(SharedThemeCatalog.COLOR_MODES[i].equals(mode))index=i;
        return new InkColors(Color.parseColor(SharedThemeCatalog.COLOR_MODE_PAPER[index]),Color.parseColor(SharedThemeCatalog.COLOR_MODE_INK[index]));
    }
    static InkColors from(Context context){
        TypedValue value=new TypedValue();
        String mode=context.getTheme().resolveAttribute(R.attr.inkColorMode,value,true)&&value.string!=null?value.string.toString():SharedThemeCatalog.DEFAULT_COLOR_MODE;
        return forMode(mode);
    }
}
