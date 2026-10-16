package com.catlytics.feature.playlists.impl

import com.catlytics.core.designsystem.layout.CatlyticsLayoutTokens
import com.catlytics.core.designsystem.layout.LocalPaneChrome
import com.catlytics.core.designsystem.component.CatlyticsEmptyState
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.metadata
import androidx.navigation3.ui.NavDisplay
import com.catlytics.core.model.LIKED_PLAYLIST_ID
import com.catlytics.core.model.Playlist
import com.catlytics.core.model.Track
import com.catlytics.core.model.TrackSelectionAction
import com.catlytics.core.navigation.verticalSheetEnterTransition
import com.catlytics.core.navigation.verticalSheetExitTransition
import com.catlytics.feature.playlists.api.CreatePlaylistRoute
import com.catlytics.feature.playlists.api.PlaylistsRoute
import com.catlytics.feature.playlists.api.PlaylistDetailRoute

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
fun EntryProviderScope<NavKey>.playlistsEntry(
    searchQuery: () -> String,
    onDestinationSelected: (NavKey) -> Unit,
    onListDestinationSelected: (NavKey) -> Unit = onDestinationSelected,
    onTrackOptions: (track: Track, onRemoveFromPlaylist: () -> Unit) -> Unit,
    likedTrackIds: () -> Set<String> = { emptySet() },
    onTrackSelectionAction: (TrackSelectionAction) -> Unit = {},
    onPlaylistDeleted: () -> Unit,
    bottomPadding: () -> Dp = { 0.dp },
    onPlaylistDetailTopBarColorChange: (NavKey, Color) -> Unit,
    onCloseCreatePlaylist: () -> Unit = {},
    onPlaylistCreated: (route: CreatePlaylistRoute, playlist: Playlist) -> Unit = { _, _ -> },
) {
    entry<PlaylistsRoute>(
        metadata = ListDetailSceneStrategy.listPane(detailPlaceholder = { PlaylistDetailPlaceholder() }) +
            ListDetailSceneStrategy.preferredPaneSize(width = CatlyticsLayoutTokens.ListPanePreferredWidth),
    ) {
        val viewModel: PlaylistsViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        val activePlaylist by viewModel.activePlaylist.collectAsStateWithLifecycle()
        val viewMode by viewModel.viewMode.collectAsStateWithLifecycle()
        val sortDirection by viewModel.sortDirection.collectAsStateWithLifecycle()
        val likedPlaylistName = stringResource(R.string.playlist_liked_name)
        PlaylistsScreen(
            uiState = uiState,
            activePlaylist = activePlaylist,
            viewMode = viewMode,
            onViewModeChange = viewModel::setViewMode,
            onPlaylistSelected = { playlist ->
                onListDestinationSelected(
                    PlaylistDetailRoute(
                        playlistId = playlist.id,
                        playlistName = if (playlist.id == LIKED_PLAYLIST_ID) {
                            likedPlaylistName
                        } else {
                            playlist.name
                        },
                    ),
                )
            },
            onCreateClick = { onDestinationSelected(CreatePlaylistRoute()) },
            onRename = viewModel::rename,
            onDelete = viewModel::delete,
            onSetCover = viewModel::setCover,
            onPlay = viewModel::play,
            onPlayShuffled = viewModel::playShuffled,
            onTogglePlayback = viewModel::togglePlayback,
            searchQuery = searchQuery(),
            sortDirection = sortDirection,
            onSortDirectionChange = viewModel::setSortDirection,
            bottomPadding = bottomPadding,
            scaffoldContentPadding = LocalPaneChrome.current.contentPadding,
            collapseFraction = LocalPaneChrome.current.collapsedFraction,
        )
    }
    entry<PlaylistDetailRoute>(metadata = ListDetailSceneStrategy.detailPane()) { route ->
        PlaylistDetailRoute(
            playlistId = route.playlistId,
            onTrackOptions = onTrackOptions,
            likedTrackIds = likedTrackIds(),
            onTrackSelectionAction = onTrackSelectionAction,
            onTopBarColorChange = { color ->
                onPlaylistDetailTopBarColorChange(route, color)
            },
            onDeleted = onPlaylistDeleted,
            bottomPadding = bottomPadding,
            scaffoldContentPadding = LocalPaneChrome.current.contentPadding,
        )
    }
    entry<CreatePlaylistRoute>(
        metadata = metadata {
            put(NavDisplay.TransitionKey) { verticalSheetEnterTransition() }
            put(NavDisplay.PopTransitionKey) { verticalSheetExitTransition() }
            put(NavDisplay.PredictivePopTransitionKey) { _ -> verticalSheetExitTransition() }
        },
    ) { route ->
        val viewModel: CreatePlaylistViewModel = hiltViewModel()
        val playlists by viewModel.playlists.collectAsStateWithLifecycle()
        val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
        CreatePlaylistScreen(
            playlists = playlists,
            initialTrackCount = route.initialTrackIds.size,
            isSaving = isSaving,
            onClose = onCloseCreatePlaylist,
            onCreate = { name, description, artworkUri ->
                viewModel.create(
                    name = name,
                    description = description,
                    artworkUri = artworkUri,
                    trackIds = route.initialTrackIds,
                ) { playlist -> onPlaylistCreated(route, playlist) }
            },
        )
    }
}

@Composable
private fun PlaylistDetailPlaceholder() {
    CatlyticsEmptyState(
        title = stringResource(R.string.playlists_detail_placeholder_title),
        message = stringResource(R.string.playlists_detail_placeholder_message),
        messageColor = MaterialTheme.colorScheme.onSurfaceVariant,
        mascotSize = 160.dp,
    )
}
