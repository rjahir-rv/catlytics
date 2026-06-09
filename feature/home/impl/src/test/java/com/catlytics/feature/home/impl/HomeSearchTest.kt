package com.catlytics.feature.home.impl

import com.catlytics.core.model.Artist
import com.catlytics.core.model.Track
import org.junit.Assert.assertEquals
import org.junit.Test

class HomeSearchTest {
    private val tracks = listOf(
        track(id = "one", title = "Midnight City", artist = "M83"),
        track(id = "two", title = "Heroes", artist = "David Bowie"),
        track(id = "three", title = "Space Oddity", artist = "David Bowie"),
    )

    @Test
    fun `empty query returns every track`() {
        assertEquals(tracks, tracks.filterByQuery("  "))
    }

    @Test
    fun `query filters tracks by title ignoring case`() {
        assertEquals(listOf(tracks[0]), tracks.filterByQuery("MIDNIGHT"))
    }

    @Test
    fun `query filters tracks by artist ignoring case`() {
        assertEquals(listOf(tracks[1], tracks[2]), tracks.filterByQuery("david bowie"))
    }

    @Test
    fun `query without matches returns empty list`() {
        assertEquals(emptyList<Track>(), tracks.filterByQuery("unknown"))
    }

    private fun track(
        id: String,
        title: String,
        artist: String,
    ) = Track(
        id = id,
        title = title,
        artist = Artist(
            id = "artist-$id",
            name = artist,
        ),
        durationMillis = 180_000L,
        mediaUri = "content://media/$id",
    )
}
