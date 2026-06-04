package com.catlytics.core.playback

import com.catlytics.core.model.Artist
import com.catlytics.core.model.Track
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class Media3MappersTest {
    @Test
    fun `toMediaItem maps track identity uri and metadata`() {
        val track = Track(
            id = "track-42",
            title = "Local Song",
            artist = Artist(
                id = "artist-7",
                name = "Local Artist",
            ),
            durationMillis = 180_000L,
            mediaUri = "content://media/external/audio/media/42",
            artworkUri = "content://media/external/audio/albumart/9",
        )

        val mediaItem = track.toMediaItem()

        assertEquals("track-42", mediaItem.mediaId)
        assertEquals("content://media/external/audio/media/42", mediaItem.localConfiguration?.uri.toString())
        assertEquals("Local Song", mediaItem.mediaMetadata.title.toString())
        assertEquals("Local Artist", mediaItem.mediaMetadata.artist.toString())
        assertEquals(180_000L, mediaItem.mediaMetadata.durationMs)
        assertEquals("content://media/external/audio/albumart/9", mediaItem.mediaMetadata.artworkUri.toString())
    }
}
