@file:OptIn(ExperimentalMaterial3Api::class)

package com.catlytics.core.designsystem.component

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CatlyticsTopAppBarTest {
    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var scrollBehavior: TopAppBarScrollBehavior

    @Test
    fun enterAlwaysScrollBehaviorHidesAndRevealsTopBar() {
        setScrollableContent(canScroll = { true })

        val viewportBounds = composeRule
            .onNodeWithTag(SCROLLABLE_CONTENT_TAG)
            .fetchSemanticsNode()
            .boundsInRoot
        val firstItemBounds = composeRule
            .onNodeWithTag(FIRST_ITEM_TAG)
            .fetchSemanticsNode()
            .boundsInRoot

        assertEquals(0f, viewportBounds.top, 0.5f)
        assertTrue(firstItemBounds.top > viewportBounds.top)

        composeRule.onNodeWithTag(SCROLLABLE_CONTENT_TAG).performTouchInput { swipeUp() }
        composeRule.waitForIdle()

        composeRule.runOnIdle {
            assertTrue(scrollBehavior.state.heightOffset < 0f)
        }

        composeRule.onNodeWithTag(SCROLLABLE_CONTENT_TAG).performTouchInput { swipeDown() }
        composeRule.waitForIdle()

        composeRule.runOnIdle {
            assertEquals(0f, scrollBehavior.state.heightOffset, 0.5f)
        }
    }

    @Test
    fun disabledScrollBehaviorKeepsTopBarVisible() {
        setScrollableContent(canScroll = { false })

        composeRule.onNodeWithTag(SCROLLABLE_CONTENT_TAG).performTouchInput { swipeUp() }
        composeRule.waitForIdle()

        composeRule.runOnIdle {
            assertEquals(0f, scrollBehavior.state.heightOffset, 0f)
        }
    }

    @Test
    fun scaffoldPaddingKeepsGridViewportEdgeToEdge() {
        composeRule.setContent {
            MaterialTheme {
                Scaffold(
                    topBar = {
                        CatlyticsTopAppBar(title = { Text("Playlists") })
                    },
                ) { contentPadding ->
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag(GRID_CONTENT_TAG),
                        contentPadding = contentPadding,
                    ) {
                        item {
                            Text(
                                text = "First grid item",
                                modifier = Modifier.testTag(FIRST_GRID_ITEM_TAG),
                            )
                        }
                    }
                }
            }
        }

        val viewportBounds = composeRule
            .onNodeWithTag(GRID_CONTENT_TAG)
            .fetchSemanticsNode()
            .boundsInRoot
        val firstItemBounds = composeRule
            .onNodeWithTag(FIRST_GRID_ITEM_TAG)
            .fetchSemanticsNode()
            .boundsInRoot

        assertEquals(0f, viewportBounds.top, 0.5f)
        assertTrue(firstItemBounds.top > viewportBounds.top)
    }

    private fun setScrollableContent(canScroll: () -> Boolean) {
        composeRule.setContent {
            MaterialTheme {
                val behavior = TopAppBarDefaults.enterAlwaysScrollBehavior(
                    canScroll = canScroll,
                )
                SideEffect {
                    scrollBehavior = behavior
                }

                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(behavior.nestedScrollConnection),
                    topBar = {
                        CatlyticsTopAppBar(
                            title = { Text("Biblioteca") },
                            scrollBehavior = behavior,
                        )
                    },
                ) { contentPadding ->
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag(SCROLLABLE_CONTENT_TAG),
                        contentPadding = contentPadding,
                    ) {
                        item {
                            Text(
                                text = "First item",
                                modifier = Modifier.testTag(FIRST_ITEM_TAG),
                            )
                        }
                        items((1..100).toList()) { item ->
                            Text("Canción $item")
                        }
                    }
                }
            }
        }
    }

    private companion object {
        const val SCROLLABLE_CONTENT_TAG = "top-bar-scrollable-content"
        const val FIRST_ITEM_TAG = "first-scrollable-item"
        const val GRID_CONTENT_TAG = "edge-to-edge-grid"
        const val FIRST_GRID_ITEM_TAG = "first-grid-item"
    }
}
