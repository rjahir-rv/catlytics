package com.catlytics.core.playback

import com.catlytics.core.model.Artist
import com.catlytics.core.model.Track
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackQueueOrderTest {
    @Test
    fun `uses Media3 shuffle traversal order for the playback queue`() {
        val tracks = listOf(track("one"), track("two"), track("three"))

        val queue = tracks.inPlaybackOrder(shuffledIndices = listOf(2, 1, 0))

        assertEquals(listOf("three", "two", "one"), queue.map(Track::id))
    }

    @Test
    fun `uses insertion order when shuffle is disabled`() {
        val tracks = listOf(track("one"), track("two"), track("three"))

        val queue = tracks.inPlaybackOrder(shuffledIndices = null)

        assertEquals(listOf("one", "two", "three"), queue.map(Track::id))
    }
}

private fun track(id: String) = Track(
    id = id,
    title = id,
    artist = Artist(id = "artist", name = "Artist"),
    durationMillis = 1_000L,
    mediaUri = "content://media/$id",
    artworkUri = null,
    albumId = null,
    albumTitle = null,
)
