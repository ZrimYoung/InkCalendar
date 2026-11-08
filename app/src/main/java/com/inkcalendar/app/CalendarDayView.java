package com.inkcalendar.app;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;

/** Date selection, event dot and annotation have separate slots, independent of font glyph metrics. */
@SuppressLint("ViewConstructor")
final class CalendarDayView extends LinearLayout {
    final StableTextView number,detail;
    final View marker;
    private final Paint ink=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final GradientDrawable dot=new GradientDrawable();
    private final String style;
    private final boolean today;
    private final InkColors colors;
    private final int dotSize;
    CalendarDayView(Context context,String style,String day,String annotation,boolean marked,boolean today,
                    boolean chosen,boolean compact,float scale,Typeface latin,Typeface bold,Typeface chinese){
        super(context);this.colors=InkColors.from(context);this.style=style;this.today=today;setOrientation(VERTICAL);setGravity(Gravity.CENTER);setWillNotDraw(false);
        dotSize=dp(4);float system=Math.max(1f,getResources().getConfiguration().fontScale);
        number=new StableTextView(context);number.setGravity(Gravity.CENTER);number.setTextSize(22*scale);
        number.setTypeface(today?bold:latin);number.useChinese(chinese,today);number.setText(day);
        addView(number,new LayoutParams(-1,dp(36*scale*system)));
        marker=new View(context);dot.setShape(GradientDrawable.OVAL);marker.setBackground(dot);LayoutParams mark=new LayoutParams(dotSize,dotSize);mark.topMargin=dp(4);mark.bottomMargin=dp(2);addView(marker,mark);
        marker.setVisibility(marked?VISIBLE:INVISIBLE);
        detail=new StableTextView(context);detail.setGravity(Gravity.CENTER);detail.setTextSize(11*scale);detail.setTypeface(latin);detail.useChinese(chinese,false);detail.setText(annotation);detail.setMaxLines(1);
        addView(detail,new LayoutParams(-1,compact?0:dp(20*scale*system)));
        setSelected(chosen);setMinimumHeight(dp((compact?52:70)*scale*system));updateColors();
    }
    private int dp(float value){return Math.round(value*getResources().getDisplayMetrics().density);}
    private boolean filled(){return "ruled".equals(style)&&(isSelected()||isPressed());}
    private void updateColors(){if(colors==null||number==null||detail==null||marker==null)return;number.setTextColor((filled()||"open".equals(style)&&(isSelected()||isPressed()))?colors.paper:colors.ink);detail.setTextColor(filled()?colors.paper:colors.ink);dot.setColor(filled()?colors.paper:colors.ink);}
    @Override protected void drawableStateChanged(){super.drawableStateChanged();updateColors();invalidate();}
    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);ink.setColor(colors.paper);ink.setStyle(Paint.Style.FILL);canvas.drawRect(0,0,getWidth(),getHeight(),ink);ink.setColor(colors.ink);
        boolean active=isSelected()||isPressed();
        if(filled())canvas.drawRect(0,0,getWidth(),getHeight(),ink);
        else if(active&&"open".equals(style))canvas.drawCircle(getWidth()/2f,number.getTop()+number.getHeight()/2f,Math.min(getWidth()-dp(4),number.getHeight())/2f,ink);
        else if(active&&"plain".equals(style)){float width=Math.min(getWidth()-dp(8),dp(32));canvas.drawRect((getWidth()-width)/2,number.getBottom()-dp(2),(getWidth()+width)/2,number.getBottom(),ink);}
        if(today&&!active&&"ruled".equals(style)){ink.setStyle(Paint.Style.STROKE);ink.setStrokeWidth(dp(2));canvas.drawRect(dp(1),dp(1),getWidth()-dp(1),getHeight()-dp(1),ink);}
        if(isFocused()){ink.setStyle(Paint.Style.STROKE);ink.setStrokeWidth(dp(2));ink.setColor(filled()?colors.paper:colors.ink);canvas.drawRect(dp(2),dp(2),getWidth()-dp(2),getHeight()-dp(2),ink);}
    }
}
