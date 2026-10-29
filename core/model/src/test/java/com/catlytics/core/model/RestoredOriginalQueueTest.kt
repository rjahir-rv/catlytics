package com.catlytics.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class RestoredOriginalQueueTest {
    private val original = listOf(track("a"), track("b"), track("c"))
    private val shuffled = listOf(track("b"), track("c"), track("a"))

    @Test
    fun `uses persisted original order when shuffle is enabled`() {
        assertEquals(
            original,
            restoredOriginalQueue(
                restoredQueue = shuffled,
                persistedOriginalQueue = original,
                isShuffleEnabled = true,
            ),
        )
    }

    @Test
    fun `keeps restored queue when shuffle is disabled`() {
        assertEquals(
            shuffled,
            restoredOriginalQueue(
                restoredQueue = shuffled,
                persistedOriginalQueue = original,
                isShuffleEnabled = false,
            ),
        )
    }

    @Test
    fun `keeps restored queue when no original order was persisted`() {
        assertEquals(
            shuffled,
            restoredOriginalQueue(
                restoredQueue = shuffled,
                persistedOriginalQueue = emptyList(),
                isShuffleEnabled = true,
            ),
        )
    }

    @Test
    fun `keeps restored queue when persisted original describes other tracks`() {
        val restored = listOf(track("b"), track("a"))

        assertEquals(
            restored,
            restoredOriginalQueue(
                restoredQueue = restored,
                persistedOriginalQueue = original,
                isShuffleEnabled = true,
            ),
        )
    }

    private fun track(id: String) = Track(
        id = id,
        title = id,
        artist = Artist(id = "artist", name = "Artista"),
        durationMillis = 1_000L,
        mediaUri = "content://$id",
    )
}
