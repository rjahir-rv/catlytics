package com.catlytics.feature.home.impl

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.catlytics.feature.home.api.HomeRoute

fun EntryProviderScope<NavKey>.homeEntry(
    searchQuery: () -> String,
) {
    entry<HomeRoute> {
        HomeRoute(searchQuery = searchQuery())
    }
}
