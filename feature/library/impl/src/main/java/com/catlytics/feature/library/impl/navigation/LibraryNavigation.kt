package com.catlytics.feature.library.impl.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.catlytics.core.model.PlaylistSource
import com.catlytics.core.model.Track
import com.catlytics.feature.library.api.LibraryAlbumRoute as LibraryAlbumDestination
import com.catlytics.feature.library.api.LibraryArtistRoute as LibraryArtistDestination
import com.catlytics.feature.library.api.LibraryFolderRoute as LibraryFolderDestination
import com.catlytics.feature.library.api.LibraryRoute
import com.catlytics.feature.library.impl.album.LibraryAlbumRoute
import com.catlytics.feature.library.impl.artist.LibraryArtistRoute
import com.catlytics.feature.library.impl.folder.LibraryFolderRoute
import com.catlytics.feature.library.impl.root.LibraryRoute as LibraryRootRoute

fun EntryProviderScope<NavKey>.libraryEntry(
    searchQuery: () -> String,
    artistSearchQuery: () -> String,
    onArtistSearchQueryChange: (String) -> Unit,
    onDestinationSelected: (NavKey) -> Unit,
    onAddToPlaylist: (PlaylistSource) -> Unit,
    onTrackOptions: (Track) -> Unit,
    onLibraryDetailTopBarColorChange: (Color) -> Unit,
    bottomPadding: () -> Dp = { 0.dp },
    contentPadding: () -> androidx.compose.foundation.layout.PaddingValues = { androidx.compose.foundation.layout.PaddingValues(0.dp) },
) {
    entry<LibraryRoute> {
        Box(modifier = Modifier.padding(contentPadding())) {
            LibraryRootRoute(
                searchQuery = searchQuery(),
                onAlbumSelected = { album ->
                    onDestinationSelected(LibraryAlbumDestination(album.id, album.title))
                },
                onArtistSelected = { artist ->
                    onDestinationSelected(
                        LibraryArtistDestination(artist.artist.id, artist.artist.name),
                    )
                },
                onFolderSelected = { folder ->
                    onDestinationSelected(LibraryFolderDestination(folder.id, folder.name))
                },
                onAddToPlaylist = onAddToPlaylist,
                bottomPadding = bottomPadding,
            )
        }
    }
    entry<LibraryAlbumDestination> { route ->
        Box(modifier = Modifier.padding(contentPadding())) {
            LibraryAlbumRoute(
                route = route,
                onTrackOptions = onTrackOptions,
                onTopBarColorChange = onLibraryDetailTopBarColorChange,
                bottomPadding = bottomPadding,
            )
        }
    }
    entry<LibraryArtistDestination> { route ->
        Box(modifier = Modifier.padding(contentPadding())) {
            LibraryArtistRoute(
                route = route,
                onAlbumSelected = { album ->
                    onDestinationSelected(LibraryAlbumDestination(album.id, album.title))
                },
                onAddToPlaylist = onAddToPlaylist,
                onTrackOptions = onTrackOptions,
                onTopBarColorChange = onLibraryDetailTopBarColorChange,
                searchQuery = artistSearchQuery(),
                onSearchQueryChange = onArtistSearchQueryChange,
                bottomPadding = bottomPadding,
            )
        }
    }
    entry<LibraryFolderDestination> { route ->
        Box(modifier = Modifier.padding(contentPadding())) {
            LibraryFolderRoute(
                route = route,
                onFolderSelected = { folder ->
                    onDestinationSelected(LibraryFolderDestination(folder.id, folder.name))
                },
                onAddToPlaylist = onAddToPlaylist,
                onTrackOptions = onTrackOptions,
                bottomPadding = bottomPadding,
            )
        }
    }
}
