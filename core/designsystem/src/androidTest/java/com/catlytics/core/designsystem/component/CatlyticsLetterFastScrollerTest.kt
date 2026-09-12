package com.catlytics.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test

class CatlyticsLetterFastScrollerTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun scrollerIsHiddenBelowTheTrackThreshold() {
        composeRule.setContent {
            MaterialTheme {
                FastScrollerHost(titles = titlesForCount(10))
            }
        }

        composeRule
            .onAllNodesWithContentDescription("Índice alfabético", substring = true)
            .assertCountEquals(0)
    }

    @Test
    fun scrollerStaysHiddenAtTheTopEvenWhenTheLibraryIsLong() {
        composeRule.setContent {
            MaterialTheme {
                FastScrollerHost(titles = titlesForCount(30))
            }
        }

        composeRule
            .onAllNodesWithContentDescription("Índice alfabético", substring = true)
            .assertCountEquals(0)
    }

    @Test
    fun scrollerAppearsAfterTheListStartsClipping() {
        composeRule.setContent {
            MaterialTheme {
                FastScrollerHost(titles = titlesForCount(30))
            }
        }

        composeRule.onNodeWithTag(LIST_TAG).performScrollToIndex(4)
        composeRule.waitForIdle()

        composeRule
            .onNodeWithContentDescription("Índice alfabético", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun scrollerStaysHiddenWhileFeaturedHeadersAreVisible() {
        composeRule.setContent {
            MaterialTheme {
                FastScrollerHost(
                    titles = titlesForCount(30),
                    headerItemCount = 3,
                )
            }
        }

        composeRule
            .onAllNodesWithContentDescription("Índice alfabético", substring = true)
            .assertCountEquals(0)
    }

    @Test
    fun scrollerLetterFollowsTheFirstVisibleTrack() {
        composeRule.setContent {
            MaterialTheme {
                FastScrollerHost(titles = letteredTitles())
            }
        }

        composeRule.onNodeWithTag(LIST_TAG).performScrollToIndex(36)
        composeRule.waitForIdle()

        composeRule
            .onNodeWithContentDescription("Índice alfabético, letra S")
            .assertIsDisplayed()
    }

    @Test
    fun draggingTheRailMovesTheList() {
        composeRule.setContent {
            MaterialTheme {
                FastScrollerHost(titles = letteredTitles())
            }
        }

        composeRule.onNodeWithTag(LIST_TAG).performScrollToIndex(2)
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(LETTER_FAST_SCROLLER_TAG).performTouchInput {
            down(bottomCenter)
            up()
        }
        composeRule.waitForIdle()

        composeRule
            .onAllNodesWithContentDescription("Índice alfabético, letra A")
            .assertCountEquals(0)
        composeRule
            .onNodeWithContentDescription("Índice alfabético", substring = true)
            .assertIsDisplayed()
    }

    @Composable
    private fun FastScrollerHost(
        titles: List<String>,
        headerItemCount: Int = 0,
    ) {
        val listState = rememberLazyListState()
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag(LIST_TAG),
            ) {
                items(headerItemCount) { index ->
                    Text(
                        text = "Header $index",
                        modifier = Modifier.height(120.dp),
                    )
                }
                itemsIndexed(titles) { _, title ->
                    Text(
                        text = title,
                        modifier = Modifier.height(56.dp),
                    )
                }
            }
            CatlyticsLetterFastScroller(
                listState = listState,
                itemCount = titles.size,
                headerItemCount = headerItemCount,
                letterForVisibleTrackIndex = { index -> titles[index].sectionLetter() },
            )
        }
    }

    private fun titlesForCount(count: Int): List<String> =
        List(count) { index -> "A track $index" }

    private fun letteredTitles(): List<String> =
        ('A'..'Z').flatMap { letter ->
            List(2) { index -> "$letter song $index" }
        }

    private companion object {
        const val LIST_TAG = "letter-fast-scroller-list"
    }
}
