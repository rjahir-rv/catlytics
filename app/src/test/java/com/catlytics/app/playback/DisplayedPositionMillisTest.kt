package com.catlytics.app.playback

import org.junit.Assert.assertEquals
import org.junit.Test

class DisplayedPositionMillisTest {
    @Test
    fun returnsPlaybackPositionWhenNotSeeking() {
        assertEquals(
            45_000L,
            displayedPositionMillis(
                isSeeking = false,
                pendingProgress = 0.75f,
                positionMillis = 45_000L,
                durationMillis = 180_000L,
            ),
        )
    }

    @Test
    fun returnsScrubbedPositionWhenSeeking() {
        assertEquals(
            90_000L,
            displayedPositionMillis(
                isSeeking = true,
                pendingProgress = 0.5f,
                positionMillis = 10_000L,
                durationMillis = 180_000L,
            ),
        )
    }

    @Test
    fun clampsScrubbedPositionToDurationBounds() {
        assertEquals(
            180_000L,
            displayedPositionMillis(
                isSeeking = true,
                pendingProgress = 1.5f,
                positionMillis = 10_000L,
                durationMillis = 180_000L,
            ),
        )
        assertEquals(
            0L,
            displayedPositionMillis(
                isSeeking = true,
                pendingProgress = -0.25f,
                positionMillis = 10_000L,
                durationMillis = 180_000L,
            ),
        )
    }
}