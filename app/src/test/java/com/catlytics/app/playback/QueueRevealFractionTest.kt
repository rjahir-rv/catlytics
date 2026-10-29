package com.catlytics.app.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QueueRevealFractionTest {
    @Test
    fun rowsStartHiddenAndEndFullyVisible() {
        assertEquals(0f, queueRevealFraction(progress = 0f, distance = 0), 0f)
        assertEquals(1f, queueRevealFraction(progress = 1f, distance = 0), 0f)
        assertEquals(1f, queueRevealFraction(progress = 1f, distance = 50), 0f)
    }

    @Test
    fun fartherRowsStartLater() {
        val near = queueRevealFraction(progress = 0.3f, distance = 0)
        val far = queueRevealFraction(progress = 0.3f, distance = 3)

        assertTrue(near > far)
    }

    @Test
    fun staggerIsCappedForDistantRows() {
        assertEquals(
            queueRevealFraction(progress = 0.8f, distance = 7),
            queueRevealFraction(progress = 0.8f, distance = 40),
            0f,
        )
    }
}
