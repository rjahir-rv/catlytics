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
                Text(queueTracksLabel(1))
                Text(queueTracksLabel(0))
                Text(queueTracksLabel(5))
            }
        }

        composeRule.onNodeWithText("1 canción").assertIsDisplayed()
        composeRule.onNodeWithText("0 canciones").assertIsDisplayed()
        composeRule.onNodeWithText("5 canciones").assertIsDisplayed()
    }
}
