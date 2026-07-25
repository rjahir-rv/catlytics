package com.catlytics.app.ui

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class TopBarContentPaddingTest {
    @Test
    fun expandedTopBarKeepsFullContentPadding() {
        val padding = calculateTopContentPadding(
            scaffoldTopPadding = 88.dp,
            statusBarPadding = 24.dp,
            collapsedFraction = 0f,
        )

        assertEquals(88.dp, padding)
    }

    @Test
    fun collapsedTopBarLetsContentDrawBehindStatusBar() {
        val padding = calculateTopContentPadding(
            scaffoldTopPadding = 24.dp,
            statusBarPadding = 24.dp,
            collapsedFraction = 1f,
        )

        assertEquals(0.dp, padding)
    }

    @Test
    fun collapsingTopBarMovesContentContinuouslyTowardTopEdge() {
        val padding = calculateTopContentPadding(
            scaffoldTopPadding = 56.dp,
            statusBarPadding = 24.dp,
            collapsedFraction = 0.5f,
        )

        assertEquals(44.dp, padding)
    }
}
