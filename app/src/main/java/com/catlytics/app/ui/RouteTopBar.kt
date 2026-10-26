package com.catlytics.app.ui

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.NavKey
import com.catlytics.app.R
import com.catlytics.app.navigation.TopLevelDestination
import com.catlytics.app.ui.chrome.LibraryDetailTopAppBar
import com.catlytics.app.ui.chrome.SettingsTopAppBar
import com.catlytics.app.ui.chrome.TopLevelTopAppBar
import com.catlytics.feature.home.api.DailyPlaylistRoute
import com.catlytics.feature.home.api.HomeRoute
import com.catlytics.feature.home.api.RecentlyAddedRoute
import com.catlytics.feature.library.api.LibraryAlbumRoute
import com.catlytics.feature.library.api.LibraryArtistRoute
import com.catlytics.feature.library.api.LibraryFolderRoute
import com.catlytics.feature.library.api.LibraryRoute
import com.catlytics.feature.playlists.api.CreatePlaylistRoute
import com.catlytics.feature.playlists.api.PlaylistDetailRoute
import com.catlytics.feature.playlists.api.PlaylistsRoute
import com.catlytics.feature.settings.api.SettingsRoute
import com.catlytics.feature.statistics.api.StatisticsExploreRoute

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RouteTopBar(
    route: NavKey,
    scrollBehavior: TopAppBarScrollBehavior,
    search: AppSearchState,
    searchFocusRequester: FocusRequester,
    albumTitle: String?,
    settingsTitle: String?,
    onBack: (() -> Unit)?,
    onSettingsBack: () -> Unit,
    onCloseSearch: (SearchField) -> Unit,
    onSettingsClick: () -> Unit,
) {
    val topLevelDestination = TopLevelDestination.entries.firstOrNull { it.route == route }
    when {
        route is CreatePlaylistRoute -> Unit
        route is LibraryAlbumRoute -> {
            LibraryDetailTopAppBar(
                title = albumTitle ?: route.albumTitle,
                onBack = onBack,
                containerColor = Color.Transparent,
                scrollBehavior = scrollBehavior,
            )
        }
        route is LibraryArtistRoute -> {
            val artistSearch = search.artist
            LibraryDetailTopAppBar(
                title = "",
                onBack = onBack,
                containerColor = Color.Transparent,
                supportsSearch = true,
                isSearchExpanded = artistSearch.isExpanded,
                searchQuery = artistSearch.query,
                onSearchQueryChange = { artistSearch.query = it },
                onSearchActionClick = {
                    if (artistSearch.isExpanded) artistSearch.clear() else artistSearch.isExpanded = true
                },
                searchPlaceholderRes = R.string.app_search_placeholder_artist,
                searchFocusRequester = searchFocusRequester,
                scrollBehavior = scrollBehavior,
            )
        }
        route is LibraryFolderRoute -> {
            LibraryDetailTopAppBar(
                title = route.folderName,
                onBack = onBack,
                scrollBehavior = scrollBehavior,
            )
        }
        route is PlaylistDetailRoute -> {
            LibraryDetailTopAppBar(
                title = "",
                onBack = onBack,
                containerColor = Color.Transparent,
                scrollBehavior = scrollBehavior,
            )
        }
        route == DailyPlaylistRoute -> {
            LibraryDetailTopAppBar(
                title = stringResource(R.string.app_top_bar_title_daily_playlist),
                onBack = onBack,
                scrollBehavior = scrollBehavior,
            )
        }
        route == RecentlyAddedRoute -> {
            LibraryDetailTopAppBar(
                title = stringResource(R.string.app_top_bar_title_recently_added),
                onBack = onBack,
                scrollBehavior = scrollBehavior,
            )
        }
        route == StatisticsExploreRoute -> {
            LibraryDetailTopAppBar(
                title = stringResource(R.string.app_top_bar_title_statistics_explore),
                onBack = onBack,
                scrollBehavior = scrollBehavior,
            )
        }
        topLevelDestination != null -> {
            val field = search.fieldFor(route)
            TopLevelTopAppBar(
                title = stringResource(topLevelDestination.labelRes),
                supportsSearch = field != null,
                isSearchExpanded = field?.isExpanded == true,
                searchQuery = field?.query.orEmpty(),
                searchFocusRequester = searchFocusRequester,
                onSearchQueryChange = { newValue -> field?.query = newValue },
                onSearchActionClick = {
                    if (field != null) {
                        if (field.isExpanded) onCloseSearch(field) else field.isExpanded = true
                    }
                },
                searchPlaceholderRes = searchPlaceholderRes(route),
                onSettingsClick = onSettingsClick,
                scrollBehavior = scrollBehavior,
            )
        }
        route == SettingsRoute -> {
            SettingsTopAppBar(
                title = settingsTitle ?: stringResource(R.string.app_top_bar_title_settings),
                onBack = onSettingsBack,
                scrollBehavior = scrollBehavior,
            )
        }
    }
}

private fun searchPlaceholderRes(route: NavKey): Int = when (route) {
    HomeRoute -> R.string.app_search_placeholder_home
    LibraryRoute -> R.string.app_search_placeholder_library
    PlaylistsRoute -> R.string.app_search_placeholder_playlists
    else -> R.string.app_search_placeholder_default
}
