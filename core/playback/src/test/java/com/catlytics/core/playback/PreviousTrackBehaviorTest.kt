package com.catlytics.core.playback

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PreviousTrackBehaviorTest {
    @Test
    fun `goes to previous track before five seconds when one exists`() {
        assertTrue(
            shouldSkipToPreviousMediaItem(
                positionMillis = 4_999L,
                hasPreviousMediaItem = true,
            ),
        )
    }

    @Test
    fun `restarts current track at five seconds`() {
        assertFalse(
            shouldSkipToPreviousMediaItem(
                positionMillis = 5_000L,
                hasPreviousMediaItem = true,
            ),
        )
    }

    @Test
    fun `restarts current track when there is no previous track`() {
        assertFalse(
            shouldSkipToPreviousMediaItem(
                positionMillis = 1_000L,
                hasPreviousMediaItem = false,
            ),
        )
    }
}
