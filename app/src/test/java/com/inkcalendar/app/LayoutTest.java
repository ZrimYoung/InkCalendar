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

/** Host-rendered layout checks. These images are not physical-device screenshots. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=28,qualifiers="w400dp-h800dp-port-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class LayoutTest {
    private List<View> all(View root){List<View> out=new ArrayList<>();out.add(root);if(root instanceof ViewGroup)for(int i=0;i<((ViewGroup)root).getChildCount();i++)out.addAll(all(((ViewGroup)root).getChildAt(i)));return out;}
    private View desc(View root,String text){for(View v:all(root))if(text.contentEquals(v.getContentDescription()==null?"":v.getContentDescription()))return v;throw new AssertionError(text);}
    private View label(View root,String text){for(View v:all(root))if(v instanceof TextView && text.contentEquals(((TextView)v).getText()))return v;throw new AssertionError(text);}
    private View root(MainActivity a)throws Exception{Field f=MainActivity.class.getDeclaredField("root");f.setAccessible(true);return (View)f.get(a);}
    private int top(View view,View root){int y=0;while(view!=root){y+=view.getTop();view=(View)view.getParent();}return y;}
    private void layout(View root,int width){root.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));root.layout(0,0,width,root.getMeasuredHeight());}
    private void snapshot(View root,String name)throws Exception{
        layout(root,400);Bitmap bitmap=Bitmap.createBitmap(400,root.getHeight(),Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(bitmap);canvas.drawColor(Color.WHITE);root.draw(canvas);
        for(View v:all(root))if(v.isShown() && v.getTag() instanceof String && v.getTag().toString().startsWith("almanac:") && v instanceof TextView && ((TextView)v).getText().length()>0 && !((View)v.getParent()).isSelected()) {
            int left=0;View node=v;while(node!=root){left+=node.getLeft();node=(View)node.getParent();}
            int y=top(v,root),ink=0;
            for(int py=y;py<y+v.getHeight()-1;py++)for(int px=left+1;px<left+v.getWidth()-1;px++)if(px>=0 && px<bitmap.getWidth() && py>=0 && py<bitmap.getHeight() && Color.red(bitmap.getPixel(px,py))<224)ink++;
            assertTrue("Clipped calendar annotation: "+((TextView)v).getText(),ink>0);
        }
        File file=new File("../delivery/host-"+name+"-0.4.0.png");file.getParentFile().mkdirs();try(FileOutputStream out=new FileOutputStream(file)){assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG,100,out));}bitmap.recycle();
    }
    @Test public void bilingualMainAndSettingsKeepControlPositions()throws Exception{
        Context context=RuntimeEnvironment.getApplication();context.deleteDatabase("calendar.db");context.getSharedPreferences("display",0).edit().clear().putString("language","en").commit();
        ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class).setup();MainActivity activity=controller.get();
        try {
            View en=root(activity);snapshot(en,"main-en");int navTop=top(desc(en,"Next month"),en),milestoneTop=top(desc(en,"Milestones"),en);
            desc(en,"Settings").performClick();Dialog settings=ShadowDialog.getLatestDialog();
            View settingsRoot=((ViewGroup)((ViewGroup)settings.findViewById(android.R.id.content)).getChildAt(0)).getChildAt(0);
            snapshot(settingsRoot,"settings-en");int saveTop=top(label(settingsRoot,"Save"),settingsRoot);
            label(settingsRoot,"简体中文").performClick();label(settingsRoot,"Save").performClick();
            View zh=root(activity);snapshot(zh,"main-zh");
            assertEquals(navTop,top(desc(zh,"下个月"),zh));assertTrue(Math.abs(milestoneTop-top(desc(zh,"纪念日与倒计时"),zh))<=1);
            desc(zh,"设置").performClick();settings=ShadowDialog.getLatestDialog();settingsRoot=((ViewGroup)((ViewGroup)settings.findViewById(android.R.id.content)).getChildAt(0)).getChildAt(0);
            snapshot(settingsRoot,"settings-zh");assertEquals(saveTop,top(label(settingsRoot,"保存"),settingsRoot));
            label(settingsRoot,"桌面展示模式").performClick();label(settingsRoot,"保存").performClick();snapshot(root(activity),"desk-zh");
            desc(root(activity),"纪念日与倒计时").performClick();Dialog list=ShadowDialog.getLatestDialog();desc(list.getWindow().getDecorView(),"添加纪念日或倒计时").performClick();Dialog editor=ShadowDialog.getLatestDialog();
            View form=((ViewGroup)((ViewGroup)editor.findViewById(android.R.id.content)).getChildAt(0)).getChildAt(0);snapshot(form,"milestone-zh");
        } finally {controller.pause().stop().destroy();}
    }
}
