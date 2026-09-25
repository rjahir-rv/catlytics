package com.catlytics.app.playback

import org.junit.Assert.assertEquals
import org.junit.Test

class QueueTracksLabelTest {
    @Test
    fun usesSingularCopyForSingleTrack() {
        assertEquals("1 canción", queueTracksLabel(1))
    }

    @Test
    fun usesPluralCopyForOtherCounts() {
        assertEquals("0 canciones", queueTracksLabel(0))
        assertEquals("5 canciones", queueTracksLabel(5))
    }
}
