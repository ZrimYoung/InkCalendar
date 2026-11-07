package com.inkcalendar.app;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

/** Reflows choices using the measured width, with bounded cards even for short rows. */
final class ChoiceGrid extends ViewGroup {
    private final int minimum,maximum,gap;
    private int columns=1,cellWidth;
    private int[] rowHeights=new int[0];

    ChoiceGrid(Context context){
        super(context);float density=getResources().getDisplayMetrics().density;
        minimum=Math.round(SharedThemeCatalog.CHOICE_MIN*density);maximum=Math.round(SharedThemeCatalog.CHOICE_MAX*density);gap=Math.round(SharedThemeCatalog.CHOICE_GAP*density);
        setPadding(0,gap/2,0,gap/2);
    }

    @Override protected LayoutParams generateDefaultLayoutParams(){return new LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT);}
    private void prepareRows(int count){
        if(rowHeights.length<count)rowHeights=new int[count];
        java.util.Arrays.fill(rowHeights,0,count,0);
    }

    @Override protected void onMeasure(int widthSpec,int heightSpec){
        int width=MeasureSpec.getMode(widthSpec)==MeasureSpec.UNSPECIFIED?maximum:MeasureSpec.getSize(widthSpec);
        int available=Math.max(0,width-getPaddingLeft()-getPaddingRight());
        int possible=Math.max(1,(available+gap)/(minimum+gap));
        int wanted=Math.max(1,(available+gap+maximum+gap-1)/(maximum+gap));
        columns=Math.min(possible,wanted);
        cellWidth=Math.min(maximum,Math.max(0,(available-gap*(columns-1))/columns));
        int visible=0;for(int i=0;i<getChildCount();i++)if(getChildAt(i).getVisibility()!=GONE)visible++;
        int rows=(visible+columns-1)/columns;prepareRows(rows);
        int index=0;
        for(int i=0;i<getChildCount();i++){
            View child=getChildAt(i);if(child.getVisibility()==GONE)continue;
            child.measure(MeasureSpec.makeMeasureSpec(cellWidth,MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(0,MeasureSpec.UNSPECIFIED));
            int row=index++/columns;rowHeights[row]=Math.max(rowHeights[row],child.getMeasuredHeight());
        }
        int height=getPaddingTop()+getPaddingBottom()+Math.max(0,rows-1)*gap;
        for(int row=0;row<rows;row++)height+=rowHeights[row];
        index=0;
        for(int i=0;i<getChildCount();i++){
            View child=getChildAt(i);if(child.getVisibility()==GONE)continue;
            int rowHeight=rowHeights[index++/columns];
            if(child.getMeasuredHeight()!=rowHeight)child.measure(MeasureSpec.makeMeasureSpec(cellWidth,MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(rowHeight,MeasureSpec.EXACTLY));
        }
        setMeasuredDimension(resolveSize(width,widthSpec),resolveSize(height,heightSpec));
    }

    @Override protected void onLayout(boolean changed,int left,int top,int right,int bottom){
        boolean rtl=getLayoutDirection()==LAYOUT_DIRECTION_RTL;int index=0,y=getPaddingTop();
        for(int i=0;i<getChildCount();i++){
            View child=getChildAt(i);if(child.getVisibility()==GONE)continue;
            int col=index%columns,row=index/columns;
            int x=rtl?getWidth()-getPaddingRight()-cellWidth-col*(cellWidth+gap):getPaddingLeft()+col*(cellWidth+gap);
            child.layout(x,y,x+cellWidth,y+rowHeights[row]);index++;
            if(index%columns==0)y+=rowHeights[row]+gap;
        }
    }
}
