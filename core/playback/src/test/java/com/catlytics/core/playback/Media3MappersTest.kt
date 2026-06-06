package com.catlytics.core.playback

import androidx.media3.common.Player
import com.catlytics.core.model.Artist
import com.catlytics.core.model.PlaybackRepeatMode
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

    @Test
    fun `repeat modes map from media3 constants`() {
        assertEquals(PlaybackRepeatMode.Off, Player.REPEAT_MODE_OFF.toPlaybackRepeatMode())
        assertEquals(PlaybackRepeatMode.One, Player.REPEAT_MODE_ONE.toPlaybackRepeatMode())
        assertEquals(PlaybackRepeatMode.All, Player.REPEAT_MODE_ALL.toPlaybackRepeatMode())
    }

    @Test
    fun `repeat modes map to media3 constants`() {
        assertEquals(Player.REPEAT_MODE_OFF, PlaybackRepeatMode.Off.toMedia3RepeatMode())
        assertEquals(Player.REPEAT_MODE_ONE, PlaybackRepeatMode.One.toMedia3RepeatMode())
        assertEquals(Player.REPEAT_MODE_ALL, PlaybackRepeatMode.All.toMedia3RepeatMode())
    }
}
