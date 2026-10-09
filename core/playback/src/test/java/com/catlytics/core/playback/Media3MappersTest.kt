package com.catlytics.core.playback

import androidx.media3.common.Player
import com.catlytics.core.model.Artist
import com.catlytics.core.model.PlaybackRepeatMode
import com.catlytics.core.model.PlaybackStatus
import com.catlytics.core.model.Track
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class Media3MappersTest {
    @Test
    fun `buffering while paused is reported as paused`() {
        assertEquals(
            PlaybackStatus.Paused,
            playbackStatusOf(Player.STATE_BUFFERING, playWhenReady = false),
        )
    }

    @Test
    fun `buffering while about to play is reported as buffering`() {
        assertEquals(
            PlaybackStatus.Buffering,
            playbackStatusOf(Player.STATE_BUFFERING, playWhenReady = true),
        )
    }

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
    fun `edited artwork paths become file uris and album title is exposed`() {
        val track = Track(
            id = "track-1",
            title = "Canción",
            artist = Artist(id = "artist-1", name = "Artista"),
            durationMillis = 1_000L,
            mediaUri = "content://media/external/audio/media/1",
            artworkUri = "/data/user/0/com.catlytics/files/track_artwork/track-1-a.cover",
            albumTitle = "Demos",
        )

        val metadata = track.toMediaItem().mediaMetadata

        assertEquals(
            "file:///data/user/0/com.catlytics/files/track_artwork/track-1-a.cover",
            metadata.artworkUri.toString(),
        )
        assertEquals("Demos", metadata.albumTitle.toString())
    }

    @Test
    fun `queued tracks pick up edited metadata and keep unknown ones`() {
        val queued = listOf(
            Track("a", "Old", Artist("x", "X"), 1L, mediaUri = "content://a"),
            Track("gone", "Gone", Artist("x", "X"), 1L, mediaUri = "content://gone"),
        )
        val edited = queued.first().copy(title = "New", artist = Artist("y", "Y"))

        val refreshed = queued.withLatestMetadata(mapOf("a" to edited))

        assertEquals(listOf(edited, queued[1]), refreshed)
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
