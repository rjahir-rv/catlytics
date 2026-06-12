package com.catlytics.app.playback

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import com.catlytics.core.model.Artist
import com.catlytics.core.model.PlaybackState
import com.catlytics.core.model.Track
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class NowPlayingScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun shareButtonSharesCurrentTrack() {
        var sharedTrack: Track? = null

        composeRule.setContent {
            MaterialTheme {
                NowPlayingScreen(
                    playbackState = PlaybackState(currentTrack = track),
                    onShareTrack = { sharedTrack = it },
                    onBack = {},
                    onTogglePlayback = {},
                    onSkipPrevious = {},
                    onSkipNext = {},
                    onSeekTo = {},
                    onToggleShuffle = {},
                    onCycleRepeatMode = {},
                    onPlayQueueItem = {},
                    onMoveQueueItem = { _, _ -> },
                    onAddToPlaylist = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription("Compartir canción").performClick()

        assertEquals(track, sharedTrack)
    }

    @Test
    fun shareButtonIsDisabledWithoutCurrentTrack() {
        composeRule.setContent {
            MaterialTheme {
                NowPlayingScreen(
                    playbackState = PlaybackState(),
                    onShareTrack = {},
                    onBack = {},
                    onTogglePlayback = {},
                    onSkipPrevious = {},
                    onSkipNext = {},
                    onSeekTo = {},
                    onToggleShuffle = {},
                    onCycleRepeatMode = {},
                    onPlayQueueItem = {},
                    onMoveQueueItem = { _, _ -> },
                    onAddToPlaylist = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription("Compartir canción").assertIsNotEnabled()
    }

    private companion object {
        val track = Track(
            id = "track-id",
            title = "Song",
            artist = Artist(id = "artist-id", name = "Artist"),
            durationMillis = 180_000L,
            mediaUri = "content://media/external/audio/media/42",
        )
    }
}
