package com.catlytics.feature.statistics.impl

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.catlytics.feature.statistics.api.StatisticsRoute

fun EntryProviderScope<NavKey>.statisticsEntry(
    bottomPadding: () -> androidx.compose.ui.unit.Dp = { 0.dp },
    contentPadding: () -> androidx.compose.foundation.layout.PaddingValues = { androidx.compose.foundation.layout.PaddingValues(0.dp) },
) {
    entry<StatisticsRoute> {
        Box(modifier = Modifier.padding(contentPadding())) {
            StatisticsScreen(
                bottomPadding = bottomPadding
            )
        }
    }
}
