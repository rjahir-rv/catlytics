package com.catlytics.app.playback

import org.junit.Assert.assertEquals
import org.junit.Test

class QueueAutoScrollTest {
    @Test
    fun doesNotScrollWhenItemIsAwayFromEdges() {
        assertEquals(0f, autoScrollDelta(itemTop = 400f), 0f)
    }

    @Test
    fun scrollsForwardProportionallyInsideBottomEdge() {
        // Bottom edge zone starts at 1000 - 100 = 900; item bottom at 950 is halfway in.
        assertEquals(10f, autoScrollDelta(itemTop = 850f), 0.001f)
    }

    @Test
    fun scrollsForwardAtFullSpeedWhenPinnedToBottom() {
        assertEquals(20f, autoScrollDelta(itemTop = 900f), 0.001f)
    }

    @Test
    fun scrollsBackwardInsideTopEdge() {
        assertEquals(-10f, autoScrollDelta(itemTop = 50f), 0.001f)
    }

    @Test
    fun capsSpeedWhenItemIsBeyondTopEdge() {
        assertEquals(-20f, autoScrollDelta(itemTop = -60f), 0.001f)
    }

    private fun autoScrollDelta(itemTop: Float) = queueAutoScrollDelta(
        itemTopPx = itemTop,
        itemBottomPx = itemTop + 100f,
        viewportStartPx = 0f,
        viewportEndPx = 1_000f,
        edgePx = 100f,
        maxStepPx = 20f,
    )
}
