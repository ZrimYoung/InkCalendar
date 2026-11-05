package com.inkcalendar.app;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;

final class InkRule extends Drawable {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float density;
    private final boolean white;
    InkRule(float density,boolean dashed,boolean white){this.density=density;this.white=white;if(dashed)paint.setPathEffect(new DashPathEffect(new float[]{3*density,3*density},0));}
    @Override public void draw(Canvas canvas){if(white)canvas.drawColor(Color.WHITE);paint.setColor(Color.BLACK);paint.setStrokeWidth(density);canvas.drawLine(getBounds().left,getBounds().bottom-density/2,getBounds().right,getBounds().bottom-density/2,paint);}
    @Override public void setAlpha(int alpha){paint.setAlpha(alpha);invalidateSelf();}
    @Override public void setColorFilter(ColorFilter filter){paint.setColorFilter(filter);invalidateSelf();}
    @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}
