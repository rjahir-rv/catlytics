package com.catlytics.app.ui.chrome

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.catlytics.app.navigation.TopLevelDestination
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class CatlyticsAppBottomBarTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val resources = InstrumentationRegistry.getInstrumentation().targetContext.resources

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
            composeRule.onNodeWithText(resources.getString(destination.labelRes)).assertIsDisplayed()
        }
        composeRule
            .onNodeWithContentDescription(resources.getString(TopLevelDestination.Library.labelRes))
            .performClick()

        assertEquals(TopLevelDestination.Library.route, selectedRoute)
    }
}
