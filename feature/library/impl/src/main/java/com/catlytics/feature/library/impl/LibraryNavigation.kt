package com.catlytics.feature.library.impl

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.catlytics.feature.library.api.LibraryRoute

fun EntryProviderScope<NavKey>.libraryEntry() {
    entry<LibraryRoute> {
        LibraryScreen()
    }
}
