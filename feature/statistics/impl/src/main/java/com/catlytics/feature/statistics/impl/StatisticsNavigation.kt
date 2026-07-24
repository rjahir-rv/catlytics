package com.catlytics.feature.statistics.impl

import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.catlytics.feature.statistics.api.StatisticsRoute

fun EntryProviderScope<NavKey>.statisticsEntry(
    bottomPadding: () -> androidx.compose.ui.unit.Dp = { 0.dp },
    contentPadding: () -> androidx.compose.foundation.layout.PaddingValues = { androidx.compose.foundation.layout.PaddingValues(0.dp) },
) {
    entry<StatisticsRoute> {
        StatisticsScreen(
            bottomPadding = bottomPadding,
            scaffoldContentPadding = contentPadding(),
        )
    }
}
