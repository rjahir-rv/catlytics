package com.catlytics.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackSelectionSnapshotTest {
    @Test
    fun `enter starts selection with the pressed track`() {
        val selection = TrackSelectionSnapshot().enter("track-1")

        assertTrue(selection.active)
        assertEquals(setOf("track-1"), selection.selectedIds)
    }

    @Test
    fun `toggle adds and removes ids without leaving selection mode`() {
        val selected = TrackSelectionSnapshot().enter("track-1").toggle("track-2")
        val deselected = selected.toggle("track-1")

        assertTrue(deselected.active)
        assertEquals(setOf("track-2"), deselected.selectedIds)
    }

    @Test
    fun `select visible keeps ids that are no longer on screen`() {
        val selection = TrackSelectionSnapshot()
            .enter("hidden")
            .selectVisible(listOf("visible-1", "visible-2"))

        assertEquals(setOf("hidden", "visible-1", "visible-2"), selection.selectedIds)
    }

    @Test
    fun `selected tracks keep the source list order`() {
        val tracks = listOf(track("a"), track("b"), track("c"))
        val selected = TrackSelectionSnapshot(active = true, selectedIds = setOf("c", "a"))
            .selectedTracks(tracks)

        assertEquals(listOf("a", "c"), selected.map(Track::id))
    }

    @Test
    fun `all selected are liked only when every id is already liked`() {
        val selection = TrackSelectionSnapshot(active = true, selectedIds = setOf("a", "b"))

        assertTrue(selection.allSelectedAreLiked(setOf("a", "b", "c")))
        assertFalse(selection.allSelectedAreLiked(setOf("a")))
        assertFalse(TrackSelectionSnapshot().allSelectedAreLiked(setOf("a")))
    }

    private fun track(id: String) = Track(
        id = id,
        title = id,
        artist = Artist(id = "artist", name = "Artista"),
        durationMillis = 1_000L,
        mediaUri = "content://$id",
    )
}
