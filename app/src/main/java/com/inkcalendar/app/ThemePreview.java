package com.inkcalendar.app;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;

/** Tiny vector thumbnails: no screenshot assets, bitmap allocations or network requests. */
@SuppressLint("ViewConstructor")
final class ThemePreview extends View {
    private final ThemeConfig theme;
    private final String component;
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    ThemePreview(Context context,ThemeConfig theme,String component){super(context);this.theme=new ThemeConfig(theme);this.component=component;setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
    private void line(Canvas c,float x,float y,float end,float bottom){paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(1);c.drawLine(x,y,end,bottom,paint);}
    private void box(Canvas c,float x,float y,float w,float h,boolean fill){paint.setStyle(fill?Paint.Style.FILL:Paint.Style.STROKE);paint.setStrokeWidth(1);c.drawRect(x,y,x+w,y+h,paint);}
    private void text(Canvas c,String value,float x,float y,float size){paint.setStyle(Paint.Style.FILL);paint.setTextSize(size);c.drawText(value,x,y,paint);}
    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);int save=canvas.save();canvas.scale(getWidth()/148f,getHeight()/96f);paint.setColor(Color.WHITE);paint.setStyle(Paint.Style.FILL);canvas.drawRect(0,0,148,96,paint);paint.setColor(Color.BLACK);
        if(component==null){line(canvas,10,8,138,8);text(canvas,theme.is("caps","on")?"INK CALENDAR":"Ink Calendar",10,19,7);hero(canvas,26);if(theme.is("flow","agenda-first")){agenda(canvas,58,12);calendar(canvas,74,20);}else{calendar(canvas,58,22);agenda(canvas,85,9);}}
        else if("date".equals(component))hero(canvas,30);
        else if("calendar".equals(component))calendar(canvas,10,75);
        else if("agenda".equals(component))agenda(canvas,12,72);
        else if("buttons".equals(component)){for(int i=0;i<3;i++){float x=24+i*37;boolean solid=theme.is("buttons","solid");if(!theme.is("buttons","bare"))box(canvas,x,32,26,26,solid);paint.setColor(solid?Color.WHITE:Color.BLACK);line(canvas,x+7,45,x+19,45);if(i==1)line(canvas,x+13,39,x+13,51);paint.setColor(Color.BLACK);}}
        else if("flow".equals(component)){if(theme.is("flow","agenda-first")){agenda(canvas,10,30);calendar(canvas,49,40);}else{calendar(canvas,10,40);agenda(canvas,58,30);}}
        else {String value=theme.is("caps","on")?"INK CALENDAR":"Ink Calendar";float x=12;for(char ch:value.toCharArray()){text(canvas,String.valueOf(ch),x,48,14);x+=paint.measureText(String.valueOf(ch))+(theme.is("tracking","spaced")?2:theme.is("tracking","tight")?-0.5f:0);}}
        canvas.restoreToCount(save);
    }
    private void hero(Canvas c,float y){
        if(theme.is("date","tiles")){box(c,38,y-2,31,27,false);box(c,75,y-2,31,27,false);text(c,"0",44,y+20,24);text(c,"5",81,y+20,24);}
        else if(theme.is("date","center")){text(c,"05",57,y+18,26);text(c,"Thursday",56,y+29,7);}
        else if(theme.is("date","reverse")){text(c,"November",12,y+8,8);text(c,"Thursday",12,y+19,8);text(c,"05",100,y+21,29);}
        else {text(c,"05",12,y+21,theme.is("date","masthead")?34:29);text(c,"November",76,y+8,8);text(c,"Thursday",76,y+19,8);if(theme.is("date","masthead"))line(c,76,y-2,132,y-2);}
    }
    private void calendar(Canvas c,float y,float height){
        float step=height/3;
        for(int r=0;r<3;r++){for(int col=0;col<7;col++){float x=15+col*19,top=y+r*step;boolean selected=r==1&&col==3;
            if(selected&&theme.is("calendar","open")){paint.setStyle(Paint.Style.FILL);c.drawCircle(x+3,top+step/2-1,Math.min(6,step/2),paint);paint.setColor(Color.WHITE);}
            text(c,String.valueOf(r*7+col+1),x,top+step/2+2,Math.min(8,step*.7f));paint.setColor(Color.BLACK);
            if(selected&&theme.is("calendar","plain"))line(c,x-1,top+step/2+5,x+9,top+step/2+5);
        }if(theme.is("calendar","ruled"))line(c,10,y+(r+1)*step-1,138,y+(r+1)*step-1);}
    }
    private void agenda(Canvas c,float y,float height){
        float step=height/3;
        for(int i=0;i<3;i++){float top=y+i*step;if(theme.is("agenda","cards"))box(c,10,top,128,step-2,false);
            if(theme.is("agenda","timeline")){line(c,13,top+4,32,top+4);line(c,38,top+1,38,top+step-2);line(c,46,top+4,126,top+4);}
            else if(theme.is("agenda","ledger")){line(c,13,top+4,91,top+4);line(c,110,top+4,134,top+4);}
            else {line(c,14,top+3,45,top+3);line(c,14,top+Math.max(5,step*.55f),118,top+Math.max(5,step*.55f));}
        }
    }
}
