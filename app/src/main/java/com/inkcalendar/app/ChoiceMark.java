package com.inkcalendar.app;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;

/** Monochrome check/radio mark, with native CompoundButton semantics. */
final class ChoiceMark extends Drawable {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final boolean radio;
    private final int size;
    private boolean checked;
    ChoiceMark(float density, boolean radio) { this.radio = radio; size = Math.round(32 * density); }
    @Override public void draw(Canvas canvas) {
        int save = canvas.save();
        canvas.translate(getBounds().left, getBounds().top);
        canvas.scale(getBounds().width() / 32f, getBounds().height() / 32f);
        paint.setColor(Color.BLACK); paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(1.5f);
        if (radio) canvas.drawCircle(12, 16, 9, paint);
        else canvas.drawRect(3, 7, 21, 25, paint);
        if (checked) {
            if (radio) { paint.setStyle(Paint.Style.FILL); canvas.drawCircle(12, 16, 5, paint); }
            else {
                paint.setStrokeWidth(2);
                canvas.drawLine(6, 15, 11, 20, paint); canvas.drawLine(11, 20, 18, 11, paint);
            }
        }
        canvas.restoreToCount(save);
    }
    @Override public boolean isStateful() { return true; }
    @Override protected boolean onStateChange(int[] states) {
        boolean next = false;
        for (int state : states) if (state == android.R.attr.state_checked) next = true;
        if (next == checked) return false;
        checked = next; invalidateSelf(); return true;
    }
    @Override public int getIntrinsicWidth() { return size; }
    @Override public int getIntrinsicHeight() { return size; }
    @Override public void setAlpha(int alpha) { paint.setAlpha(alpha); invalidateSelf(); }
    @Override public void setColorFilter(ColorFilter filter) { paint.setColorFilter(filter); invalidateSelf(); }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
