package com.catlytics.feature.library.impl.navigation

import com.catlytics.core.designsystem.layout.CatlyticsLayoutTokens
import com.catlytics.core.designsystem.layout.LocalPaneChrome
import com.catlytics.feature.library.impl.R
import com.catlytics.core.designsystem.component.CatlyticsEmptyState
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.Composable
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.catlytics.core.model.PlaylistSource
import com.catlytics.core.model.Track
import com.catlytics.core.model.TrackSelectionAction
import com.catlytics.feature.library.api.LibraryAlbumRoute as LibraryAlbumDestination
import com.catlytics.feature.library.api.LibraryArtistRoute as LibraryArtistDestination
import com.catlytics.feature.library.api.LibraryFolderRoute as LibraryFolderDestination
import com.catlytics.feature.library.api.LibraryRoute
import com.catlytics.feature.library.impl.album.LibraryAlbumRoute
import com.catlytics.feature.library.impl.artist.LibraryArtistRoute
import com.catlytics.feature.library.impl.folder.LibraryFolderRoute
import com.catlytics.feature.library.impl.root.LibraryRoute as LibraryRootRoute

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
fun EntryProviderScope<NavKey>.libraryEntry(
    searchQuery: () -> String,
    artistSearchQuery: () -> String,
    isArtistSearchActive: () -> Boolean = { false },
    onArtistSearchQueryChange: (String) -> Unit,
    onDestinationSelected: (NavKey) -> Unit,
    onListDestinationSelected: (NavKey) -> Unit = onDestinationSelected,
    onAddToPlaylist: (PlaylistSource) -> Unit,
    onTrackOptions: (Track) -> Unit,
    likedTrackIds: () -> Set<String> = { emptySet() },
    currentTrackId: () -> String? = { null },
    isCurrentTrackPlaying: () -> Boolean = { false },
    onTrackSelectionAction: (TrackSelectionAction) -> Unit = {},
    onLibraryDetailTopBarColorChange: (NavKey, Color) -> Unit,
    onLibraryDetailTitleChange: (NavKey, String) -> Unit = { _, _ -> },
    hasAudioPermission: () -> Boolean,
    onRequestAudioPermission: () -> Unit,
    bottomPadding: () -> Dp = { 0.dp },
) {
    entry<LibraryRoute>(
        metadata = ListDetailSceneStrategy.listPane(detailPlaceholder = { LibraryDetailPlaceholder() }) +
            ListDetailSceneStrategy.preferredPaneSize(width = CatlyticsLayoutTokens.ListPanePreferredWidth),
    ) {
        LibraryRootRoute(
            searchQuery = searchQuery(),
            onAlbumSelected = { album ->
                onListDestinationSelected(LibraryAlbumDestination(album.id, album.title))
            },
            onArtistSelected = { artist ->
                onListDestinationSelected(
                    LibraryArtistDestination(artist.artist.id, artist.artist.name),
                )
            },
            onFolderSelected = { folder ->
                onListDestinationSelected(LibraryFolderDestination(folder.id, folder.name))
            },
            onAddToPlaylist = onAddToPlaylist,
            hasAudioPermission = hasAudioPermission(),
            onRequestPermission = onRequestAudioPermission,
            bottomPadding = bottomPadding,
            scaffoldContentPadding = LocalPaneChrome.current.contentPadding,
            collapseFraction = LocalPaneChrome.current.collapsedFraction,
        )
    }
    entry<LibraryAlbumDestination>(metadata = ListDetailSceneStrategy.detailPane()) { route ->
        LibraryAlbumRoute(
            route = route,
            onTrackOptions = onTrackOptions,
            onAddToPlaylist = onAddToPlaylist,
            onArtistSelected = { artist ->
                onDestinationSelected(LibraryArtistDestination(artist.id, artist.name))
            },
            likedTrackIds = likedTrackIds(),
            currentTrackId = currentTrackId(),
            isCurrentTrackPlaying = isCurrentTrackPlaying(),
            onTrackSelectionAction = onTrackSelectionAction,
            onTopBarColorChange = { color ->
                onLibraryDetailTopBarColorChange(route, color)
            },
            onTitleChange = { title -> onLibraryDetailTitleChange(route, title) },
            bottomPadding = bottomPadding,
            scaffoldContentPadding = LocalPaneChrome.current.contentPadding,
        )
    }
    entry<LibraryArtistDestination>(metadata = ListDetailSceneStrategy.detailPane()) { route ->
        LibraryArtistRoute(
            route = route,
            onAlbumSelected = { album ->
                onDestinationSelected(LibraryAlbumDestination(album.id, album.title))
            },
            onAddToPlaylist = onAddToPlaylist,
            onTrackOptions = onTrackOptions,
            likedTrackIds = likedTrackIds(),
            currentTrackId = currentTrackId(),
            isCurrentTrackPlaying = isCurrentTrackPlaying(),
            onTrackSelectionAction = onTrackSelectionAction,
            onTopBarColorChange = { color ->
                onLibraryDetailTopBarColorChange(route, color)
            },
            searchQuery = artistSearchQuery(),
            isSearchActive = isArtistSearchActive(),
            onSearchQueryChange = onArtistSearchQueryChange,
            bottomPadding = bottomPadding,
            scaffoldContentPadding = LocalPaneChrome.current.contentPadding,
        )
    }
    entry<LibraryFolderDestination>(metadata = ListDetailSceneStrategy.detailPane()) { route ->
        LibraryFolderRoute(
            route = route,
            onFolderSelected = { folder ->
                onDestinationSelected(LibraryFolderDestination(folder.id, folder.name))
            },
            onAddToPlaylist = onAddToPlaylist,
            onTrackOptions = onTrackOptions,
            likedTrackIds = likedTrackIds(),
            currentTrackId = currentTrackId(),
            isCurrentTrackPlaying = isCurrentTrackPlaying(),
            onTrackSelectionAction = onTrackSelectionAction,
            bottomPadding = bottomPadding,
            scaffoldContentPadding = LocalPaneChrome.current.contentPadding,
        )
    }
}

@Composable
private fun LibraryDetailPlaceholder() {
    CatlyticsEmptyState(
        title = stringResource(R.string.library_detail_placeholder_title),
        message = stringResource(R.string.library_detail_placeholder_message),
        messageColor = MaterialTheme.colorScheme.onSurfaceVariant,
        mascotSize = 160.dp,
    )
}
