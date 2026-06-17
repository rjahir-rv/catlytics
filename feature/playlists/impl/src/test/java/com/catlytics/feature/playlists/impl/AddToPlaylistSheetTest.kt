package com.catlytics.feature.playlists.impl

import com.catlytics.core.model.LIKED_PLAYLIST_ID
import com.catlytics.core.model.Playlist
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AddToPlaylistSheetTest {
    @Test
    fun `pending track count excludes tracks already in playlist`() {
        val playlist = Playlist(
            id = "playlist-1",
            name = "Focus",
            trackIds = listOf("track-1", "track-3"),
        )

        assertEquals(1, pendingTrackCount(playlist, listOf("track-1", "track-2")))
    }

    @Test
    fun `playlist is fully added when every source track is already present`() {
        val playlist = Playlist(
            id = "playlist-1",
            name = "Focus",
            trackIds = listOf("track-1", "track-2"),
        )

        assertTrue(isPlaylistFullyAdded(playlist, listOf("track-1", "track-2")))
        assertFalse(isPlaylistFullyAdded(playlist, listOf("track-1", "track-3")))
    }

    @Test
    fun `selection keeps every selected playlist`() {
        var selected = emptySet<String>()

        selected = togglePlaylistSelectionState(selected, "playlist-1")
        selected = togglePlaylistSelectionState(selected, "playlist-2")

        assertEquals(setOf("playlist-1", "playlist-2"), selected)
    }

    @Test
    fun `selection toggles off when tapped again`() {
        var selected = togglePlaylistSelectionState(
            selectedIds = setOf("playlist-1", "playlist-2"),
            playlistId = "playlist-1",
        )

        assertEquals(setOf("playlist-2"), selected)
    }

    @Test
    fun `liked playlist can be fully added`() {
        val playlist = Playlist(
            id = LIKED_PLAYLIST_ID,
            name = "Tus me gusta",
            trackIds = listOf("track-1"),
        )

        assertTrue(isPlaylistFullyAdded(playlist, listOf("track-1")))
    }
}