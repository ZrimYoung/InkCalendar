package com.inkcalendar.app;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

/** High contrast switch with a separate thumb position; no animation on e-ink. */
final class ToggleMark extends Drawable {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float density;
    private boolean checked;
    ToggleMark(float density){this.density=density;setBounds(0,0,Math.round(44*density),Math.round(28*density));}
    @Override public boolean isStateful(){return true;}
    @Override protected boolean onStateChange(int[] states){boolean value=false;for(int s:states)if(s==android.R.attr.state_checked)value=true;if(value==checked)return false;checked=value;invalidateSelf();return true;}
    @Override public void draw(Canvas c){
        int save=c.save();c.translate(getBounds().left,getBounds().top);c.scale(getBounds().width()/44f,getBounds().height()/28f);
        RectF track=new RectF(1,1,43,27);paint.setStyle(Paint.Style.FILL);paint.setColor(checked?Color.BLACK:Color.WHITE);c.drawRoundRect(track,13,13,paint);
        paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(1.5f);paint.setColor(Color.BLACK);c.drawRoundRect(track,13,13,paint);
        paint.setStyle(Paint.Style.FILL);paint.setColor(checked?Color.WHITE:Color.BLACK);c.drawCircle(checked?31:13,14,9,paint);c.restoreToCount(save);
    }
    @Override public int getIntrinsicWidth(){return Math.round(44*density);}
    @Override public int getIntrinsicHeight(){return Math.round(28*density);}
    @Override public void setAlpha(int alpha){paint.setAlpha(alpha);invalidateSelf();}
    @Override public void setColorFilter(ColorFilter filter){paint.setColorFilter(filter);invalidateSelf();}
    @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}
