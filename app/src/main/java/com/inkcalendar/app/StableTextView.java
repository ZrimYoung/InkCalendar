package com.inkcalendar.app;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.Layout;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.style.LineHeightSpan;
import android.widget.TextView;

/** Programmatic labels reserve the larger of the two translations at the actual width. */
@SuppressLint("ViewConstructor")
final class StableTextView extends TextView {
    private String alternate = "";
    private Typeface alternateFont;
    private Typeface chinese;
    private boolean chineseBold;
    private float tracking;

    StableTextView(Context context) { super(context); setIncludeFontPadding(false); }

    void useChinese(Typeface face, boolean bold) { chinese=face;chineseBold=bold;setText(getText()); }
    void tracking(float em){tracking=em;setText(getText());requestLayout();}

    void reserveFor(String text, Typeface font) {
        alternate = text; alternateFont = font; requestLayout();
    }

    @Override public void setText(CharSequence text, BufferType type) {
        super.setText(uniformLines(TextTracking.apply(ThemeFonts.mixed(text,chinese,chineseBold),tracking), this), type);
    }

    static CharSequence uniformLines(CharSequence text, TextView owner) {
        SpannableString value = new SpannableString(text == null ? "" : text);
        if (value.length() > 0) value.setSpan(new LineHeightSpan() {
            @Override public void chooseHeight(CharSequence s, int start, int end, int spanStart, int v, Paint.FontMetricsInt fm) {
                // Both bundled font families fit these shared ascent/descent values.
                fm.ascent = fm.top = -(int) Math.ceil(owner.getTextSize() * 1.2f);
                fm.descent = fm.bottom = (int) Math.ceil(owner.getTextSize() * 0.35f);
                fm.leading = 0;
            }
        }, 0, value.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return value;
    }

    @SuppressWarnings("deprecation")
    @Override protected void onMeasure(int widthSpec, int heightSpec) {
        super.onMeasure(widthSpec, heightSpec);
        if (alternate == null || alternate.isEmpty() || MeasureSpec.getMode(heightSpec) == MeasureSpec.EXACTLY) return;
        int width = getMeasuredWidth() - getCompoundPaddingLeft() - getCompoundPaddingRight();
        if (width <= 0) return;
        TextPaint paint = new TextPaint(getPaint());
        if (alternateFont != null) paint.setTypeface(alternateFont);
        StaticLayout layout = new StaticLayout(uniformLines(TextTracking.apply(ThemeFonts.mixed(alternate,chinese,chineseBold),tracking), this), paint, width,
            Layout.Alignment.ALIGN_NORMAL, getLineSpacingMultiplier(), getLineSpacingExtra(), false);
        int height = Math.max(getMeasuredHeight(), layout.getHeight() + getCompoundPaddingTop() + getCompoundPaddingBottom());
        setMeasuredDimension(getMeasuredWidth(), resolveSize(height, heightSpec));
    }
}
