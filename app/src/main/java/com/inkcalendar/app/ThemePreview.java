package com.inkcalendar.app;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.view.View;

/** Proportional vector thumbnails; no screenshots, bitmap allocations or network requests. */
@SuppressLint("ViewConstructor")
final class ThemePreview extends View {
    private static final String[] WEEK_DAYS={"M","T","W","T","F","S","S"};
    private final ThemeConfig theme;
    private final String component;
    private final boolean weekly,balanced;
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Typeface regular,bold;

    ThemePreview(Context context,ThemeConfig theme,String component){
        this(context,theme,component,DisplayOptions.read(context.getSharedPreferences("display",Context.MODE_PRIVATE)));
    }
    ThemePreview(Context context,ThemeConfig theme,String component,DisplayOptions options){
        super(context);this.theme=new ThemeConfig(theme);this.component=component;
        weekly="week".equals(options.view);balanced="balanced".equals(options.layout);
        ThemeFonts fonts=new ThemeFonts(context.getAssets());
        regular=fonts.latin("original",false);bold=fonts.latin("original",true);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    private boolean landscape(){return getResources().getConfiguration().orientation==Configuration.ORIENTATION_LANDSCAPE;}
    private boolean wideLandscape(){return landscape()&&getResources().getConfiguration().screenWidthDp>=640;}
    private float previewWidth(){return component==null?(landscape()?240:188):148;}
    private float previewHeight(){return component==null?148:96;}

    @Override protected void onMeasure(int widthSpec,int heightSpec){
        int width=resolveSize(Math.round(previewWidth()*getResources().getDisplayMetrics().density),widthSpec);
        int height=Math.round(width*previewHeight()/previewWidth());
        setMeasuredDimension(width,resolveSize(height,heightSpec));
    }

    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);canvas.drawColor(Color.WHITE);
        if(getWidth()==0||getHeight()==0)return;
        float scale=Math.min(getWidth()/previewWidth(),getHeight()/previewHeight());
        int save=canvas.save();
        canvas.translate((getWidth()-previewWidth()*scale)/2,(getHeight()-previewHeight()*scale)/2);
        canvas.scale(scale,scale);paint.setColor(Color.BLACK);
        if(component==null)style(canvas);else component(canvas);
        canvas.restoreToCount(save);
    }

    private void line(Canvas c,float x,float y,float end,float bottom){line(c,x,y,end,bottom,1);}
    private void line(Canvas c,float x,float y,float end,float bottom,float width){
        paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(width);c.drawLine(x,y,end,bottom,paint);
    }
    private void box(Canvas c,float x,float y,float w,float h,boolean fill){
        paint.setStyle(fill?Paint.Style.FILL:Paint.Style.STROKE);paint.setStrokeWidth(1);c.drawRect(x,y,x+w,y+h,paint);
    }
    private void text(Canvas c,String value,float x,float y,float size){text(c,value,x,y,size,false,Paint.Align.LEFT);}
    private void text(Canvas c,String value,float x,float y,float size,boolean strong,Paint.Align align){
        paint.setStyle(Paint.Style.FILL);paint.setTypeface(strong?bold:regular);
        paint.setTextSize(size);paint.setTextAlign(align);c.drawText(value,x,y,paint);
    }
    private void fittedText(Canvas c,String value,float x,float y,float size,float width,Paint.Align align){
        paint.setTypeface(regular);paint.setTextSize(size);
        text(c,value,x,y,Math.min(size,size*width/Math.max(1,paint.measureText(value))),false,align);
    }
    private int ruleWidth(){return Math.max(1,Math.round(theme.ruleWidth()/2f));}
    private void words(Canvas c,float x,float y){
        text(c,"November",x,y,7);text(c,theme.is("caps","on")?"FRIDAY":"Friday",x,y+13,10);
    }

    private void style(Canvas c){
        float width=previewWidth(),center=width/2,right=width-10;
        text(c,theme.is("caps","on")?"INK CALENDAR":"Ink Calendar",10,12,8,true,Paint.Align.LEFT);
        line(c,10,19,right,19,ruleWidth());
        if(wideLandscape()){
            if(balanced){
                boolean first=theme.is("flow","agenda-first");float identityX=first?10:132,calendarX=first?132:10;
                landscapeIdentity(c,identityX,27,98);styleAgenda(c,identityX,108,98,15);styleCalendar(c,calendarX,31,98,105,6,true,true);return;
            }
            landscapeIdentity(c,10,27,76);
            if(theme.is("flow","agenda-first")){
                styleAgenda(c,102,34,128,21);styleCalendar(c,10,100,220,40,3,false,false);
            }else{
                styleAgenda(c,10,108,76,15);styleCalendar(c,98,31,132,105,6,true,true);
            }
            return;
        }
        if(theme.is("date","tiles")){
            box(c,center-46,26,43,35,false);box(c,center+3,26,43,35,false);
            text(c,"0",center-25,53,30,true,Paint.Align.CENTER);text(c,"6",center+24,53,30,true,Paint.Align.CENTER);
            text(c,"November · Friday",center,71,6.5f,false,Paint.Align.CENTER);
        }else if(theme.is("date","center")){
            text(c,"06",center,53,36,true,Paint.Align.CENTER);text(c,"Friday",center,66,8,false,Paint.Align.CENTER);
        }else if(theme.is("date","reverse")){
            words(c,10,35);text(c,"06",right,56,34,true,Paint.Align.RIGHT);
        }else{
            boolean masthead=theme.is("date","masthead");
            float copy=width*.55f;
            text(c,"06",10,59,masthead?40:34,true,Paint.Align.LEFT);words(c,copy,35);
            if(masthead)line(c,copy,56,right,56);
        }
        if(theme.is("flow","agenda-first")){styleAgenda(c,10,84,width-20,13);styleCalendar(c,10,110,width-20,38,3,false,false);}
        else{styleCalendar(c,10,80,width-20,38,3,false,false);styleAgenda(c,10,123,width-20,13);}
    }

    private void landscapeIdentity(Canvas c,float x,float y,float width){
        if(theme.is("date","tiles")){
            float digit=(width-4)/2;
            box(c,x,y,digit,37,false);box(c,x+digit+4,y,digit,37,false);
            text(c,"0",x+digit/2,y+30,29,true,Paint.Align.CENTER);
            text(c,"6",x+width-digit/2,y+30,29,true,Paint.Align.CENTER);
            text(c,"November",x,y+48,7);text(c,"Friday",x,y+59,8);
        }else if(theme.is("date","reverse")){
            text(c,"November",x,y+17,6);text(c,theme.is("caps","on")?"FRIDAY":"Friday",x,y+29,7);
            text(c,"06",x+width,y+47,27,true,Paint.Align.RIGHT);
        }else{
            boolean centered=theme.is("date","center");Paint.Align align=centered?Paint.Align.CENTER:Paint.Align.LEFT;
            float anchor=centered?x+width/2:x;
            text(c,"November",anchor,y+7,7,false,align);
            text(c,"06",anchor,y+46,theme.is("date","masthead")||centered?38:32,true,align);
            text(c,theme.is("caps","on")?"FRIDAY":"Friday",anchor,y+60,8,false,align);
        }
    }

    private void styleCalendar(Canvas c,float x,float y,float width,float height,int rows,boolean fullMonth,boolean title){
        if(weekly){styleWeek(c,x,y,width,height,title);return;}
        if(title){text(c,"November 2026",x+width/2,y+7,8,true,Paint.Align.CENTER);y+=16;height-=16;}
        float column=width/7,step=(height-8)/rows;
        for(int col=0;col<7;col++)text(c,WEEK_DAYS[col],x+(col+.5f)*column,y,5.5f,false,Paint.Align.CENTER);
        for(int row=0;row<rows;row++){
            float top=y+5+row*step;
            for(int col=0;col<7;col++){
                float center=x+(col+.5f)*column;int value=fullMonth?row*7+col-5:row*7+col+2;
                boolean selected=value==6;int day=value<1?31+value:value>30?value-30:value;
                if(selected&&theme.is("calendar","ruled")){box(c,center-column/2,top-1,column,step,true);paint.setColor(Color.WHITE);}
                else if(selected&&theme.is("calendar","open")){
                    paint.setStyle(Paint.Style.FILL);c.drawCircle(center,top+step*.4f,Math.min(6,step*.5f),paint);paint.setColor(Color.WHITE);
                }
                text(c,String.valueOf(day),center,top+step*.7f,Math.min(8,step*.7f),selected,Paint.Align.CENTER);paint.setColor(Color.BLACK);
                if(selected&&theme.is("calendar","plain"))line(c,center-5,top+step-1,center+5,top+step-1);
            }
            if(theme.is("calendar","ruled"))line(c,x,top+step-1,x+width,top+step-1,.5f);
        }
    }
    private void styleWeek(Canvas c,float x,float y,float width,float height,boolean title){
        if(title){text(c,"Nov 2 - 8",x+width/2,y+7,8,true,Paint.Align.CENTER);y+=16;height-=16;}
        float step=height/7;
        for(int i=0;i<7;i++){
            float top=y+i*step,center=x+Math.min(8,width*.08f);boolean selected=i==4;
            if(selected&&theme.is("calendar","ruled")){box(c,x,top+1,15,Math.max(3,step-2),true);paint.setColor(Color.WHITE);}
            else if(selected&&theme.is("calendar","open")){paint.setStyle(Paint.Style.FILL);c.drawCircle(center,top+step*.5f,Math.min(6,step*.42f),paint);paint.setColor(Color.WHITE);}
            text(c,String.valueOf(i+2),center,top+step*.7f,Math.min(8,step*.65f),selected,Paint.Align.CENTER);paint.setColor(Color.BLACK);
            if(selected&&theme.is("calendar","plain"))line(c,x+2,top+step-1,x+14,top+step-1);
            line(c,x+22,top+step*.5f,x+width-3,top+step*.5f,.5f);
            if(theme.is("calendar","ruled"))line(c,x,top+step,x+width,top+step,.5f);
        }
    }

    private void styleAgenda(Canvas c,float x,float y,float width,float step){
        line(c,x,y-3,x+width,y-3,ruleWidth());
        for(int i=0;i<2;i++){
            float top=y+i*step;String time=i==0?"09:30":"14:00",name=i==0?"Read a chapter":"Weekly plan";
            if(theme.is("agenda","cards"))box(c,x,top-1,width,step-1,false);
            if(theme.is("agenda","timeline")){
                fittedText(c,time,x+2,top+7,6,width*.28f-2,Paint.Align.LEFT);
                fittedText(c,name,x+width*.32f,top+7,6.5f,width*.68f-2,Paint.Align.LEFT);
            }else if(theme.is("agenda","ledger")){
                fittedText(c,name,x+2,top+7,6.5f,width*.68f-2,Paint.Align.LEFT);
                fittedText(c,time,x+width-2,top+7,6,width*.28f-2,Paint.Align.RIGHT);
            }else{text(c,time,x+2,top+3,5);fittedText(c,name,x+2,top+10,6.5f,width-4,Paint.Align.LEFT);}
        }
    }

    private void component(Canvas c){
        if("date".equals(component)){
            if(wideLandscape())landscapeIdentity(c,36,10,76);else hero(c,30);
        }
        else if("calendar".equals(component))calendar(c,10,75);
        else if("agenda".equals(component))agenda(c,12,72);
        else if("buttons".equals(component)){
            for(int i=0;i<3;i++){
                float x=24+i*37;boolean solid=theme.is("buttons","solid");
                if(!theme.is("buttons","bare"))box(c,x,32,26,26,solid);
                paint.setColor(solid?Color.WHITE:Color.BLACK);line(c,x+7,45,x+19,45);
                if(i==1)line(c,x+13,39,x+13,51);paint.setColor(Color.BLACK);
            }
        }else if("flow".equals(component)){
            if(wideLandscape()){
                if(theme.is("flow","agenda-first")){
                    text(c,"06",12,36,28,true,Paint.Align.LEFT);styleAgenda(c,62,15,76,14);
                    styleCalendar(c,10,54,128,38,3,false,false);
                }else{styleAgenda(c,10,48,48,14);styleCalendar(c,66,15,72,74,6,true,true);}
            }else if(theme.is("flow","agenda-first")){agenda(c,10,30);calendar(c,49,40);}
            else{calendar(c,10,40);agenda(c,58,30);}
        }else{
            String value=theme.is("caps","on")?"INK CALENDAR":"Ink Calendar";float x=12;
            for(char ch:value.toCharArray()){
                text(c,String.valueOf(ch),x,48,14);
                x+=paint.measureText(String.valueOf(ch))+(theme.is("tracking","spaced")?2:theme.is("tracking","tight")?-.5f:0);
            }
        }
    }

    private void hero(Canvas c,float y){
        if(theme.is("date","tiles")){box(c,38,y-2,31,27,false);box(c,75,y-2,31,27,false);text(c,"0",44,y+20,24);text(c,"5",81,y+20,24);}
        else if(theme.is("date","center")){text(c,"05",57,y+18,26);text(c,"Thursday",56,y+29,7);}
        else if(theme.is("date","reverse")){text(c,"November",12,y+8,8);text(c,"Thursday",12,y+19,8);text(c,"05",100,y+21,29);}
        else{text(c,"05",12,y+21,theme.is("date","masthead")?34:29);text(c,"November",76,y+8,8);text(c,"Thursday",76,y+19,8);if(theme.is("date","masthead"))line(c,76,y-2,132,y-2);}
    }
    private void calendar(Canvas c,float y,float height){
        if(weekly){styleWeek(c,10,y,128,height,false);return;}
        float step=height/3;
        for(int r=0;r<3;r++){
            for(int col=0;col<7;col++){
                float x=15+col*19,top=y+r*step;boolean selected=r==1&&col==3;
                if(selected&&theme.is("calendar","open")){
                    paint.setStyle(Paint.Style.FILL);c.drawCircle(x+3,top+step/2-1,Math.min(6,step/2),paint);paint.setColor(Color.WHITE);
                }
                text(c,String.valueOf(r*7+col+1),x,top+step/2+2,Math.min(8,step*.7f));paint.setColor(Color.BLACK);
                if(selected&&theme.is("calendar","plain"))line(c,x-1,top+step/2+5,x+9,top+step/2+5);
            }
            if(theme.is("calendar","ruled"))line(c,10,y+(r+1)*step-1,138,y+(r+1)*step-1);
        }
    }
    private void agenda(Canvas c,float y,float height){
        float step=height/3;
        for(int i=0;i<3;i++){
            float top=y+i*step;if(theme.is("agenda","cards"))box(c,10,top,128,step-2,false);
            if(theme.is("agenda","timeline")){line(c,13,top+4,32,top+4);line(c,38,top+1,38,top+step-2);line(c,46,top+4,126,top+4);}
            else if(theme.is("agenda","ledger")){line(c,13,top+4,91,top+4);line(c,110,top+4,134,top+4);}
            else{line(c,14,top+3,45,top+3);line(c,14,top+Math.max(5,step*.55f),118,top+Math.max(5,step*.55f));}
        }
    }
}
