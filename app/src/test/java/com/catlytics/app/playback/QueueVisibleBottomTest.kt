package com.catlytics.app.playback

import org.junit.Assert.assertEquals
import org.junit.Test

class QueueVisibleBottomTest {
    @Test
    fun limitsToVisibleAreaWhenSheetIsPartiallyExpanded() {
        assertEquals(
            860f,
            queueVisibleBottomPx(
                containerHeightPx = 2_000,
                sheetOffsetPx = 1_000f,
                headerHeightPx = 140,
                viewportStartPx = 0f,
                viewportEndPx = 1_800f,
            ),
        )
    }

    @Test
    fun usesFullViewportWhenSheetIsFullyExpanded() {
        assertEquals(
            1_700f,
            queueVisibleBottomPx(
                containerHeightPx = 2_000,
                sheetOffsetPx = 160f,
                headerHeightPx = 140,
                viewportStartPx = 0f,
                viewportEndPx = 1_700f,
            ),
        )
    }

    @Test
    fun neverReturnsLessThanViewportStart() {
        assertEquals(
            0f,
            queueVisibleBottomPx(
                containerHeightPx = 2_000,
                sheetOffsetPx = 1_900f,
                headerHeightPx = 140,
                viewportStartPx = 0f,
                viewportEndPx = 1_800f,
            ),
        )
    }
}
