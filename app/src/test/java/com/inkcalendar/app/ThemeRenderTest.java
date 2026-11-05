package com.inkcalendar.app;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.shadows.ShadowDialog;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=28,qualifiers="w400dp-h800dp-port-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class ThemeRenderTest {
    private List<View> all(View root){List<View> out=new ArrayList<>();out.add(root);if(root instanceof ViewGroup)for(int i=0;i<((ViewGroup)root).getChildCount();i++)out.addAll(all(((ViewGroup)root).getChildAt(i)));return out;}
    private View desc(View root,String text){for(View v:all(root))if(text.contentEquals(v.getContentDescription()==null?"":v.getContentDescription()))return v;throw new AssertionError(text);}
    private View root(MainActivity a)throws Exception{Field f=MainActivity.class.getDeclaredField("root");f.setAccessible(true);return (View)f.get(a);}
    private View form(Dialog d){return ((ViewGroup)((ViewGroup)d.findViewById(android.R.id.content)).getChildAt(0)).getChildAt(0);}
    private void snapshot(View root,String name,int width)throws Exception{
        root.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));root.layout(0,0,width,root.getMeasuredHeight());
        for(View v:all(root))if(v.isShown()&&v instanceof TextView&&!(v instanceof android.widget.EditText)){
            TextView t=(TextView)v;if(t.getLayout()!=null)for(int line=0;line<t.getLayout().getLineCount();line++)assertTrue(name+": clipped text "+t.getText()+" width="+t.getLayout().getLineMax(line)+" available="+(t.getWidth()-t.getCompoundPaddingLeft()-t.getCompoundPaddingRight()),t.getLayout().getLineMax(line)<=t.getWidth()-t.getCompoundPaddingLeft()-t.getCompoundPaddingRight()+2);
        }
        Bitmap bitmap=Bitmap.createBitmap(width,root.getHeight(),Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(bitmap);canvas.drawColor(Color.WHITE);root.draw(canvas);
        File file=new File("../delivery/host-theme-"+name+".png");file.getParentFile().mkdirs();try(FileOutputStream out=new FileOutputStream(file)){assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG,100,out));}bitmap.recycle();
    }
    @Test public void renderPresetsAndSettingsPreviews()throws Exception{
        Context c=RuntimeEnvironment.getApplication();c.deleteDatabase("calendar.db");
        for(int i=0;i<ThemeConfig.STYLES.length;i++){
            c.deleteDatabase("calendar.db");
            String style=ThemeConfig.STYLES[i];c.getSharedPreferences("display",0).edit().clear().putString("language",i%2==0?"zh":"en").putString("theme_style",style).putString("theme_font",ThemeConfig.FONTS[i]).commit();
            ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class).setup();MainActivity a=controller.get();
            try{
                Field storeField=MainActivity.class.getDeclaredField("store");storeField.setAccessible(true);EventStore store=(EventStore)storeField.get(a);store.save(new Event(0,Dates.today(),"Project review · 项目讨论","09:30"));
                controller.pause().resume();snapshot(root(a),style,400);
                if(i==0){desc(a.getWindow().getDecorView(),"设置").performClick();Dialog settings=ShadowDialog.getLatestDialog();snapshot(form(settings),"settings",400);
                    desc(settings.getWindow().getDecorView(),"外观与主题").performClick();Dialog appearance=ShadowDialog.getLatestDialog();snapshot(form(appearance),"appearance",400);
                }
                if(i==4){c.getSharedPreferences("display",0).edit().putBoolean("large",true).commit();controller.recreate();a=controller.get();snapshot(root(a),"ticket-large-320",320);}
            }finally{controller.pause().stop().destroy();}
        }
    }
    @Test public void narrowLargeAndWideLayoutsKeepTextInsideBounds()throws Exception{
        Context c=RuntimeEnvironment.getApplication();
        try{
            for(String style:new String[]{"today","ticket","minimal"})for(String language:new String[]{"en","zh"})for(boolean wide:new boolean[]{false,true}){
                int width=wide?720:320;RuntimeEnvironment.setQualifiers(wide?"w720dp-h400dp-land-mdpi":"w320dp-h640dp-port-mdpi");c.deleteDatabase("calendar.db");
                c.getSharedPreferences("display",0).edit().clear().putString("language",language).putString("theme_style",style).putString("theme_font","atkinson").putBoolean("large",true).commit();
                ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class).setup();MainActivity a=controller.get();
                try{
                    Field f=MainActivity.class.getDeclaredField("store");f.setAccessible(true);((EventStore)f.get(a)).save(new Event(0,Dates.today(),"Project review · 项目讨论","09:30"));controller.pause().resume();
                    snapshot(root(a),style+"-"+language+"-"+width,width);
                }finally{controller.pause().stop().destroy();}
            }
        }finally{RuntimeEnvironment.setQualifiers("w400dp-h800dp-port-mdpi");}
    }
}
