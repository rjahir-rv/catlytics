package com.catlytics.app.ui.chrome

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.catlytics.app.navigation.TopLevelDestination
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class CatlyticsAppBottomBarTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun compactBarKeepsLabelsAndDestinationActions() {
        var selectedRoute: Any? = null
        composeRule.setContent {
            MaterialTheme {
                CatlyticsBottomBar(
                    selectedRoute = TopLevelDestination.Home.route,
                    onDestinationSelected = { selectedRoute = it },
                )
            }
        }

        TopLevelDestination.entries.forEach { destination ->
            composeRule.onNodeWithText(destination.label).assertIsDisplayed()
        }
        composeRule
            .onNodeWithContentDescription(TopLevelDestination.Library.label)
            .performClick()

        assertEquals(TopLevelDestination.Library.route, selectedRoute)
    }
}
