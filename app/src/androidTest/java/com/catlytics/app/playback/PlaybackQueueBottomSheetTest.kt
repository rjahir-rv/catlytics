package com.catlytics.app.playback

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import com.catlytics.core.designsystem.component.ArtworkGradientColors
import com.catlytics.core.model.Artist
import com.catlytics.core.model.SleepTimerState
import com.catlytics.core.model.Track
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PlaybackQueueBottomSheetTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun rowsShowOnlyReorderHandle() {
        setSheet()

        composeRule.onNodeWithContentDescription("Reordenar Song B").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Opciones de Song B").assertDoesNotExist()
    }

    @Test
    fun longPressOpensTrackOptions() {
        var optionsTrack: Track? = null
        setSheet(onTrackOptions = { optionsTrack = it })

        composeRule.onNodeWithText("Song B").performTouchInput { longClick() }

        assertEquals(queue[1], optionsTrack)
    }

    @Test
    fun controlsBarTogglesShuffleAndRepeat() {
        var shuffleClicks = 0
        var repeatClicks = 0
        setSheet(
            onToggleShuffle = { shuffleClicks++ },
            onCycleRepeatMode = { repeatClicks++ },
        )

        composeRule.onNodeWithText("Aleatorio").performClick()
        composeRule.onNodeWithText("Repetir").performClick()

        assertEquals(1, shuffleClicks)
        assertEquals(1, repeatClicks)
    }

    @Test
    fun timerPresetStartsSleepTimer() {
        var startedMinutes: Int? = null
        setSheet(onStartSleepTimer = { startedMinutes = it })

        composeRule.onNodeWithText("Temporizador").performClick()
        composeRule.onNodeWithText("30 min").performClick()

        assertEquals(30, startedMinutes)
    }

    @Test
    fun activeTimerShowsCountdownAndMarker() {
        setSheet(
            sleepTimerState = SleepTimerState.Active(
                totalDurationMillis = 900_000L,
                remainingMillis = 60_000L,
            ),
        )

        composeRule.onNodeWithText("01:00").assertIsDisplayed()
        composeRule.onNodeWithText("La música se pausará aquí").assertIsDisplayed()
    }

    private fun setSheet(
        sleepTimerState: SleepTimerState = SleepTimerState.Inactive,
        onTrackOptions: (Track) -> Unit = {},
        onToggleShuffle: () -> Unit = {},
        onCycleRepeatMode: () -> Unit = {},
        onStartSleepTimer: (Int) -> Unit = {},
    ) {
        composeRule.setContent {
            MaterialTheme {
                PlaybackQueueBottomSheet(
                    queue = queue,
                    currentTrackId = queue.first().id,
                    gradientColors = ArtworkGradientColors(Color.DarkGray, Color.Gray, Color.Black),
                    onDismiss = {},
                    onPlayQueueItem = {},
                    onMoveQueueItem = { _, _ -> },
                    onRemoveQueueItem = {},
                    onTrackOptions = onTrackOptions,
                    sleepTimerState = sleepTimerState,
                    onToggleShuffle = onToggleShuffle,
                    onCycleRepeatMode = onCycleRepeatMode,
                    onStartSleepTimer = onStartSleepTimer,
                )
            }
        }
    }

    private companion object {
        val queue = listOf("A", "B", "C").map { suffix ->
            Track(
                id = "track-$suffix",
                title = "Song $suffix",
                artist = Artist(id = "artist-id", name = "Artist"),
                durationMillis = 180_000L,
                mediaUri = "content://media/external/audio/media/$suffix",
            )
        }
    }
}
