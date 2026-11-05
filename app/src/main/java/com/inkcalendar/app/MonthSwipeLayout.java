package com.inkcalendar.app;

import android.content.Context;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.widget.LinearLayout;

/** Horizontal month gestures cancel date clicks; vertical gestures stay with the ScrollView. */
final class MonthSwipeLayout extends LinearLayout {
    interface Listener { void onMonthSwipe(int months); }
    private final Listener listener;
    private final int slop;
    private final float minimumDistance;
    private float startX, startY;
    private boolean horizontal, vertical, multiple;

    MonthSwipeLayout(Context context, Listener listener) {
        super(context);
        this.listener = listener;
        slop = ViewConfiguration.get(context).getScaledTouchSlop();
        minimumDistance = 48 * getResources().getDisplayMetrics().density;
        setOrientation(VERTICAL);
    }

    @Override public boolean onInterceptTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                startX = event.getX(); startY = event.getY();
                horizontal = false; vertical = false; multiple = false;
                getParent().requestDisallowInterceptTouchEvent(true);
                return false;
            case MotionEvent.ACTION_POINTER_DOWN:
                multiple = true; return false;
            case MotionEvent.ACTION_MOVE:
                if (multiple || vertical) return false;
                float dx = Math.abs(event.getX() - startX), dy = Math.abs(event.getY() - startY);
                if (dy > slop && dy >= dx) {
                    vertical = true;
                    getParent().requestDisallowInterceptTouchEvent(false);
                    return false;
                }
                if (dx > slop && dx > dy * 1.5f) horizontal = true;
                return horizontal;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                getParent().requestDisallowInterceptTouchEvent(false);
                return false;
            default: return false;
        }
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_POINTER_DOWN) multiple = true;
        if (event.getActionMasked() == MotionEvent.ACTION_UP) {
            int direction = multiple ? 0 : SwipeDirection.months(event.getX() - startX, event.getY() - startY, minimumDistance);
            getParent().requestDisallowInterceptTouchEvent(false);
            if (horizontal && direction != 0) listener.onMonthSwipe(direction);
            else performClick();
            horizontal = false;
        } else if (event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
            horizontal = false;
            getParent().requestDisallowInterceptTouchEvent(false);
        }
        return true;
    }

    @Override public boolean performClick() { super.performClick(); return true; }
}
