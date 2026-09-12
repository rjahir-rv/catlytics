package com.catlytics.core.designsystem.component

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LetterIndexTest {
    @Test
    fun `section letter uses the first alphabetic character`() {
        assertEquals('M', "Midnight City".sectionLetter())
    }

    @Test
    fun `section letter strips diacritics and uppercases`() {
        assertEquals('E', "échame la culpa".sectionLetter())
        assertEquals('N', "Ñandú".sectionLetter())
    }

    @Test
    fun `section letter maps digits symbols and blanks to hash`() {
        assertEquals('#', "123 Go".sectionLetter())
        assertEquals('#', "".sectionLetter())
        assertEquals('#', "   ".sectionLetter())
        assertEquals('#', "...dots".sectionLetter())
    }

    @Test
    fun `section letter ignores leading whitespace`() {
        assertEquals('S', "  space".sectionLetter())
    }

    @Test
    fun `visible track index skips header items and clamps`() {
        assertEquals(0, visibleTrackIndex(firstVisibleItemIndex = 1, headerItemCount = 3, itemCount = 10))
        assertEquals(2, visibleTrackIndex(firstVisibleItemIndex = 5, headerItemCount = 3, itemCount = 10))
        assertEquals(9, visibleTrackIndex(firstVisibleItemIndex = 40, headerItemCount = 3, itemCount = 10))
        assertEquals(0, visibleTrackIndex(firstVisibleItemIndex = 0, headerItemCount = 0, itemCount = 0))
    }

    @Test
    fun `scroll fraction maps the first and last tracks`() {
        assertEquals(0, trackIndexForScrollFraction(0f, itemCount = 100))
        assertEquals(99, trackIndexForScrollFraction(1f, itemCount = 100))
        assertEquals(0f, scrollFractionForTrackIndex(0, itemCount = 100), 0.0001f)
        assertEquals(1f, scrollFractionForTrackIndex(99, itemCount = 100), 0.0001f)
        assertEquals(0, trackIndexForScrollFraction(0.4f, itemCount = 1))
    }

    @Test
    fun `fast scroller is hidden below the track threshold`() {
        assertFalse(shouldShowLetterFastScroller(itemCount = 10))
        assertFalse(shouldShowLetterFastScroller(itemCount = 29))
        assertTrue(shouldShowLetterFastScroller(itemCount = 30))
        assertTrue(shouldShowLetterFastScroller(itemCount = 200))
    }

    @Test
    fun `fast scroller stays hidden while featured headers are at the top`() {
        assertFalse(
            isLetterFastScrollerRevealed(
                firstVisibleItemIndex = 0,
                firstVisibleItemScrollOffset = 0,
                headerItemCount = 3,
            ),
        )
        assertFalse(
            isLetterFastScrollerRevealed(
                firstVisibleItemIndex = 0,
                firstVisibleItemScrollOffset = 80,
                headerItemCount = 3,
            ),
        )
        assertFalse(
            isLetterFastScrollerRevealed(
                firstVisibleItemIndex = 1,
                firstVisibleItemScrollOffset = 0,
                headerItemCount = 3,
            ),
        )
    }

    @Test
    fun `fast scroller is revealed once the track list starts clipping`() {
        assertTrue(
            isLetterFastScrollerRevealed(
                firstVisibleItemIndex = 2,
                firstVisibleItemScrollOffset = 12,
                headerItemCount = 3,
            ),
        )
        assertTrue(
            isLetterFastScrollerRevealed(
                firstVisibleItemIndex = 3,
                firstVisibleItemScrollOffset = 0,
                headerItemCount = 3,
            ),
        )
    }

    @Test
    fun `fast scroller without headers is revealed only after the list clips`() {
        assertFalse(
            isLetterFastScrollerRevealed(
                firstVisibleItemIndex = 0,
                firstVisibleItemScrollOffset = 0,
                headerItemCount = 0,
            ),
        )
        assertTrue(
            isLetterFastScrollerRevealed(
                firstVisibleItemIndex = 0,
                firstVisibleItemScrollOffset = 8,
                headerItemCount = 0,
            ),
        )
        assertTrue(
            isLetterFastScrollerRevealed(
                firstVisibleItemIndex = 1,
                firstVisibleItemScrollOffset = 0,
                headerItemCount = 0,
            ),
        )
    }
}
