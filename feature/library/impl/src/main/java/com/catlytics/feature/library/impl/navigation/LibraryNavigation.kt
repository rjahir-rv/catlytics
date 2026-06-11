package com.catlytics.feature.library.impl.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.catlytics.feature.library.api.LibraryAlbumRoute as LibraryAlbumDestination
import com.catlytics.feature.library.api.LibraryFolderRoute as LibraryFolderDestination
import com.catlytics.feature.library.api.LibraryRoute
import com.catlytics.feature.library.impl.album.LibraryAlbumRoute
import com.catlytics.feature.library.impl.folder.LibraryFolderRoute
import com.catlytics.feature.library.impl.root.LibraryRoute as LibraryRootRoute

fun EntryProviderScope<NavKey>.libraryEntry(
    onDestinationSelected: (NavKey) -> Unit,
) {
    entry<LibraryRoute> {
        LibraryRootRoute(
            onAlbumSelected = { album ->
                onDestinationSelected(LibraryAlbumDestination(album.id, album.title))
            },
            onFolderSelected = { folder ->
                onDestinationSelected(LibraryFolderDestination(folder.id, folder.name))
            },
        )
    }
    entry<LibraryAlbumDestination> { route ->
        LibraryAlbumRoute(route = route)
    }
    entry<LibraryFolderDestination> { route ->
        LibraryFolderRoute(
            route = route,
            onFolderSelected = { folder ->
                onDestinationSelected(LibraryFolderDestination(folder.id, folder.name))
            },
        )
    }
}
