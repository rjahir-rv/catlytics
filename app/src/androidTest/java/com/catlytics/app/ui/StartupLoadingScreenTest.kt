package com.catlytics.app.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
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

    @Test
    fun loadingGateDoesNotComposeAppContentUntilLoadingFinishes() {
        var isLoading by mutableStateOf(true)
        composeRule.setContent {
            CatlyticsTheme {
                StartupLoadingGate(
                    isLoading = isLoading,
                    composeContent = !isLoading,
                ) {
                    Text(
                        text = "Contenido de la app",
                        modifier = Modifier.testTag(APP_CONTENT_TAG),
                    )
                }
            }
        }

        composeRule.onNodeWithTag(STARTUP_LOADING_PROGRESS_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(APP_CONTENT_TAG).assertDoesNotExist()

        composeRule.runOnIdle { isLoading = false }

        composeRule.onNodeWithTag(STARTUP_LOADING_PROGRESS_TAG).assertDoesNotExist()
        composeRule.onNodeWithTag(APP_CONTENT_TAG).assertIsDisplayed()
    }

    @Test
    fun loadingGateKeepsProgressOverComposedContentUntilItIsReady() {
        composeRule.setContent {
            CatlyticsTheme {
                StartupLoadingGate(
                    isLoading = true,
                    composeContent = true,
                ) {
                    Text(
                        text = "Contenido cargando",
                        modifier = Modifier.testTag(APP_CONTENT_TAG),
                    )
                }
            }
        }

        composeRule.onNodeWithTag(STARTUP_LOADING_PROGRESS_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(APP_CONTENT_TAG, useUnmergedTree = true).assertExists()
    }

    private companion object {
        const val APP_CONTENT_TAG = "app_content"
    }
}
