package com.catlytics.app.playback

import org.junit.Assert.assertEquals
import org.junit.Test

class RelativeSeekPositionTest {
    @Test
    fun backwardSeekStopsAtBeginningOfTrack() {
        assertEquals(
            0L,
            calculateRelativeSeekPosition(
                positionMillis = 5_000L,
                durationMillis = 180_000L,
                offsetMillis = -10_000L,
            ),
        )
    }

    @Test
    fun forwardSeekStopsAtEndOfTrack() {
        assertEquals(
            180_000L,
            calculateRelativeSeekPosition(
                positionMillis = 175_000L,
                durationMillis = 180_000L,
                offsetMillis = 10_000L,
            ),
        )
    }

    @Test
    fun relativeSeekMovesByRequestedOffset() {
        assertEquals(
            40_000L,
            calculateRelativeSeekPosition(
                positionMillis = 30_000L,
                durationMillis = 180_000L,
                offsetMillis = 10_000L,
            ),
        )
    }
}
