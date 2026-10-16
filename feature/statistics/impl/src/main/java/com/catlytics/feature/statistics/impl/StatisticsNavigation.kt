package com.catlytics.feature.statistics.impl

import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.catlytics.core.designsystem.layout.LocalPaneChrome
import com.catlytics.feature.statistics.api.StatisticsExploreRoute
import com.catlytics.feature.statistics.api.StatisticsRoute

fun EntryProviderScope<NavKey>.statisticsEntry(
    bottomPadding: () -> androidx.compose.ui.unit.Dp = { 0.dp },
    onNavigateToExplore: () -> Unit = {},
) {
    entry<StatisticsRoute> {
        StatisticsScreen(
            bottomPadding = bottomPadding,
            scaffoldContentPadding = LocalPaneChrome.current.contentPadding,
            onExploreClick = onNavigateToExplore,
        )
    }
    entry<StatisticsExploreRoute> {
        StatisticsExploreScreen(
            bottomPadding = bottomPadding,
            scaffoldContentPadding = LocalPaneChrome.current.contentPadding,
        )
    }
}
