package com.catlytics.app.playback

import com.catlytics.core.model.Artist
import com.catlytics.core.model.PlaybackRepeatMode
import com.catlytics.core.model.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UpNextTrackTest {
    private val queue = listOf(track("a"), track("b"), track("c"))

    @Test
    fun returnsFollowingTrack() {
        assertEquals("b", upNextTrack(queue, currentIndex = 0, PlaybackRepeatMode.Off)?.id)
    }

    @Test
    fun returnsNullAtEndWithoutRepeatAll() {
        assertNull(upNextTrack(queue, currentIndex = 2, PlaybackRepeatMode.Off))
        assertNull(upNextTrack(queue, currentIndex = 2, PlaybackRepeatMode.One))
    }

    @Test
    fun wrapsToFirstTrackWithRepeatAll() {
        assertEquals("a", upNextTrack(queue, currentIndex = 2, PlaybackRepeatMode.All)?.id)
    }

    @Test
    fun returnsNullForSingleTrackOrInvalidIndex() {
        assertNull(upNextTrack(listOf(track("a")), currentIndex = 0, PlaybackRepeatMode.All))
        assertNull(upNextTrack(queue, currentIndex = -1, PlaybackRepeatMode.Off))
        assertNull(upNextTrack(queue, currentIndex = 5, PlaybackRepeatMode.All))
    }

    private fun track(id: String) = Track(
        id = id,
        title = "Canción $id",
        artist = Artist(id = "artist", name = "Artista"),
        durationMillis = 180_000L,
        mediaUri = "content://media/$id",
    )
}
