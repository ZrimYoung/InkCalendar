package com.inkcalendar.app;

import android.content.Context;
import android.view.View;
import android.view.ViewParent;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ScrollView;

/** Fits only the monthly calendar; the surrounding date and agenda keep their layout. */
final class MonthFitLayout extends FrameLayout {
    private boolean fitting;
    private float contentScale=1;
    private final int[] here=new int[2],viewportAt=new int[2];
    private final ViewTreeObserver.OnGlobalLayoutListener fitListener=this::fit;
    MonthFitLayout(Context context){super(context);}
    void setFitting(boolean value){if(fitting==value)return;fitting=value;if(!value)contentScale=1;requestLayout();}
    float contentScale(){return contentScale;}
    static float scaleFor(int natural,int available){return natural<=0?1:Math.min(1,Math.max(SharedThemeCatalog.CALENDAR_FIT_MIN/100f,available/(float)natural));}
    @Override protected void onAttachedToWindow(){super.onAttachedToWindow();getViewTreeObserver().addOnGlobalLayoutListener(fitListener);}
    @Override protected void onDetachedFromWindow(){if(getViewTreeObserver().isAlive())getViewTreeObserver().removeOnGlobalLayoutListener(fitListener);super.onDetachedFromWindow();}
    @Override protected void onMeasure(int widthSpec,int heightSpec){
        if(getChildCount()==0){super.onMeasure(widthSpec,heightSpec);return;}
        View content=getChildAt(0);float scale=fitting?contentScale:1;
        int width=MeasureSpec.getSize(widthSpec),available=Math.max(0,width-getPaddingLeft()-getPaddingRight());
        // Compensate the source width so the rendered calendar keeps its original column width.
        content.measure(MeasureSpec.makeMeasureSpec(Math.round(available/scale),MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(0,MeasureSpec.UNSPECIFIED));
        content.setPivotX(0);content.setPivotY(0);content.setScaleX(scale);content.setScaleY(scale);
        setMeasuredDimension(resolveSize(width,widthSpec),resolveSize(Math.round(content.getMeasuredHeight()*scale)+getPaddingTop()+getPaddingBottom(),heightSpec));
    }
    private void fit(){
        if(!fitting||getChildCount()==0)return;
        ScrollView viewport=null;
        for(ViewParent parent=getParent();parent!=null;parent=parent.getParent())if(parent instanceof ScrollView){viewport=(ScrollView)parent;break;}
        if(viewport==null||viewport.getHeight()==0)return;
        getLocationInWindow(here);viewport.getLocationInWindow(viewportAt);
        int top=here[1]-viewportAt[1]+viewport.getScrollY();
        int gap=Math.round(SharedThemeCatalog.CALENDAR_FIT_BOTTOM_GAP*getResources().getDisplayMetrics().density);
        int available=viewport.getHeight()-viewport.getPaddingBottom()-top-gap;
        float scale=scaleFor(getChildAt(0).getMeasuredHeight(),available);
        if(Math.abs(contentScale-scale)>.002f){contentScale=scale;requestLayout();}
    }
}
