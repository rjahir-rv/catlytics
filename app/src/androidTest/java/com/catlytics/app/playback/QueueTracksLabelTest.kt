package com.catlytics.app.playback

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

class QueueTracksLabelTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun rendersSingularAndPluralCopyWithCount() {
        composeRule.setContent {
            Column {
                Text(queueUpNextLabel(1))
                Text(queueUpNextLabel(0))
                Text(queueUpNextLabel(5))
            }
        }

        composeRule.onNodeWithText("A continuación · 1 canción").assertIsDisplayed()
        composeRule.onNodeWithText("A continuación · 0 canciones").assertIsDisplayed()
        composeRule.onNodeWithText("A continuación · 5 canciones").assertIsDisplayed()
    }
}
