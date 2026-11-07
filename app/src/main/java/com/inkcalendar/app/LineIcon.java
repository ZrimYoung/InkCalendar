package com.inkcalendar.app;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
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
            Path moon=new Path();moon.moveTo(20.9f,12.5f);
            moon.cubicTo(20.6f,17.4f,16.4f,21.1f,11.5f,20.9f);moon.cubicTo(6.6f,20.6f,2.8f,16.4f,3.1f,11.5f);moon.cubicTo(3.3f,6.9f,6.9f,3.3f,11.5f,3.1f);
            moon.cubicTo(9.6f,6.1f,10,9.2f,12.2f,11.4f);moon.cubicTo(14.4f,13.6f,18,14.2f,20.9f,12.5f);moon.close();canvas.drawPath(moon,paint);
        } else if (kind == TERMS) {
            Path sprout=new Path();sprout.moveTo(14,9.5f);sprout.lineTo(14,7);sprout.cubicTo(14,4.8f,15.8f,3,18,3);sprout.lineTo(19.5f,3);sprout.quadTo(20,3,20,3.5f);sprout.lineTo(20,5);sprout.cubicTo(20,7.2f,18.2f,9,16,9);sprout.cubicTo(13.8f,9,12,10.8f,12,13);sprout.cubicTo(12,15,13,16,13,18);sprout.cubicTo(13,19.1f,12.6f,20.2f,12,21);
            sprout.moveTo(4,9);sprout.cubicTo(7.3f,6.5f,12,8.9f,12,13);sprout.cubicTo(8.7f,15.5f,4,13.1f,4,9);sprout.close();canvas.drawPath(sprout,paint);canvas.drawLine(5,21,19,21,paint);
        } else if (kind == FLAG) {
            Path flag=new Path();flag.moveTo(4,22);flag.lineTo(4,4);flag.quadTo(4,3.5f,4.4f,3.2f);flag.cubicTo(5.4f,2.4f,6.7f,2,8,2);flag.cubicTo(11,2,13,4,15.3f,4);flag.quadTo(17.3f,4,18.4f,3.2f);flag.quadTo(20,2.2f,20,4);flag.lineTo(20,14);flag.quadTo(20,14.5f,19.6f,14.8f);flag.cubicTo(18.6f,15.6f,17.3f,16,16,16);flag.cubicTo(13,16,11,14,8,14);flag.cubicTo(6.5f,14,5.1f,14.5f,4,15.5f);canvas.drawPath(flag,paint);
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
