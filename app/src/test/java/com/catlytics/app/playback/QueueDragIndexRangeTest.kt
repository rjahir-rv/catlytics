package com.catlytics.app.playback

import org.junit.Assert.assertEquals
import org.junit.Test

class QueueDragIndexRangeTest {
    @Test
    fun upcomingTrackCannotMoveAboveCurrentTrack() {
        assertEquals(3 until 8, queueDragIndexRange(draggedIndex = 5, currentIndex = 2, size = 8))
    }

    @Test
    fun historyTrackCannotMoveBelowCurrentTrack() {
        assertEquals(0 until 2, queueDragIndexRange(draggedIndex = 0, currentIndex = 2, size = 8))
    }

    @Test
    fun currentTrackStaysInPlace() {
        assertEquals(2..2, queueDragIndexRange(draggedIndex = 2, currentIndex = 2, size = 8))
    }

    @Test
    fun queueWithoutCurrentTrackMovesFreely() {
        assertEquals(0 until 8, queueDragIndexRange(draggedIndex = 5, currentIndex = -1, size = 8))
    }

    @Test
    fun emptyQueueHasNoPositions() {
        assertEquals(IntRange.EMPTY, queueDragIndexRange(draggedIndex = 0, currentIndex = -1, size = 0))
    }

    @Test
    fun scrollsToTopWhenHistoryIsCollapsed() {
        assertEquals(0, queueCurrentScrollIndex(currentIndex = 4, isHistoryCollapsed = true))
    }

    @Test
    fun scrollsPastHeaderAndHistoryWhenExpanded() {
        assertEquals(5, queueCurrentScrollIndex(currentIndex = 4, isHistoryCollapsed = false))
    }

    @Test
    fun scrollsToFirstItemWithoutHistory() {
        assertEquals(0, queueCurrentScrollIndex(currentIndex = 0, isHistoryCollapsed = false))
    }
}
