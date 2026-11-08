package com.inkcalendar.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.widget.SeekBar;

/** Keeps native touch/accessibility behaviour with a quiet, monochrome e-ink skin. */
final class InkSeekBar extends SeekBar {
    InkSeekBar(Context context){
        super(context);
        float density=getResources().getDisplayMetrics().density;
        InkColors colors=InkColors.from(context);
        setBackground(null);setProgressDrawable(new Track(density,colors));setThumb(new Thumb(density,colors));
        setThumbOffset(Math.round(12*density));setPadding(Math.round(12*density),0,Math.round(12*density),0);
        setMinimumHeight(Math.round(44*density));
        if(Build.VERSION.SDK_INT>=21){setThumbTintList(null);setProgressTintList(null);setProgressBackgroundTintList(null);setSplitTrack(false);}
    }
    @Override protected synchronized void onMeasure(int widthSpec,int heightSpec){
        super.onMeasure(widthSpec,heightSpec);
        int minimum=Math.round(44*getResources().getDisplayMetrics().density);
        setMeasuredDimension(getMeasuredWidth(),resolveSize(Math.max(minimum,getMeasuredHeight()),heightSpec));
    }
    private abstract static class InkDrawable extends Drawable {
        final Paint ink=new Paint(Paint.ANTI_ALIAS_FLAG);final float density;final InkColors colors;
        InkDrawable(float density,InkColors colors){this.colors=colors;this.density=density;}
        @Override public void setAlpha(int value){ink.setAlpha(value);invalidateSelf();}
        @Override public void setColorFilter(ColorFilter value){ink.setColorFilter(value);invalidateSelf();}
        @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
    }
    private static final class Track extends InkDrawable {
        private final RectF bar=new RectF();
        Track(float density,InkColors colors){super(density,colors);}
        @Override public void draw(Canvas canvas){
            float y=getBounds().exactCenterY(),left=getBounds().left,right=getBounds().right;
            bar.set(left,y-3*density,right,y+3*density);ink.setStyle(Paint.Style.FILL);ink.setColor(colors.track);canvas.drawRoundRect(bar,3*density,3*density,ink);
            ink.setStyle(Paint.Style.STROKE);ink.setStrokeWidth(density);ink.setColor(colors.edge);canvas.drawRoundRect(bar,3*density,3*density,ink);
            if(getLevel()>0){bar.right=left+(right-left)*getLevel()/10000f;ink.setStyle(Paint.Style.FILL);ink.setColor(colors.ink);canvas.drawRoundRect(bar,3*density,3*density,ink);}
        }
        @Override protected boolean onLevelChange(int value){invalidateSelf();return true;}
        @Override public int getIntrinsicWidth(){return Math.round(64*density);}
        @Override public int getIntrinsicHeight(){return Math.round(6*density);}
    }
    private static final class Thumb extends InkDrawable {
        private boolean focused,enabled=true;
        Thumb(float density,InkColors colors){super(density,colors);}
        @Override public boolean isStateful(){return true;}
        @Override protected boolean onStateChange(int[] states){
            focused=false;enabled=false;
            for(int state:states){if(state==android.R.attr.state_focused)focused=true;if(state==android.R.attr.state_enabled)enabled=true;}
            invalidateSelf();return true;
        }
        @Override public void draw(Canvas canvas){
            float x=getBounds().exactCenterX(),y=getBounds().exactCenterY();
            ink.setStyle(Paint.Style.FILL);ink.setColor(enabled?colors.ink:colors.disabled);canvas.drawCircle(x,y,8*density,ink);
            if(focused){ink.setStyle(Paint.Style.STROKE);ink.setStrokeWidth(density);canvas.drawCircle(x,y,11*density,ink);}
        }
        @Override public int getIntrinsicWidth(){return Math.round(24*density);}
        @Override public int getIntrinsicHeight(){return Math.round(24*density);}
    }
}
