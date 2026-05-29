package com.catlytics.feature.playlists.impl

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.catlytics.feature.playlists.api.PlaylistsRoute

fun EntryProviderScope<NavKey>.playlistsEntry() {
    entry<PlaylistsRoute> {
        PlaylistsScreen()
    }
}
