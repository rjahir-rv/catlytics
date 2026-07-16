package com.catlytics.core.designsystem.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.assertRangeInfoEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import org.junit.Rule
import org.junit.Test

class CatlyticsMiniPlayerTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun progressIndicatorExposesNormalizedPlaybackProgress() {
        composeRule.setContent {
            MaterialTheme {
                CatlyticsMiniPlayer(
                    title = "Canción de prueba",
                    artist = "Artista de prueba",
                    isPlaying = true,
                    isBuffering = false,
                    progress = 0.5f,
                    onTogglePlayback = {},
                    onSkipPrevious = {},
                    onSkipNext = {},
                    onClick = {},
                )
            }
        }

        composeRule
            .onNodeWithContentDescription("Progreso de reproducción")
            .assertRangeInfoEquals(ProgressBarRangeInfo(0.5f, 0f..1f))
    }
}
