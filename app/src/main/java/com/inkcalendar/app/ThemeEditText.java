package com.inkcalendar.app;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.Spanned;
import android.text.TextWatcher;
import android.widget.EditText;

/** Keep Latin and Chinese font pairing while typing without changing characters or cursor position. */
@SuppressLint("ViewConstructor")
final class ThemeEditText extends EditText {
    private final Typeface chinese;
    ThemeEditText(Context context,Typeface chinese){super(context);this.chinese=chinese;addTextChangedListener(new TextWatcher(){
        public void beforeTextChanged(CharSequence s,int start,int count,int after){}
        public void onTextChanged(CharSequence s,int start,int before,int count){}
        public void afterTextChanged(Editable value){
            for(ThemeFonts.ChineseSpan span:value.getSpans(0,value.length(),ThemeFonts.ChineseSpan.class))value.removeSpan(span);
            Spanned mixed=(Spanned)ThemeFonts.mixed(value,ThemeEditText.this.chinese,false);
            for(ThemeFonts.ChineseSpan span:mixed.getSpans(0,mixed.length(),ThemeFonts.ChineseSpan.class))value.setSpan(span,mixed.getSpanStart(span),mixed.getSpanEnd(span),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    });}
}
