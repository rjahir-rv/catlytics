package com.catlytics.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.navigation3.runtime.NavKey
import com.catlytics.app.playback.NowPlayingRoute
import com.catlytics.core.model.LIKED_PLAYLIST_ID
import com.catlytics.core.navigation.TopLevelBackStack
import com.catlytics.core.navigation.findListDetailIndices
import com.catlytics.feature.library.api.LibraryAlbumRoute
import com.catlytics.feature.library.api.LibraryArtistRoute
import com.catlytics.feature.library.api.LibraryFolderRoute
import com.catlytics.feature.library.api.LibraryRoute
import com.catlytics.feature.playlists.api.CreatePlaylistRoute
import com.catlytics.feature.playlists.api.PlaylistDetailRoute
import com.catlytics.feature.playlists.api.PlaylistsRoute
import com.catlytics.feature.settings.api.SettingsRoute
import com.catlytics.feature.statistics.api.StatisticsExploreRoute

@Stable
internal class AppNavigator(
    private val topLevelBackStack: TopLevelBackStack,
    private val isTwoPane: () -> Boolean,
) {
    val topLevelKey: NavKey
        get() = topLevelBackStack.topLevelKey

    val isNowPlayingOpen: Boolean
        get() = topLevelBackStack.backStack.lastOrNull() == NowPlayingRoute

    val contentBackStack: List<NavKey>
        get() = topLevelBackStack.backStack.filter { it != NowPlayingRoute }

    fun add(key: NavKey) = topLevelBackStack.add(key)

    fun addTopLevel(key: NavKey) = topLevelBackStack.addTopLevel(key)

    fun closeCurrent() = topLevelBackStack.removeLast()

    fun openNowPlaying() = addIfNotOnTop(NowPlayingRoute)

    fun closeNowPlaying() {
        if (isNowPlayingOpen) closeCurrent()
    }

    fun openSettings() = addIfNotOnTop(SettingsRoute)

    fun openStatisticsExplore() = addIfNotOnTop(StatisticsExploreRoute)

    fun openLikedPlaylist(name: String) {
        addTopLevel(PlaylistsRoute)
        add(PlaylistDetailRoute(playlistId = LIKED_PLAYLIST_ID, playlistName = name))
    }

    fun openCreatePlaylist(trackIds: List<String>) {
        add(CreatePlaylistRoute(initialTrackIds = trackIds, openDetailOnCreate = false))
    }

    fun openLibraryDetail(route: NavKey) {
        closeNowPlaying()
        addTopLevel(LibraryRoute)
        add(route)
    }

    fun openFromListPane(key: NavKey) {
        val backStack = contentBackStack
        val indices = if (isTwoPane()) {
            findListDetailIndices(
                items = backStack,
                isList = ::isListPaneRoute,
                isDetail = ::isDetailPaneRoute,
            )
        } else {
            null
        }
        val detailIndex = indices?.detailIndex
        when {
            detailIndex == null -> add(key)
            backStack[detailIndex] == key -> Unit
            else -> topLevelBackStack.replaceAbove(anchor = backStack[indices.listIndex], key = key)
        }
    }

    private fun addIfNotOnTop(key: NavKey) {
        if (topLevelBackStack.backStack.lastOrNull() != key) add(key)
    }
}

@Composable
internal fun rememberAppNavigator(
    topLevelBackStack: TopLevelBackStack,
    isTwoPane: Boolean,
): AppNavigator {
    val currentIsTwoPane by rememberUpdatedState(isTwoPane)
    return remember(topLevelBackStack) {
        AppNavigator(topLevelBackStack) { currentIsTwoPane }
    }
}

internal fun isListPaneRoute(route: NavKey): Boolean =
    route == LibraryRoute || route == PlaylistsRoute

internal fun isDetailPaneRoute(route: NavKey): Boolean =
    route is LibraryAlbumRoute ||
        route is LibraryArtistRoute ||
        route is LibraryFolderRoute ||
        route is PlaylistDetailRoute
