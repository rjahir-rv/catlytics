package com.catlytics.feature.playlists.impl

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.catlytics.core.model.Track
import com.catlytics.feature.playlists.api.PlaylistsRoute
import com.catlytics.feature.playlists.api.PlaylistDetailRoute

fun EntryProviderScope<NavKey>.playlistsEntry(
    searchQuery: () -> String,
    onDestinationSelected: (NavKey) -> Unit,
    onTrackOptions: (track: Track, onRemoveFromPlaylist: () -> Unit) -> Unit,
    bottomPadding: () -> Dp = { 0.dp },
    onPlaylistDetailTopBarColorChange: (Color) -> Unit,
    contentPadding: () -> androidx.compose.foundation.layout.PaddingValues = { androidx.compose.foundation.layout.PaddingValues(0.dp) },
) {
    entry<PlaylistsRoute> {
        Box(modifier = Modifier.padding(contentPadding())) {
            val viewModel: PlaylistsViewModel = hiltViewModel()
            val playlists by viewModel.playlists.collectAsStateWithLifecycle()
            val viewMode by viewModel.viewMode.collectAsStateWithLifecycle()
            val sortDirection by viewModel.sortDirection.collectAsStateWithLifecycle()
            PlaylistsScreen(
                playlists = playlists,
                viewMode = viewMode,
                onViewModeChange = viewModel::setViewMode,
                onPlaylistSelected = { playlist ->
                    onDestinationSelected(PlaylistDetailRoute(playlist.id, playlist.name))
                },
                onCreate = viewModel::create,
                onRename = viewModel::rename,
                onDelete = viewModel::delete,
                onSetCover = viewModel::setCover,
                searchQuery = searchQuery(),
                sortDirection = sortDirection,
                onSortDirectionChange = viewModel::setSortDirection,
                bottomPadding = bottomPadding,
            )
        }
    }
    entry<PlaylistDetailRoute> { route ->
        Box(modifier = Modifier.padding(contentPadding())) {
            PlaylistDetailRoute(
                playlistId = route.playlistId,
                onTrackOptions = onTrackOptions,
                onTopBarColorChange = onPlaylistDetailTopBarColorChange,
                bottomPadding = bottomPadding,
            )
        }
    }
}
