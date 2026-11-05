package com.inkcalendar.app;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Typeface;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.widget.CheckBox;

/** Reserve translated label height just as StableTextView does, including the check mark. */
@SuppressLint("ViewConstructor")
final class StableCheckBox extends CheckBox {
    private String alternate="";
    private final TextPaint alternatePaint=new TextPaint();
    private int measuredWidth=-1, alternateHeight;
    private float measuredSize=-1;
    private Typeface chinese;
    StableCheckBox(Context context) {super(context);}
    void useChinese(Typeface face){chinese=face;setText(ThemeFonts.mixed(getText(),face,false));}
    void reserveFor(String text,Typeface face) {
        alternate=text;alternatePaint.setTypeface(face);measuredWidth=-1;requestLayout();
    }
    @Override protected void onMeasure(int widthSpec,int heightSpec) {
        super.onMeasure(widthSpec,heightSpec);
        int width=getMeasuredWidth()-getCompoundPaddingLeft()-getCompoundPaddingRight();
        if(width<=0 || alternate.isEmpty() || MeasureSpec.getMode(heightSpec)==MeasureSpec.EXACTLY)return;
        if(width!=measuredWidth || getTextSize()!=measuredSize) measureAlternate(width);
        setMeasuredDimension(getMeasuredWidth(),resolveSize(Math.max(getMeasuredHeight(),alternateHeight+getCompoundPaddingTop()+getCompoundPaddingBottom()),heightSpec));
    }
    @SuppressWarnings("deprecation")
    private void measureAlternate(int width) {
        measuredWidth=width;measuredSize=getTextSize();alternatePaint.setTextSize(getTextSize());
        alternateHeight=new StaticLayout(StableTextView.uniformLines(ThemeFonts.mixed(alternate,chinese,false),this),alternatePaint,width,Layout.Alignment.ALIGN_NORMAL,getLineSpacingMultiplier(),getLineSpacingExtra(),false).getHeight();
    }
}
