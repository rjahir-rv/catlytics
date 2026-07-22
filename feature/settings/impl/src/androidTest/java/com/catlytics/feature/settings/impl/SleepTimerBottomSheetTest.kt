package com.catlytics.feature.settings.impl

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import com.catlytics.core.model.SleepTimerState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SleepTimerBottomSheetTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun dialSelectionIsUsedWhenStartingTimer() {
        var selectedMinutes: Int? = null
        composeRule.setContent {
            MaterialTheme {
                SleepTimerBottomSheet(
                    state = SleepTimerState.Inactive,
                    onStart = { selectedMinutes = it },
                    onCancel = {},
                    onDismiss = {},
                )
            }
        }

        composeRule.onNodeWithTag(SLEEP_TIMER_DIAL_TEST_TAG)
            .performSemanticsAction(SemanticsActions.SetProgress) { setProgress ->
                setProgress(65f)
            }
        composeRule.onNodeWithText("Iniciar temporizador").performClick()

        composeRule.runOnIdle { assertEquals(65, selectedMinutes) }
    }

    @Test
    fun activeTimerShowsCountdownAndCanBeCancelled() {
        var cancelCalls = 0
        composeRule.setContent {
            MaterialTheme {
                SleepTimerBottomSheet(
                    state = SleepTimerState.Active(
                        totalDurationMillis = 30 * 60_000L,
                        remainingMillis = 10 * 60_000L,
                    ),
                    onStart = {},
                    onCancel = { cancelCalls += 1 },
                    onDismiss = {},
                )
            }
        }

        composeRule.onNodeWithText("Temporizador activo").assertIsDisplayed()
        composeRule.onNodeWithText("10:00").assertIsDisplayed()
        composeRule.onNodeWithText("Reiniciar temporizador").assertIsDisplayed()
        composeRule.onNodeWithText("Cancelar temporizador").performClick()

        composeRule.runOnIdle { assertEquals(1, cancelCalls) }
    }
}
