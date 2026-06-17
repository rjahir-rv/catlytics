package com.catlytics.feature.home.impl

import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.catlytics.feature.home.api.HomeRoute
import com.catlytics.core.model.Track

fun EntryProviderScope<NavKey>.homeEntry(
    searchQuery: () -> String,
    onTrackOptions: (Track) -> Unit,
    contentModifier: Modifier = Modifier,
) {
    entry<HomeRoute> {
        Box(modifier = contentModifier) {
            HomeRoute(searchQuery = searchQuery(), onTrackOptions = onTrackOptions)
        }
    }
}
