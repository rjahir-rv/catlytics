package com.catlytics.app.playback

import com.catlytics.core.model.Artist
import com.catlytics.core.model.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AdjacentArtworkUrisTest {
    @Test
    fun emptyQueueReturnsNoUris() {
        assertTrue(adjacentArtworkUris(queue = emptyList(), currentIndex = 0).isEmpty())
    }

    @Test
    fun firstTrackPrefetchesFollowingNeighbors() {
        val queue = listOf(track("one"), track("two"), track("three"), track("four"))

        assertEquals(
            listOf("content://art/two", "content://art/three"),
            adjacentArtworkUris(queue, currentIndex = 0),
        )
    }

    @Test
    fun middleTrackPrefetchesPreviousAndNextNeighbors() {
        val queue = listOf(track("one"), track("two"), track("three"), track("four"))

        assertEquals(
            listOf("content://art/two", "content://art/four", "content://art/one"),
            adjacentArtworkUris(queue, currentIndex = 2),
        )
    }

    @Test
    fun lastTrackPrefetchesPreviousNeighbors() {
        val queue = listOf(track("one"), track("two"), track("three"))

        assertEquals(
            listOf("content://art/two", "content://art/one"),
            adjacentArtworkUris(queue, currentIndex = 2),
        )
    }

    @Test
    fun skipsMissingArtworkAndDeduplicatesSharedCovers() {
        val sharedCover = "content://art/shared"
        val queue = listOf(
            track("one", artworkUri = sharedCover),
            track("two", artworkUri = null),
            track("three", artworkUri = sharedCover),
            track("four", artworkUri = "content://art/four"),
        )

        assertEquals(
            listOf(sharedCover, "content://art/four"),
            adjacentArtworkUris(queue, currentIndex = 1),
        )
    }

    @Test
    fun zeroRadiusReturnsNoUris() {
        val queue = listOf(track("one"), track("two"))

        assertTrue(adjacentArtworkUris(queue, currentIndex = 0, radius = 0).isEmpty())
    }

    private fun track(
        id: String,
        artworkUri: String? = "content://art/$id",
    ) = Track(
        id = id,
        title = id,
        artist = Artist(id = "artist", name = "Artist"),
        durationMillis = 1_000L,
        mediaUri = "content://media/$id",
        artworkUri = artworkUri,
    )
}
