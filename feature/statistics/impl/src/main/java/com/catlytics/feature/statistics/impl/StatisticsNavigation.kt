package com.catlytics.feature.statistics.impl

import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.catlytics.feature.statistics.api.StatisticsRoute

fun EntryProviderScope<NavKey>.statisticsEntry(
    contentModifier: Modifier = Modifier,
) {
    entry<StatisticsRoute> {
        Box(modifier = contentModifier) {
            StatisticsScreen()
        }
    }
}
