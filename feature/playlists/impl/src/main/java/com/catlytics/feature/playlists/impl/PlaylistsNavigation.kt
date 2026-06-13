package com.catlytics.feature.playlists.impl

import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.catlytics.feature.playlists.api.PlaylistsRoute
import com.catlytics.feature.playlists.api.PlaylistDetailRoute

fun EntryProviderScope<NavKey>.playlistsEntry(onDestinationSelected: (NavKey) -> Unit) {
    entry<PlaylistsRoute> {
        val viewModel: PlaylistsViewModel = hiltViewModel()
        val playlists by viewModel.playlists.collectAsStateWithLifecycle()
        val viewMode by viewModel.viewMode.collectAsStateWithLifecycle()
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
        )
    }
    entry<PlaylistDetailRoute> { route ->
        PlaylistDetailRoute(route.playlistId)
    }
}
