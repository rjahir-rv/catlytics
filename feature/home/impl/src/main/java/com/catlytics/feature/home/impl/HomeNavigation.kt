package com.catlytics.feature.home.impl

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.catlytics.feature.home.api.HomeRoute
import com.catlytics.core.model.PlaylistSource

fun EntryProviderScope<NavKey>.homeEntry(
    searchQuery: () -> String,
    onAddToPlaylist: (PlaylistSource) -> Unit,
) {
    entry<HomeRoute> {
        HomeRoute(searchQuery = searchQuery(), onAddToPlaylist = onAddToPlaylist)
    }
}
