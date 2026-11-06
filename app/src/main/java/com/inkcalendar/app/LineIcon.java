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
    static final int PREVIOUS = 0, NEXT = 1, TODAY = 2, ADD = 3, SETTINGS = 4, DELETE = 5, LIST = 6, MILESTONE = 7, CANCEL = 8, SAVE = 9;
    static final int DISPLAY=10,TYPE=11,MOON=12,TERMS=13,FLAG=14,LANGUAGE=15,PALETTE=16,MINUS=17,LAYOUT=18,ROTATE=19,SUN=20;
    private final int kind, size;
    private final boolean inverse;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    LineIcon(int kind, float density) { this(kind,density,false); }
    LineIcon(int kind, float density, boolean inverse) {
        this.inverse=inverse;
        this.kind = kind; this.size = (int) (24 * density + 0.5f);
        paint.setColor(inverse?Color.WHITE:Color.BLACK); paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1.6f); paint.setStrokeCap(Paint.Cap.ROUND); paint.setStrokeJoin(Paint.Join.ROUND);
    }
    @Override public void draw(Canvas canvas) {
        int save = canvas.save();
        canvas.translate(getBounds().left, getBounds().top);
        canvas.scale(getBounds().width() / 24f, getBounds().height() / 24f);
        if (kind == PREVIOUS || kind == NEXT) {
            float a = kind == PREVIOUS ? 14 : 10, b = kind == PREVIOUS ? 9 : 15;
            canvas.drawLine(a, 6, b, 12, paint); canvas.drawLine(b, 12, a, 18, paint);
        } else if (kind == CANCEL) {
            canvas.drawLine(6,6,18,18,paint);canvas.drawLine(18,6,6,18,paint);
        } else if (kind == SAVE) {
            canvas.drawLine(4,12,10,18,paint);canvas.drawLine(10,18,21,5,paint);
        } else if (kind == ADD) {
            canvas.drawLine(5, 12, 19, 12, paint); canvas.drawLine(12, 5, 12, 19, paint);
        } else if (kind == MINUS) {
            canvas.drawLine(5,12,19,12,paint);
        } else if (kind == DISPLAY) {
            canvas.drawRect(3,4,21,17,paint);canvas.drawLine(12,17,12,21,paint);canvas.drawLine(7,21,17,21,paint);
        } else if (kind == SUN) {
            canvas.drawCircle(12,12,4,paint);for(int i=0;i<8;i++){double a=i*Math.PI/4;canvas.drawLine(12+7*(float)Math.cos(a),12+7*(float)Math.sin(a),12+10*(float)Math.cos(a),12+10*(float)Math.sin(a),paint);}
        } else if (kind == TYPE) {
            canvas.drawLine(4,5,20,5,paint);canvas.drawLine(12,5,12,20,paint);canvas.drawLine(8,20,16,20,paint);
        } else if (kind == MOON) {
            canvas.drawArc(new RectF(3,3,21,21),55,285,false,paint);canvas.drawArc(new RectF(9,0,24,16),85,170,false,paint);
        } else if (kind == TERMS) {
            canvas.drawLine(12,21,12,10,paint);canvas.drawOval(new RectF(3,5,12,12),paint);canvas.drawOval(new RectF(12,2,21,9),paint);canvas.drawLine(6,21,18,21,paint);
        } else if (kind == FLAG) {
            canvas.drawLine(5,3,5,22,paint);canvas.drawRect(5,4,20,13,paint);
        } else if (kind == LANGUAGE) {
            canvas.drawLine(3,7,15,7,paint);canvas.drawLine(9,3,9,7,paint);canvas.drawLine(5,10,13,17,paint);canvas.drawLine(13,7,4,18,paint);
            canvas.drawLine(14,21,18,11,paint);canvas.drawLine(18,11,22,21,paint);canvas.drawLine(16,17,20,17,paint);
        } else if (kind == PALETTE) {
            canvas.drawOval(new RectF(3,3,21,21),paint);canvas.drawCircle(8,8,1,paint);canvas.drawCircle(15,7,1,paint);canvas.drawCircle(18,12,1,paint);canvas.drawCircle(7,15,1,paint);
        } else if (kind == LAYOUT) {
            canvas.drawRect(3,4,21,20,paint);canvas.drawLine(12,4,12,20,paint);
        } else if (kind == ROTATE) {
            canvas.drawArc(new RectF(4,4,20,20),40,285,false,paint);canvas.drawLine(19,3,19,9,paint);canvas.drawLine(19,9,13,9,paint);
        } else if (kind == TODAY) {
            canvas.drawRoundRect(new RectF(4, 5, 20, 21), 2, 2, paint);
            canvas.drawLine(8, 3, 8, 7, paint); canvas.drawLine(16, 3, 16, 7, paint);
            canvas.drawLine(4, 10, 20, 10, paint); canvas.drawCircle(12, 15.5f, 2, paint);
        } else if (kind == LIST) {
            for(int y=6;y<=18;y+=6) {canvas.drawCircle(5,y,1,paint);canvas.drawLine(10,y,20,y,paint);}
        } else if (kind == MILESTONE) {
            canvas.drawRect(6,3,18,21,paint);canvas.drawLine(6,14,12,10,paint);canvas.drawLine(12,10,18,14,paint);
        } else if (kind == DELETE) {
            canvas.drawLine(4, 6, 20, 6, paint); canvas.drawLine(9, 3, 15, 3, paint);
            canvas.drawLine(6, 6, 7, 21, paint); canvas.drawLine(7, 21, 17, 21, paint); canvas.drawLine(17, 21, 18, 6, paint);
            canvas.drawLine(10, 10, 10, 17, paint); canvas.drawLine(14, 10, 14, 17, paint);
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
        int color = pressed||inverse ? Color.WHITE : Color.BLACK;
        if (paint.getColor() == color) return false;
        paint.setColor(color); invalidateSelf(); return true;
    }
    @Override public int getIntrinsicHeight() { return size; }
    @Override public void setAlpha(int alpha) { paint.setAlpha(alpha); invalidateSelf(); }
    @Override public void setColorFilter(ColorFilter filter) { paint.setColorFilter(filter); invalidateSelf(); }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
