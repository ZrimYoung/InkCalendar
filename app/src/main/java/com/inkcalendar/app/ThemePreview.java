package com.inkcalendar.app;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
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
    private final DashPathEffect dashes=new DashPathEffect(new float[]{3,3},0);
    private final Typeface regular,bold;

    ThemePreview(Context context,ThemeConfig theme,String component){
        this(context,theme,component,DisplayOptions.read(context.getSharedPreferences("display",Context.MODE_PRIVATE)));
    }
    ThemePreview(Context context,ThemeConfig theme,String component,DisplayOptions options){
        super(context);this.theme=new ThemeConfig(theme);this.component=component;
        weekly="week".equals(options.view);balanced="balanced".equals(options.layout);
        ThemeFonts fonts=new ThemeFonts(context.getAssets());
        regular=fonts.latin(component==null?"original":theme.font,false);bold=fonts.latin(component==null?"original":theme.font,true);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    private boolean landscape(){return getResources().getConfiguration().orientation==Configuration.ORIENTATION_LANDSCAPE;}
    private boolean wideLandscape(){return landscape()&&getResources().getConfiguration().screenWidthDp>=SharedThemeCatalog.WIDE_LAYOUT_MIN;}
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
        dateSample(c,10,27,width-20,50,false);
        if(theme.is("flow","agenda-first")){styleAgenda(c,10,84,width-20,13);styleCalendar(c,10,110,width-20,38,3,false,false);}
        else{styleCalendar(c,10,80,width-20,38,3,false,false);styleAgenda(c,10,123,width-20,13);}
    }

    private void landscapeIdentity(Canvas c,float x,float y,float width){dateSample(c,x,y,width,70,true);}

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
            if(theme.is("agenda","cards"))dashed(c,x,top+step-1,x+width);
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
            if(wideLandscape()&&balanced){
                boolean first=theme.is("flow","agenda-first");float side=first?10:82,calendar=first?82:10;
                text(c,"07",side,32,28,true,Paint.Align.LEFT);styleAgenda(c,side,43,56,21);styleCalendar(c,calendar,12,56,73,6,true,false);
            }else if(wideLandscape()){
                if(theme.is("flow","agenda-first")){
                    text(c,"06",12,36,28,true,Paint.Align.LEFT);styleAgenda(c,62,15,76,14);
                    styleCalendar(c,10,54,128,38,3,false,false);
                }else{styleAgenda(c,10,48,48,14);styleCalendar(c,66,15,72,74,6,true,true);}
            }else if(theme.is("flow","agenda-first")){agenda(c,10,30);calendar(c,49,40);}
            else{calendar(c,10,40);agenda(c,58,30);}
        }else{
            String[] samples="tracking".equals(component)?new String[]{theme.is("caps","on")?"CALENDAR":"Calendar"}:theme.is("caps","on")?new String[]{"NOVEMBER","SATURDAY"}:new String[]{"November","Saturday"};
            float spacing=theme.is("tracking","spaced")?2:theme.is("tracking","tight")?-.5f:0,size="tracking".equals(component)?17:14;
            for(int row=0;row<samples.length;row++){
                String value=samples[row];paint.setTypeface(regular);paint.setTextSize(size);float x=(148-paint.measureText(value)-spacing*(value.length()-1))/2;
                for(char ch:value.toCharArray()){text(c,String.valueOf(ch),x,samples.length==1?55:42+row*22,size);x+=paint.measureText(String.valueOf(ch))+spacing;}
            }
        }
    }

    private void hero(Canvas c,float y){dateSample(c,10,y-17,128,70,false);}
    private void sampleText(Canvas c,String value,float x,float y,float size,float width,boolean strong,Paint.Align align){
        paint.setTypeface(strong?bold:regular);paint.setTextSize(size);
        float fit=Math.min(1,width/Math.max(1,paint.measureText(value)));text(c,value,x,y,size*fit,strong,align);
    }
    private void dateSample(Canvas c,float x,float y,float w,float h,boolean column){
        String weekday=theme.is("caps","on")?"SATURDAY":"Saturday";
        if(theme.is("date","masthead")){
            sampleText(c,"November",x,y+h*.14f,h*.14f,w,false,Paint.Align.LEFT);line(c,x,y+h*.24f,x+w,y+h*.24f);
            sampleText(c,"07",x,y+h*.77f,h*.72f,w,true,Paint.Align.LEFT);box(c,x,y+h*.84f,w,h*.16f,true);paint.setColor(Color.WHITE);
            sampleText(c,weekday,x+w/2,y+h*.96f,h*.11f,w-6,false,Paint.Align.CENTER);paint.setColor(Color.BLACK);
        }else if(theme.is("date","tiles")){
            sampleText(c,"0",x+w*.26f,y+h*.64f,h*.74f,w*.44f,true,Paint.Align.CENTER);sampleText(c,"7",x+w*.73f,y+h*.84f,h*.74f,w*.44f,true,Paint.Align.CENTER);
            sampleText(c,"November",x+w/2,y+h*.99f,h*.12f,w,false,Paint.Align.CENTER);
        }else if(theme.is("date","center")){
            sampleText(c,"07",x+w/2,y+h*.6f,h*.72f,w,true,Paint.Align.CENTER);sampleText(c,"November",x+w/2,y+h*.8f,h*.12f,w,false,Paint.Align.CENTER);sampleText(c,weekday,x+w/2,y+h*.98f,h*.13f,w,false,Paint.Align.CENTER);
        }else if(theme.is("date","reverse")){
            sampleText(c,"November",x,y+h*.3f,h*.14f,w*.5f,false,Paint.Align.LEFT);sampleText(c,weekday,x,y+h*.58f,h*.15f,w*.5f,false,Paint.Align.LEFT);sampleText(c,"07",x+w,y+h*.75f,h*.68f,w*.46f,true,Paint.Align.RIGHT);
        }else if(column){
            sampleText(c,"November",x,y+h*.14f,h*.14f,w,false,Paint.Align.LEFT);sampleText(c,"07",x,y+h*.7f,h*.64f,w,true,Paint.Align.LEFT);sampleText(c,weekday,x,y+h*.98f,h*.13f,w,false,Paint.Align.LEFT);
        }else{
            sampleText(c,"07",x,y+h*.74f,h*.64f,w*.48f,true,Paint.Align.LEFT);sampleText(c,"November",x+w*.54f,y+h*.3f,h*.14f,w*.46f,false,Paint.Align.LEFT);sampleText(c,weekday,x+w*.54f,y+h*.6f,h*.15f,w*.46f,false,Paint.Align.LEFT);
        }
    }
    private void dashed(Canvas c,float x,float y,float right){paint.setPathEffect(dashes);line(c,x,y,right,y);paint.setPathEffect(null);}
    private void calendar(Canvas c,float y,float height){
        if(weekly){styleWeek(c,10,y,128,height,false);return;}
        float step=height/3;
        for(int r=0;r<3;r++){
            for(int col=0;col<7;col++){
                float x=15+col*19,top=y+r*step;boolean selected=r==1&&col==3;
                if(selected&&theme.is("calendar","ruled")){box(c,x-5,top-1,19,step,true);paint.setColor(Color.WHITE);}
                else if(selected&&theme.is("calendar","open")){
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
            float top=y+i*step;if(theme.is("agenda","cards"))dashed(c,10,top+step-2,138);
            if(theme.is("agenda","timeline")){line(c,13,top+4,32,top+4);line(c,38,top+1,38,top+step-2);line(c,46,top+4,126,top+4);}
            else if(theme.is("agenda","ledger")){line(c,13,top+4,91,top+4);line(c,110,top+4,134,top+4);}
            else{line(c,14,top+3,45,top+3);line(c,14,top+Math.max(5,step*.55f),118,top+Math.max(5,step*.55f));}
        }
    }
}
