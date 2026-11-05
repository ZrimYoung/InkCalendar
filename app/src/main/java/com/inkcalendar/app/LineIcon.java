package com.inkcalendar.app;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

/** Small line icons drawn without vector drawable or font-glyph dependencies. */
final class LineIcon extends Drawable {
    static final int PREVIOUS = 0, NEXT = 1, TODAY = 2, ADD = 3, SETTINGS = 4;
    private final int kind, size;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    LineIcon(int kind, float density) {
        this.kind = kind; this.size = (int) (24 * density + 0.5f);
        paint.setColor(Color.BLACK); paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1.6f); paint.setStrokeCap(Paint.Cap.ROUND); paint.setStrokeJoin(Paint.Join.ROUND);
    }
    @Override public void draw(Canvas canvas) {
        int save = canvas.save();
        canvas.translate(getBounds().left, getBounds().top);
        canvas.scale(getBounds().width() / 24f, getBounds().height() / 24f);
        if (kind == PREVIOUS || kind == NEXT) {
            float a = kind == PREVIOUS ? 14 : 10, b = kind == PREVIOUS ? 9 : 15;
            canvas.drawLine(a, 6, b, 12, paint); canvas.drawLine(b, 12, a, 18, paint);
        } else if (kind == ADD) {
            canvas.drawLine(5, 12, 19, 12, paint); canvas.drawLine(12, 5, 12, 19, paint);
        } else if (kind == TODAY) {
            canvas.drawRoundRect(new RectF(4, 5, 20, 21), 2, 2, paint);
            canvas.drawLine(8, 3, 8, 7, paint); canvas.drawLine(16, 3, 16, 7, paint);
            canvas.drawLine(4, 10, 20, 10, paint); canvas.drawCircle(12, 15.5f, 2, paint);
        } else {
            canvas.drawLine(4, 7, 9, 7, paint); canvas.drawLine(13, 7, 20, 7, paint);
            canvas.drawCircle(11, 7, 2, paint);
            canvas.drawLine(4, 17, 13, 17, paint); canvas.drawLine(17, 17, 20, 17, paint);
            canvas.drawCircle(15, 17, 2, paint);
        }
        canvas.restoreToCount(save);
    }
    @Override public int getIntrinsicWidth() { return size; }
    @Override public boolean isStateful() { return true; }
    @Override protected boolean onStateChange(int[] state) {
        boolean pressed = false;
        for (int value : state) if (value == android.R.attr.state_pressed) pressed = true;
        int color = pressed ? Color.WHITE : Color.BLACK;
        if (paint.getColor() == color) return false;
        paint.setColor(color); invalidateSelf(); return true;
    }
    @Override public int getIntrinsicHeight() { return size; }
    @Override public void setAlpha(int alpha) { paint.setAlpha(alpha); invalidateSelf(); }
    @Override public void setColorFilter(ColorFilter filter) { paint.setColorFilter(filter); invalidateSelf(); }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
