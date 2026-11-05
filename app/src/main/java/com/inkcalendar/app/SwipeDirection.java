package com.inkcalendar.app;

final class SwipeDirection {
    private SwipeDirection() { }
    static int months(float dx, float dy, float minimumDistance) {
        if (Math.abs(dx) < minimumDistance || Math.abs(dx) <= Math.abs(dy) * 1.5f) return 0;
        return dx < 0 ? 1 : -1;
    }
}
