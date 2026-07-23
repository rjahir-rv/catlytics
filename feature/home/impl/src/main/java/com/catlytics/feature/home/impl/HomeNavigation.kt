package com.catlytics.feature.home.impl

import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.catlytics.core.model.Track
import com.catlytics.feature.home.api.DailyPlaylistRoute
import com.catlytics.feature.home.api.HomeRoute

fun EntryProviderScope<NavKey>.homeEntry(
    searchQuery: () -> String,
    onTrackOptions: (Track) -> Unit,
    onNavigateToStatistics: () -> Unit,
    onNavigateToDailyPlaylist: () -> Unit,
    onNavigateToFavorites: () -> Unit,
    hasAudioPermission: () -> Boolean,
    onRequestAudioPermission: () -> Unit,
    startupError: () -> String?,
    onContentReady: () -> Unit,
    bottomPadding: () -> androidx.compose.ui.unit.Dp = { 0.dp },
    contentPadding: () -> androidx.compose.foundation.layout.PaddingValues = { androidx.compose.foundation.layout.PaddingValues(0.dp) },
) {
    entry<HomeRoute> {
        HomeRoute(
            searchQuery = searchQuery(),
            onTrackOptions = onTrackOptions,
            onNavigateToStatistics = onNavigateToStatistics,
            onNavigateToDailyPlaylist = onNavigateToDailyPlaylist,
            onNavigateToFavorites = onNavigateToFavorites,
            hasAudioPermission = hasAudioPermission(),
            onRequestPermission = onRequestAudioPermission,
            startupError = startupError(),
            onContentReady = onContentReady,
            bottomPadding = bottomPadding,
            modifier = Modifier.padding(contentPadding()),
        )
    }
    entry<DailyPlaylistRoute> {
        DailyPlaylistRoute(
            onTrackOptions = onTrackOptions,
            bottomPadding = bottomPadding,
            modifier = Modifier.padding(contentPadding()),
        )
    }
}
