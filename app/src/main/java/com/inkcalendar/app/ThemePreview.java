package com.inkcalendar.app;

import android.annotation.SuppressLint;
import android.content.Context;
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
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Typeface regular,bold;

    ThemePreview(Context context,ThemeConfig theme,String component){
        super(context);this.theme=new ThemeConfig(theme);this.component=component;
        ThemeFonts fonts=new ThemeFonts(context.getAssets());
        regular=fonts.latin("original",false);bold=fonts.latin("original",true);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    private float previewWidth(){return component==null?188:148;}
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
    private int ruleWidth(){return Math.max(1,Math.round(theme.ruleWidth()/2f));}
    private void words(Canvas c,float x,float y){
        text(c,"November",x,y,7);text(c,theme.is("caps","on")?"FRIDAY":"Friday",x,y+13,10);
    }

    private void style(Canvas c){
        text(c,theme.is("caps","on")?"INK CALENDAR":"Ink Calendar",10,12,8,true,Paint.Align.LEFT);
        line(c,10,19,178,19,ruleWidth());
        if(theme.is("date","tiles")){
            box(c,48,26,43,35,false);box(c,97,26,43,35,false);
            text(c,"0",69,53,30,true,Paint.Align.CENTER);text(c,"6",118,53,30,true,Paint.Align.CENTER);
            text(c,"November · Friday",94,71,6.5f,false,Paint.Align.CENTER);
        }else if(theme.is("date","center")){
            text(c,"06",94,53,36,true,Paint.Align.CENTER);text(c,"Friday",94,66,8,false,Paint.Align.CENTER);
        }else if(theme.is("date","reverse")){
            words(c,10,35);text(c,"06",178,56,34,true,Paint.Align.RIGHT);
        }else{
            boolean masthead=theme.is("date","masthead");
            text(c,"06",10,59,masthead?40:34,true,Paint.Align.LEFT);words(c,104,35);
            if(masthead)line(c,104,56,178,56);
        }
        if(theme.is("flow","agenda-first")){styleAgenda(c,84);styleCalendar(c,110);}
        else{styleCalendar(c,80);styleAgenda(c,123);}
    }

    private void styleCalendar(Canvas c,float y){
        for(int col=0;col<7;col++)text(c,WEEK_DAYS[col],20+col*24,y,5.5f,false,Paint.Align.CENTER);
        for(int row=0;row<3;row++){
            float top=y+5+row*10;
            for(int col=0;col<7;col++){
                float x=20+col*24;boolean selected=row==0&&col==4;
                if(selected&&theme.is("calendar","ruled")){box(c,x-10,top-1,20,10,true);paint.setColor(Color.WHITE);}
                else if(selected&&theme.is("calendar","open")){
                    paint.setStyle(Paint.Style.FILL);c.drawCircle(x,top+4,5,paint);paint.setColor(Color.WHITE);
                }
                text(c,String.valueOf(row*7+col+2),x,top+7,7,selected,Paint.Align.CENTER);paint.setColor(Color.BLACK);
                if(selected&&theme.is("calendar","plain"))line(c,x-5,top+9,x+5,top+9);
            }
            if(theme.is("calendar","ruled"))line(c,10,top+9,178,top+9,.5f);
        }
    }

    private void styleAgenda(Canvas c,float y){
        line(c,10,y-3,178,y-3,ruleWidth());
        for(int i=0;i<2;i++){
            float top=y+i*13;String time=i==0?"09:30":"14:00",name=i==0?"Read a chapter":"Weekly plan";
            if(theme.is("agenda","cards"))box(c,10,top-1,168,12,false);
            if(theme.is("agenda","timeline")){text(c,time,12,top+7,6);text(c,name,54,top+7,7);}
            else if(theme.is("agenda","ledger")){text(c,name,13,top+7,7);text(c,time,174,top+7,6,false,Paint.Align.RIGHT);}
            else{text(c,time,12,top+3,5);text(c,name,12,top+10,6.5f);}
        }
    }

    private void component(Canvas c){
        if("date".equals(component))hero(c,30);
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
            if(theme.is("flow","agenda-first")){agenda(c,10,30);calendar(c,49,40);}
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
