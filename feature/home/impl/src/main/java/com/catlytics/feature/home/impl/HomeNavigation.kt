package com.catlytics.feature.home.impl

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.catlytics.core.model.Track
import com.catlytics.core.model.TrackSelectionAction
import com.catlytics.feature.home.api.DailyPlaylistRoute
import com.catlytics.feature.home.api.HomeRoute

fun EntryProviderScope<NavKey>.homeEntry(
    searchQuery: () -> String,
    onTrackOptions: (Track) -> Unit,
    likedTrackIds: () -> Set<String> = { emptySet() },
    onTrackSelectionAction: (TrackSelectionAction) -> Unit = {},
    onNavigateToStatistics: () -> Unit,
    onNavigateToDailyPlaylist: () -> Unit,
    onNavigateToFavorites: () -> Unit,
    hasAudioPermission: () -> Boolean,
    onRequestAudioPermission: () -> Unit,
    startupError: () -> String?,
    onContentReady: () -> Unit,
    bottomPadding: () -> androidx.compose.ui.unit.Dp = { 0.dp },
    scaffoldContentPadding: () -> PaddingValues = { PaddingValues(0.dp) },
) {
    entry<HomeRoute> {
        HomeRoute(
            searchQuery = searchQuery(),
            onTrackOptions = onTrackOptions,
            likedTrackIds = likedTrackIds(),
            onTrackSelectionAction = onTrackSelectionAction,
            onNavigateToStatistics = onNavigateToStatistics,
            onNavigateToDailyPlaylist = onNavigateToDailyPlaylist,
            onNavigateToFavorites = onNavigateToFavorites,
            hasAudioPermission = hasAudioPermission(),
            onRequestPermission = onRequestAudioPermission,
            startupError = startupError(),
            onContentReady = onContentReady,
            bottomPadding = bottomPadding,
            scaffoldContentPadding = scaffoldContentPadding(),
        )
    }
    entry<DailyPlaylistRoute> {
        DailyPlaylistRoute(
            onTrackOptions = onTrackOptions,
            likedTrackIds = likedTrackIds(),
            onTrackSelectionAction = onTrackSelectionAction,
            bottomPadding = bottomPadding,
            scaffoldContentPadding = scaffoldContentPadding(),
        )
    }
}
