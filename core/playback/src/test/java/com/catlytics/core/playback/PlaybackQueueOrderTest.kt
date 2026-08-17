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

    @Test
    fun `inserts a new track immediately after the current track`() {
        val queue = listOf(track("one"), track("two"), track("three"))

        val updatedQueue = queue.withTrackAfterCurrent(
            currentTrackId = "two",
            track = track("four"),
        )

        assertEquals(listOf("one", "two", "four", "three"), updatedQueue.map(Track::id))
    }

    @Test
    fun `moves an existing queued track immediately after the current track`() {
        val queue = listOf(track("one"), track("two"), track("three"), track("four"))

        val updatedQueue = queue.withTrackAfterCurrent(
            currentTrackId = "one",
            track = queue.last(),
        )

        assertEquals(listOf("one", "four", "two", "three"), updatedQueue.map(Track::id))
    }

    @Test
    fun `does not duplicate or move the current track`() {
        val queue = listOf(track("one"), track("two"), track("three"))

        val updatedQueue = queue.withTrackAfterCurrent(
            currentTrackId = "two",
            track = queue[1],
        )

        assertEquals(queue, updatedQueue)
    }

    @Test
    fun `maps a shuffled playback order back to timeline indices`() {
        val timeline = listOf(track("one"), track("two"), track("three"), track("four"))
        val playbackOrder = listOf(timeline[2], timeline[0], timeline[3], timeline[1])

        val indices = shuffleIndicesFor(timeline, playbackOrder)

        assertEquals(listOf(2, 0, 3, 1), indices)
    }

    @Test
    fun `inserts a new track after the current item in shuffled playback order`() {
        val playbackQueue = listOf(track("three"), track("one"), track("four"), track("two"))

        val updatedQueue = playbackQueue.withTrackAfterCurrent(
            currentTrackId = "three",
            track = track("five"),
        )

        assertEquals(listOf("three", "five", "one", "four", "two"), updatedQueue.map(Track::id))
    }

    @Test
    fun `returns no shuffle indices when playback order does not match the timeline`() {
        val timeline = listOf(track("one"), track("two"))
        val playbackOrder = listOf(track("two"), track("missing"))

        assertEquals(null, shuffleIndicesFor(timeline, playbackOrder))
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
