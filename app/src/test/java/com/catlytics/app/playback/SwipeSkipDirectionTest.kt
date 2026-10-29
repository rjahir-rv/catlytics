package com.catlytics.app.playback

import org.junit.Assert.assertEquals
import org.junit.Test

class SwipeSkipDirectionTest {
    private val width = 1_000f

    @Test
    fun shortSlowDragDoesNothing() {
        assertEquals(ArtworkSwipe.None, swipeSkipDirection(offsetPx = 100f, widthPx = width, velocityPx = 0f))
    }

    @Test
    fun dragRightPastThresholdGoesToPrevious() {
        assertEquals(ArtworkSwipe.Previous, swipeSkipDirection(offsetPx = 350f, widthPx = width, velocityPx = 0f))
    }

    @Test
    fun dragLeftPastThresholdGoesToNext() {
        assertEquals(ArtworkSwipe.Next, swipeSkipDirection(offsetPx = -350f, widthPx = width, velocityPx = 0f))
    }

    @Test
    fun fastFlingInSameDirectionSkips() {
        assertEquals(ArtworkSwipe.Next, swipeSkipDirection(offsetPx = -100f, widthPx = width, velocityPx = -2_000f))
    }

    @Test
    fun flingAgainstDragDirectionDoesNothing() {
        assertEquals(ArtworkSwipe.None, swipeSkipDirection(offsetPx = -100f, widthPx = width, velocityPx = 2_000f))
    }

    @Test
    fun zeroWidthDoesNothing() {
        assertEquals(ArtworkSwipe.None, swipeSkipDirection(offsetPx = 500f, widthPx = 0f, velocityPx = 0f))
    }
}
