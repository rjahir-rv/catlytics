package com.catlytics.core.playback

import com.catlytics.core.model.Artist
import com.catlytics.core.model.Track
import com.catlytics.core.model.reorderedForShuffle
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
    fun `add to queue inserts after current before remaining context`() {
        val queue = listOf(track("one"), track("two"), track("three"))

        val updatedQueue = queue.withManualQueueAfterCurrent(
            currentTrackId = "one",
            manualQueue = listOf(track("four")),
        )

        assertEquals(listOf("one", "four", "two", "three"), updatedQueue.map(Track::id))
    }

    @Test
    fun `add to queue appends after previously queued tracks`() {
        val queue = listOf(track("one"), track("four"), track("two"), track("three"))

        val updatedQueue = queue.withManualQueueAfterCurrent(
            currentTrackId = "one",
            manualQueue = listOf(track("four"), track("five")),
        )

        assertEquals(listOf("one", "four", "five", "two", "three"), updatedQueue.map(Track::id))
    }

    @Test
    fun `play next inserts in front of previously queued tracks`() {
        val queue = listOf(track("one"), track("four"), track("five"), track("two"))

        val updatedQueue = queue.withManualQueueAfterCurrent(
            currentTrackId = "one",
            manualQueue = listOf(track("six"), track("four"), track("five")),
        )

        assertEquals(listOf("one", "six", "four", "five", "two"), updatedQueue.map(Track::id))
    }

    @Test
    fun `pulls an existing later track into the manual queue`() {
        val queue = listOf(track("one"), track("two"), track("three"), track("four"))

        val updatedQueue = queue.withManualQueueAfterCurrent(
            currentTrackId = "one",
            manualQueue = listOf(queue.last()),
        )

        assertEquals(listOf("one", "four", "two", "three"), updatedQueue.map(Track::id))
    }

    @Test
    fun `reorderedForShuffle places start track first and includes all distinct tracks`() {
        val tracks = (1..9).map { track("track-$it") }
        val startTrack = tracks[7] // track-8

        val reordered = tracks.reorderedForShuffle(startTrack)

        assertEquals("track-8", reordered.first().id)
        assertEquals(9, reordered.size)
        assertEquals(tracks.map(Track::id).toSet(), reordered.map(Track::id).toSet())
    }

    @Test
    fun `reorderedForShuffle handles single track or empty lists`() {
        val single = listOf(track("one"))
        assertEquals(single, single.reorderedForShuffle(single.first()))
        assertEquals(emptyList<Track>(), emptyList<Track>().reorderedForShuffle(track("one")))
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
