package com.catlytics.feature.library.impl.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.catlytics.feature.library.api.LibraryAlbumRoute as LibraryAlbumDestination
import com.catlytics.feature.library.api.LibraryArtistRoute as LibraryArtistDestination
import com.catlytics.feature.library.api.LibraryFolderRoute as LibraryFolderDestination
import com.catlytics.feature.library.api.LibraryRoute
import com.catlytics.feature.library.impl.album.LibraryAlbumRoute
import com.catlytics.feature.library.impl.artist.LibraryArtistRoute
import com.catlytics.feature.library.impl.folder.LibraryFolderRoute
import com.catlytics.feature.library.impl.root.LibraryRoute as LibraryRootRoute
import com.catlytics.core.model.PlaylistSource

fun EntryProviderScope<NavKey>.libraryEntry(
    onDestinationSelected: (NavKey) -> Unit,
    onAddToPlaylist: (PlaylistSource) -> Unit,
) {
    entry<LibraryRoute> {
        LibraryRootRoute(
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
        )
    }
    entry<LibraryAlbumDestination> { route ->
        LibraryAlbumRoute(route = route, onAddToPlaylist = onAddToPlaylist)
    }
    entry<LibraryArtistDestination> { route ->
        LibraryArtistRoute(
            route = route,
            onAlbumSelected = { album ->
                onDestinationSelected(LibraryAlbumDestination(album.id, album.title))
            },
            onAddToPlaylist = onAddToPlaylist,
        )
    }
    entry<LibraryFolderDestination> { route ->
        LibraryFolderRoute(
            route = route,
            onFolderSelected = { folder ->
                onDestinationSelected(LibraryFolderDestination(folder.id, folder.name))
            },
            onAddToPlaylist = onAddToPlaylist,
        )
    }
}
