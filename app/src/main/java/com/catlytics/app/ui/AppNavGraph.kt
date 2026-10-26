package com.catlytics.app.ui

import androidx.compose.ui.unit.Dp
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.catlytics.app.R
import com.catlytics.app.navigation.AppNavigator
import com.catlytics.core.designsystem.text.UiText
import com.catlytics.core.model.Playlist
import com.catlytics.core.model.PlaylistSource
import com.catlytics.core.model.Track
import com.catlytics.core.model.TrackSelectionAction
import com.catlytics.feature.home.api.DailyPlaylistRoute
import com.catlytics.feature.home.api.RecentlyAddedRoute
import com.catlytics.feature.home.impl.homeEntry
import com.catlytics.feature.library.impl.navigation.libraryEntry
import com.catlytics.feature.playlists.api.PlaylistDetailRoute
import com.catlytics.feature.playlists.impl.playlistsEntry
import com.catlytics.feature.settings.impl.settingsEntry
import com.catlytics.feature.statistics.api.StatisticsRoute
import com.catlytics.feature.statistics.impl.statisticsEntry

internal fun EntryProviderScope<NavKey>.appEntries(
    navigator: AppNavigator,
    search: AppSearchState,
    detailChrome: DetailChromeState,
    likedTrackIds: () -> Set<String>,
    currentTrackId: () -> String?,
    isCurrentTrackPlaying: () -> Boolean,
    onTrackOptions: (Track, (() -> Unit)?) -> Unit,
    onAddToPlaylist: (PlaylistSource) -> Unit,
    onTrackSelectionAction: (TrackSelectionAction) -> Unit,
    hasAudioPermission: () -> Boolean,
    onRequestAudioPermission: () -> Unit,
    startupError: () -> UiText?,
    onHomeContentReady: () -> Unit,
    likedPlaylistName: String,
    appVersion: String,
    onSettingsTopBarTitleChange: (String) -> Unit,
    onSettingsTopBarBackActionChange: ((() -> Unit)?) -> Unit,
    onPlaylistCreatedInBackground: (Playlist) -> Unit,
    bottomPadding: () -> Dp,
) {
    homeEntry(
        searchQuery = { search.home.query },
        isSearchActive = { search.home.isExpanded },
        onTrackOptions = { track -> onTrackOptions(track, null) },
        likedTrackIds = likedTrackIds,
        onTrackSelectionAction = onTrackSelectionAction,
        onNavigateToStatistics = { navigator.addTopLevel(StatisticsRoute) },
        onNavigateToDailyPlaylist = { navigator.add(DailyPlaylistRoute) },
        onNavigateToRecentlyAdded = { navigator.add(RecentlyAddedRoute) },
        onNavigateToFavorites = { navigator.openLikedPlaylist(likedPlaylistName) },
        hasAudioPermission = hasAudioPermission,
        onRequestAudioPermission = onRequestAudioPermission,
        startupError = startupError,
        onContentReady = onHomeContentReady,
        bottomPadding = bottomPadding,
    )
    libraryEntry(
        searchQuery = { search.library.query },
        artistSearchQuery = { search.artist.query },
        isArtistSearchActive = { search.artist.isExpanded },
        onArtistSearchQueryChange = { search.artist.query = it },
        onDestinationSelected = navigator::add,
        onListDestinationSelected = navigator::openFromListPane,
        onAddToPlaylist = onAddToPlaylist,
        onTrackOptions = { track -> onTrackOptions(track, null) },
        likedTrackIds = likedTrackIds,
        currentTrackId = currentTrackId,
        isCurrentTrackPlaying = isCurrentTrackPlaying,
        onTrackSelectionAction = onTrackSelectionAction,
        onLibraryDetailTopBarColorChange = { route, color -> detailChrome.colors[route] = color },
        onLibraryDetailTitleChange = { route, title -> detailChrome.titles[route] = title },
        hasAudioPermission = hasAudioPermission,
        onRequestAudioPermission = onRequestAudioPermission,
        bottomPadding = bottomPadding,
    )
    playlistsEntry(
        searchQuery = { search.playlists.query },
        onDestinationSelected = navigator::add,
        onListDestinationSelected = navigator::openFromListPane,
        onPlaylistDeleted = navigator::closeCurrent,
        onTrackOptions = onTrackOptions,
        likedTrackIds = likedTrackIds,
        onTrackSelectionAction = onTrackSelectionAction,
        bottomPadding = bottomPadding,
        onPlaylistDetailTopBarColorChange = { route, color -> detailChrome.colors[route] = color },
        onCloseCreatePlaylist = navigator::closeCurrent,
        onPlaylistCreated = { route, playlist ->
            navigator.closeCurrent()
            if (route.openDetailOnCreate) {
                navigator.add(PlaylistDetailRoute(playlistId = playlist.id, playlistName = playlist.name))
            } else {
                onPlaylistCreatedInBackground(playlist)
            }
        },
    )
    settingsEntry(
        appVersion = appVersion,
        librariesRes = R.raw.aboutlibraries,
        hasAudioPermission = hasAudioPermission,
        onRequestAudioPermission = onRequestAudioPermission,
        bottomPadding = bottomPadding,
        onTopBarTitleChange = onSettingsTopBarTitleChange,
        onTopBarBackActionChange = onSettingsTopBarBackActionChange,
    )
    statisticsEntry(
        bottomPadding = bottomPadding,
        onNavigateToExplore = navigator::openStatisticsExplore,
    )
}
