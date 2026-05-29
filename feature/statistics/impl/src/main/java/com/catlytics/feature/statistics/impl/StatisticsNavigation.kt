package com.catlytics.feature.statistics.impl

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.catlytics.feature.statistics.api.StatisticsRoute

fun EntryProviderScope<NavKey>.statisticsEntry() {
    entry<StatisticsRoute> {
        StatisticsScreen()
    }
}
