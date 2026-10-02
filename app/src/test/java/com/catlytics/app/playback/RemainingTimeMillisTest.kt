package com.catlytics.app.playback

import org.junit.Assert.assertEquals
import org.junit.Test

class RemainingTimeMillisTest {
    @Test
    fun returnsDurationMinusPosition() {
        assertEquals(135_000L, remainingTimeMillis(positionMillis = 45_000L, durationMillis = 180_000L))
    }

    @Test
    fun neverReturnsNegative() {
        assertEquals(0L, remainingTimeMillis(positionMillis = 200_000L, durationMillis = 180_000L))
    }
}
