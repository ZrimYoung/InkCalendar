package com.inkcalendar.app;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ReplacementSpan;

/** Word-sized spans supply heading tracking on Android 4.4, preserving text and word wrapping. */
final class TextTracking {
    static CharSequence apply(CharSequence value,float em){
        SpannableString result=new SpannableString(value==null?"":value);
        for(Word span:result.getSpans(0,result.length(),Word.class))result.removeSpan(span);
        if(em==0)return result;
        for(int i=0;i<result.length();){if(!latin(result.charAt(i))){i++;continue;}int start=i++;while(i<result.length()&&latin(result.charAt(i)))i++;result.setSpan(new Word(result.subSequence(start,i).toString(),em),start,i,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);}
        return result;
    }
    private static boolean latin(char c){return c>='A'&&c<='Z'||c>='a'&&c<='z';}
    static final class Word extends ReplacementSpan {
        private final String word;private final float em;
        Word(String word,float em){this.word=word;this.em=em;}
        @Override public int getSize(Paint paint,CharSequence text,int start,int end,Paint.FontMetricsInt metrics){
            if(metrics!=null)paint.getFontMetricsInt(metrics);float width=0;for(int i=0;i<word.length();i++)width+=paint.measureText(word,i,i+1);return (int)Math.ceil(width+Math.max(0,word.length()-1)*em*paint.getTextSize());
        }
        @Override public void draw(Canvas canvas,CharSequence text,int start,int end,float x,int top,int y,int bottom,Paint paint){for(int i=0;i<word.length();i++){canvas.drawText(word,i,i+1,x,y,paint);x+=paint.measureText(word,i,i+1)+em*paint.getTextSize();}}
    }
}
