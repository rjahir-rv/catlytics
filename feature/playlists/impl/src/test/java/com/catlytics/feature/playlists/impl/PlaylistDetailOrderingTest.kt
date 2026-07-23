package com.catlytics.feature.playlists.impl

import com.catlytics.core.model.Artist
import com.catlytics.core.model.Track
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class PlaylistDetailOrderingTest {
    @Test
    fun `alphabetical order ignores Spanish accents and case`() {
        val tracks = listOf(
            track("3", "Zeta", "B"),
            track("2", "árbol", "C"),
            track("1", "Arbol", "A"),
        )

        assertEquals(listOf("1", "2", "3"), tracks.alphabeticalOrder().map(Track::id))
    }

    @Test
    fun `move track changes only one position and respects boundaries`() {
        val tracks = listOf(track("1", "A"), track("2", "B"), track("3", "C"))

        assertEquals(listOf("2", "1", "3"), tracks.moveTrack("1", 1).map(Track::id))
        assertEquals(tracks, tracks.moveTrack("1", -1))
    }

    @Test
    fun `shuffle changes a multi-track order even when random returns same permutation`() {
        val tracks = listOf(track("1", "A"), track("2", "B"), track("3", "C"))

        assertNotEquals(tracks, tracks.shuffledOrder(Random(0)))
    }

    @Test
    fun `track picker cannot select tracks already in the playlist`() {
        val selected = toggleTrackSelection(
            selectedIds = setOf("track-1"),
            trackId = "track-2",
            existingTrackIds = setOf("track-2"),
        )

        assertEquals(setOf("track-1"), selected)
    }

    @Test
    fun `selected tracks preserve library order`() {
        val tracks = listOf(track("1", "A"), track("2", "B"), track("3", "C"))

        assertEquals(
            listOf("1", "3"),
            tracks.selectedTrackIdsInLibraryOrder(setOf("3", "1")),
        )
    }

    private fun track(id: String, title: String, artistName: String = "Artista") = Track(
        id = id,
        title = title,
        artist = Artist(id = "artist-$artistName", name = artistName),
        durationMillis = 180_000L,
        mediaUri = "content://$id",
    )
}
