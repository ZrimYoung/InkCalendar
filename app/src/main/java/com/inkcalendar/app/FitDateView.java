package com.inkcalendar.app;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.TextPaint;
import android.util.TypedValue;
import android.widget.TextView;

/** Fit large decorative dates to their column without relying on API 26 auto-size. */
@SuppressLint("ViewConstructor")
final class FitDateView extends TextView {
    private float maximum;
    private final TextPaint measure=new TextPaint();
    FitDateView(Context context){super(context);setIncludeFontPadding(false);setMaxLines(1);setHorizontallyScrolling(false);}
    void maximum(float sp){maximum=sp*getResources().getDisplayMetrics().scaledDensity;setTextSize(TypedValue.COMPLEX_UNIT_PX,maximum);}
    @Override protected void onMeasure(int width,int height){
        float available=MeasureSpec.getSize(width)-getCompoundPaddingLeft()-getCompoundPaddingRight()-2*getResources().getDisplayMetrics().density;
        if(MeasureSpec.getMode(width)==MeasureSpec.UNSPECIFIED)setTextSize(TypedValue.COMPLEX_UNIT_PX,maximum);
        else if(available>0){measure.set(getPaint());measure.setTextSize(maximum);float needed=measure.measureText(getText().toString());setTextSize(TypedValue.COMPLEX_UNIT_PX,Math.min(maximum,maximum*available/Math.max(1,needed)));}
        super.onMeasure(width,height);
    }
}
