package com.catlytics.feature.home.impl

import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.catlytics.feature.home.api.HomeRoute
import com.catlytics.core.model.Track

fun EntryProviderScope<NavKey>.homeEntry(
    searchQuery: () -> String,
    onTrackOptions: (Track) -> Unit,
    bottomPadding: () -> androidx.compose.ui.unit.Dp = { 0.dp },
    contentPadding: () -> androidx.compose.foundation.layout.PaddingValues = { androidx.compose.foundation.layout.PaddingValues(0.dp) },
) {
    entry<HomeRoute> {
        HomeRoute(
            searchQuery = searchQuery(),
            onTrackOptions = onTrackOptions,
            bottomPadding = bottomPadding,
            modifier = Modifier.padding(contentPadding()),
        )
    }
}
