package com.inkcalendar.app;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public final class SwipeDirectionTest {
    @Test public void leftMovesToNextMonth() { assertEquals(1, SwipeDirection.months(-100, 10, 48)); }
    @Test public void rightMovesToPreviousMonth() { assertEquals(-1, SwipeDirection.months(100, -10, 48)); }
    @Test public void shortDragDoesNotTurnPage() { assertEquals(0, SwipeDirection.months(47, 0, 48)); }
    @Test public void verticalScrollDoesNotTurnPage() { assertEquals(0, SwipeDirection.months(60, 120, 48)); }
    @Test public void DiagonalDragDoesNotTurnPage() { assertEquals(0, SwipeDirection.months(-90, 60, 48)); }
}
