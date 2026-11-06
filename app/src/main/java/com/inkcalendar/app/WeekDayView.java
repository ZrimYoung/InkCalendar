package com.inkcalendar.app;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.view.Gravity;
import android.widget.LinearLayout;

/** One day of the weekly calendar; the home date and agenda stay outside this view. */
@SuppressLint("ViewConstructor")
final class WeekDayView extends LinearLayout {
    WeekDayView(Context context,String style,String number,String summary,String meta,String count,
                boolean marked,boolean today,boolean selected,float scale,Typeface latin,Typeface bold,Typeface chinese){
        super(context);setOrientation(HORIZONTAL);setGravity(Gravity.CENTER_VERTICAL);setMinimumHeight(dp(66*scale));
        CalendarDayView badge=new CalendarDayView(context,style,number,"",marked,today,selected,true,scale,latin,bold,chinese);
        badge.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);addView(badge,new LayoutParams(dp(52*scale),-1));
        LinearLayout copy=new LinearLayout(context);copy.setOrientation(VERTICAL);copy.setPadding(dp(10),dp(8),dp(8),dp(8));
        StableTextView title=text(context,summary,15*scale,latin,chinese);title.setMaxLines(1);title.setEllipsize(TextUtils.TruncateAt.END);copy.addView(title);
        StableTextView detail=text(context,meta,11*scale,latin,chinese);detail.setMaxLines(1);detail.setEllipsize(TextUtils.TruncateAt.END);copy.addView(detail);
        addView(copy,new LayoutParams(0,-2,1));StableTextView total=text(context,count,12*scale,latin,chinese);total.setGravity(Gravity.END);addView(total,new LayoutParams(-2,-2));
        setFocusable(true);setClickable(true);
    }
    private StableTextView text(Context c,String value,float size,Typeface latin,Typeface chinese){StableTextView t=new StableTextView(c);t.setText(value);t.setTextSize(size);t.setTypeface(latin);t.useChinese(chinese,false);t.setTextColor(Color.BLACK);t.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);return t;}
    private int dp(float n){return Math.round(n*getResources().getDisplayMetrics().density);}
}
