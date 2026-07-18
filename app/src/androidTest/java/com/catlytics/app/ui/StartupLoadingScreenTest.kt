package com.catlytics.app.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.catlytics.core.designsystem.theme.CatlyticsTheme
import org.junit.Rule
import org.junit.Test

class StartupLoadingScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun loadingScreenOnlyShowsProgress() {
        composeRule.setContent {
            CatlyticsTheme {
                StartupLoadingScreen()
            }
        }

        composeRule.onNodeWithTag(STARTUP_LOADING_PROGRESS_TAG).assertIsDisplayed()
        composeRule.onNodeWithText("Inicio").assertDoesNotExist()
        composeRule
            .onNodeWithText("No encontramos canciones en este dispositivo.")
            .assertDoesNotExist()
    }
}
