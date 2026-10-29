package com.catlytics.feature.playlists.impl

import com.catlytics.core.model.LIKED_PLAYLIST_ID
import com.catlytics.core.model.Playlist
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CreatePlaylistNamingTest {
    private val liked = Playlist(LIKED_PLAYLIST_ID, "Tus me gusta", emptyList())
    private fun format(number: Int) = "Mi playlist n.º $number"

    @Test
    fun `name taken ignores case and surrounding spaces`() {
        val playlists = listOf(liked, Playlist("1", "Focus", emptyList()))

        assertTrue(isPlaylistNameTaken("  focus ", playlists))
        assertFalse(isPlaylistNameTaken("Focus 2", playlists))
        assertFalse(isPlaylistNameTaken("   ", playlists))
    }

    @Test
    fun `default number skips names already in use`() {
        val playlists = listOf(
            liked,
            Playlist("1", "Mi playlist n.º 1", emptyList()),
            Playlist("2", "Mi playlist n.º 2", emptyList()),
        )

        assertEquals(3, nextDefaultPlaylistNumber(playlists, ::format))
    }

    @Test
    fun `default number starts at one with only liked playlist`() {
        assertEquals(1, nextDefaultPlaylistNumber(listOf(liked), ::format))
    }
}
