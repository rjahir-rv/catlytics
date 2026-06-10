package com.catlytics.feature.library.impl.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.catlytics.feature.library.api.LibraryFolderRoute as LibraryFolderDestination
import com.catlytics.feature.library.api.LibraryRoute
import com.catlytics.feature.library.impl.folder.LibraryFolderRoute
import com.catlytics.feature.library.impl.root.LibraryRoute as LibraryRootRoute

fun EntryProviderScope<NavKey>.libraryEntry(
    onFolderSelected: (LibraryFolderDestination) -> Unit,
) {
    entry<LibraryRoute> {
        LibraryRootRoute(
            onFolderSelected = { folder ->
                onFolderSelected(LibraryFolderDestination(folder.id, folder.name))
            },
        )
    }
    entry<LibraryFolderDestination> { route ->
        LibraryFolderRoute(
            route = route,
            onFolderSelected = { folder ->
                onFolderSelected(LibraryFolderDestination(folder.id, folder.name))
            },
        )
    }
}
