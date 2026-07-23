package com.catlytics.feature.home.impl

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.catlytics.core.model.Artist
import com.catlytics.core.model.Track
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class DailyPlaylistScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun successShowsDailyTracksAndPlaysWithTheCompleteQueue() {
        val tracks = (1..10).map(::track)
        var selectedTrack: Track? = null
        var selectedQueue: List<Track>? = null
        composeRule.setContent {
            MaterialTheme {
                DailyPlaylistScreen(
                    uiState = DailyPlaylistUiState.Success(tracks),
                    onTrackSelected = { track, queue ->
                        selectedTrack = track
                        selectedQueue = queue
                    },
                    onTrackOptions = {},
                )
            }
        }

        composeRule.onNodeWithText("10 canciones elegidas para ti").assertIsDisplayed()
        composeRule.onNodeWithText("Track 1").performClick()

        assertEquals(tracks.first(), selectedTrack)
        assertEquals(tracks, selectedQueue)
    }

    @Test
    fun emptyExplainsTheMinimumLibrarySize() {
        composeRule.setContent {
            MaterialTheme {
                DailyPlaylistScreen(
                    uiState = DailyPlaylistUiState.Empty,
                    onTrackSelected = { _, _ -> },
                    onTrackOptions = {},
                )
            }
        }

        composeRule
            .onNodeWithText("Necesitas al menos 5 canciones para crear tu Playlist diaria.")
            .assertIsDisplayed()
    }

    @Test
    fun optionsAreForwardedFromDailyTrackRows() {
        val track = track(1)
        var optionsTrack: Track? = null
        composeRule.setContent {
            MaterialTheme {
                DailyPlaylistScreen(
                    uiState = DailyPlaylistUiState.Success(listOf(track)),
                    onTrackSelected = { _, _ -> },
                    onTrackOptions = { optionsTrack = it },
                )
            }
        }

        composeRule.onNodeWithContentDescription("Opciones de ${track.title}").performClick()

        assertEquals(track, optionsTrack)
    }

    private fun track(index: Int) = Track(
        id = "track-$index",
        title = "Track $index",
        artist = Artist("artist-$index", "Artist $index"),
        durationMillis = 180_000L,
        mediaUri = "content://track/$index",
    )
}
