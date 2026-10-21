package com.catlytics.app.ui

import android.Manifest
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.navigation.BackNavigationBehavior
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.material3.adaptive.navigationsuite.rememberNavigationSuiteScaffoldState
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.navigation3.runtime.NavEntry
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.union
import com.catlytics.app.ui.chrome.CatlyticsNavigationSuiteScaffold
import com.catlytics.core.designsystem.layout.LocalPaneChrome
import com.catlytics.core.designsystem.layout.PaneChrome
import androidx.compose.foundation.background
import androidx.navigation3.scene.SinglePaneSceneStrategy
import com.catlytics.core.navigation.findListDetailIndices
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.metadata
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.ui.NavDisplay
import com.catlytics.app.R
import com.catlytics.app.navigation.TopLevelDestination
import com.catlytics.app.playback.NowPlayingRoute
import com.catlytics.app.playback.NowPlayingScreen
import com.catlytics.app.playback.PlaybackArtwork
import com.catlytics.app.playback.PlaybackViewModel
import com.catlytics.app.playback.PrefetchAdjacentPlaybackArtwork
import com.catlytics.app.playback.shareTrack
import com.catlytics.app.ui.chrome.LibraryDetailTopAppBar
import com.catlytics.app.ui.chrome.SettingsTopAppBar
import com.catlytics.app.ui.chrome.StatusBarProtection
import com.catlytics.app.ui.chrome.TopLevelTopAppBar
import com.catlytics.app.ui.sheet.CatlyticsAppSheets
import com.catlytics.app.ui.sheet.TrackOptionsRequest
import com.catlytics.core.designsystem.component.CatlyticsMiniPlayer
import com.catlytics.core.designsystem.component.extractArtworkAccentColor
import com.catlytics.core.designsystem.text.displayAlbumTitle
import com.catlytics.core.designsystem.text.displayArtistName
import com.catlytics.core.designsystem.text.displayTrackTitle
import com.catlytics.core.domain.usecase.playlist.ToggleLikedTrackResult
import com.catlytics.core.model.LIKED_PLAYLIST_ID
import com.catlytics.core.model.PlaybackStatus
import com.catlytics.core.model.PlaylistSource
import com.catlytics.core.model.Track
import com.catlytics.core.model.TrackSelectionAction
import com.catlytics.core.navigation.rememberTopLevelBackStack
import com.catlytics.core.navigation.navigationBackTransition
import com.catlytics.core.navigation.navigationForwardTransition
import com.catlytics.core.navigation.verticalSheetEnterTransition
import com.catlytics.core.navigation.verticalSheetExitTransition
import com.catlytics.feature.home.api.HomeRoute
import com.catlytics.feature.home.api.DailyPlaylistRoute
import com.catlytics.feature.home.api.RecentlyAddedRoute
import com.catlytics.feature.home.impl.homeEntry
import com.catlytics.feature.library.api.LibraryRoute
import com.catlytics.feature.library.api.LibraryAlbumRoute
import com.catlytics.feature.library.api.LibraryArtistRoute
import com.catlytics.feature.library.api.LibraryFolderRoute
import com.catlytics.feature.library.impl.navigation.libraryEntry
import com.catlytics.feature.playlists.api.CreatePlaylistRoute
import com.catlytics.feature.playlists.api.PlaylistDetailRoute
import com.catlytics.feature.playlists.api.PlaylistsRoute
import com.catlytics.feature.playlists.impl.R as PlaylistsR
import com.catlytics.feature.playlists.impl.playlistsEntry
import com.catlytics.feature.settings.api.SettingsRoute
import com.catlytics.feature.settings.impl.settingsEntry
import com.catlytics.feature.statistics.impl.statisticsEntry
import com.catlytics.feature.statistics.api.StatisticsExploreRoute
import com.catlytics.feature.statistics.api.StatisticsRoute
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun CatlyticsApp(
    modifier: Modifier = Modifier,
    playbackViewModel: PlaybackViewModel = hiltViewModel(),
    startupViewModel: AppStartupViewModel = hiltViewModel(),
    deepLinkUri: Uri? = null,
    onDeepLinkHandled: () -> Unit = {},
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val likedPlaylistName = stringResource(R.string.app_playlist_liked_name)
    val audioPermission = remember { requiredAudioPermission() }
    var hasAudioPermission by remember(audioPermission) {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                audioPermission,
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED,
        )
    }
    var isHomeContentReady by remember { mutableStateOf(false) }
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        if (isGranted) {
            isHomeContentReady = false
        }
        hasAudioPermission = isGranted
    }
    val startupUiState by startupViewModel.uiState.collectAsStateWithLifecycle()
    val startupError = (startupUiState as? AppStartupUiState.Error)?.message
    val isLibraryRefreshRunning = hasAudioPermission && (
        startupUiState == AppStartupUiState.WaitingForPermission ||
            startupUiState == AppStartupUiState.Loading
    )
    LaunchedEffect(hasAudioPermission) {
        startupViewModel.onAudioPermissionState(hasAudioPermission)
    }

    val layoutDirection = LocalLayoutDirection.current
    val statusBarHeight = WindowInsets.statusBars
        .asPaddingValues()
        .calculateTopPadding()
    val topLevelBackStack = rememberTopLevelBackStack(HomeRoute)
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val searchFocusRequester = remember { FocusRequester() }
    var isHomeSearchExpanded by rememberSaveable { mutableStateOf(false) }
    var homeSearchQuery by rememberSaveable { mutableStateOf("") }
    var isLibrarySearchExpanded by rememberSaveable { mutableStateOf(false) }
    var librarySearchQuery by rememberSaveable { mutableStateOf("") }
    var isPlaylistsSearchExpanded by rememberSaveable { mutableStateOf(false) }
    var playlistsSearchQuery by rememberSaveable { mutableStateOf("") }
    var isArtistSearchExpanded by rememberSaveable { mutableStateOf(false) }
    var artistSearchQuery by rememberSaveable { mutableStateOf("") }
    var playlistSource by remember { mutableStateOf<PlaylistSource?>(null) }
    var playlistSheetSession by remember { mutableIntStateOf(0) }
    var trackOptionsRequest by remember { mutableStateOf<TrackOptionsRequest?>(null) }
    var editMetadataTrackId by rememberSaveable { mutableStateOf<String?>(null) }
    var bulkEditTrackIds by remember { mutableStateOf<List<String>?>(null) }
    var detailTopBarColors by remember { mutableStateOf<Map<NavKey, Color>>(emptyMap()) }
    var detailTopBarTitles by remember { mutableStateOf<Map<NavKey, String>>(emptyMap()) }
    var settingsTopBarTitle by remember { mutableStateOf<String?>(null) }
    var settingsTopBarBackAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    val hazeState = rememberHazeState()
    val coroutineScope = rememberCoroutineScope()
    var miniPlayerAccent by remember { mutableStateOf<Pair<String?, Color>?>(null) }
    val appVersion = remember(context) {
        context.packageManager
            .getPackageInfo(context.packageName, 0)
            .versionName
            .orEmpty()
    }
    val playbackState by playbackViewModel.playbackState.collectAsStateWithLifecycle()
    val isCurrentTrackLiked by playbackViewModel.isCurrentTrackLiked.collectAsStateWithLifecycle()
    val likedTrackIds by playbackViewModel.likedTrackIds.collectAsStateWithLifecycle()
    val queueSourcePlaylist by playbackViewModel.queueSourcePlaylist.collectAsStateWithLifecycle()
    val sleepTimerState by playbackViewModel.sleepTimerState.collectAsStateWithLifecycle()
    val currentRoute = topLevelBackStack.backStack.lastOrNull()
    val windowAdaptiveInfo = currentWindowAdaptiveInfoV2()
    val navigationSuiteType = NavigationSuiteScaffoldDefaults.navigationSuiteType(windowAdaptiveInfo)
    val isNavigationRail = navigationSuiteType == NavigationSuiteType.WideNavigationRailCollapsed ||
        navigationSuiteType == NavigationSuiteType.WideNavigationRailExpanded
    // Sin espacio entre paneles: cada uno pinta su propio fondo de borde a borde.
    val paneScaffoldDirective = remember(windowAdaptiveInfo) {
        calculatePaneScaffoldDirective(windowAdaptiveInfo).copy(horizontalPartitionSpacerSize = 0.dp)
    }
    val isTwoPane = paneScaffoldDirective.maxHorizontalPartitions > 1
    val currentIsTwoPane by rememberUpdatedState(isTwoPane)
    // Lista y detalle visibles a la vez en ventanas anchas (ver ListDetailSceneStrategy).
    val listDetailIndices = if (isTwoPane) {
        findListDetailIndices(
            items = topLevelBackStack.backStack,
            isList = ::isListPaneRoute,
            isDetail = ::isDetailPaneRoute,
        )
    } else {
        null
    }
    val listPaneRoute = listDetailIndices?.let { topLevelBackStack.backStack[it.listIndex] }
    val detailPaneRoute = listDetailIndices?.detailIndex?.let { topLevelBackStack.backStack[it] }
    // Ruta cuya lista sigue en pantalla: en dos paneles es la del panel izquierdo.
    val visibleListRoute = listPaneRoute ?: currentRoute
    val listPaneSearchFocusRequester = remember { FocusRequester() }
    val isWaitingForHomeContent = hasAudioPermission &&
        !isLibraryRefreshRunning &&
        currentRoute == HomeRoute &&
        !isHomeContentReady
    val showStartupLoading = isLibraryRefreshRunning || isWaitingForHomeContent
    val isOnHomeRoot = currentRoute == HomeRoute
    val isOnLibraryRoot = currentRoute == LibraryRoute
    val isOnPlaylistsRoot = currentRoute == PlaylistsRoute
    val currentTopLevelDestination = TopLevelDestination.entries
        .firstOrNull { it.route == currentRoute }
    val selectedTopLevelDestination = when (currentRoute) {
        is LibraryAlbumRoute, is LibraryArtistRoute, is LibraryFolderRoute ->
            TopLevelDestination.Library
        is PlaylistDetailRoute, is CreatePlaylistRoute -> TopLevelDestination.Playlists
        DailyPlaylistRoute, RecentlyAddedRoute -> TopLevelDestination.Home
        StatisticsExploreRoute -> TopLevelDestination.Statistics
        else -> currentTopLevelDestination
    }
    val isNowPlayingVisible = currentRoute == NowPlayingRoute
    val isCreatePlaylistVisible = currentRoute is CreatePlaylistRoute
    val isFullScreenRoute = isNowPlayingVisible || isCreatePlaylistVisible
    // En pantallas grandes la navegación nunca se oculta (tampoco en pantallas completas).
    val isNavigationVisible = isNavigationRail ||
        (!isFullScreenRoute && selectedTopLevelDestination != null)
    val isNavigationBarShown = isNavigationVisible && !isNavigationRail
    fun isSearchExpanded(route: NavKey): Boolean = when (route) {
        HomeRoute -> isHomeSearchExpanded
        LibraryRoute -> isLibrarySearchExpanded
        PlaylistsRoute -> isPlaylistsSearchExpanded
        is LibraryArtistRoute -> isArtistSearchExpanded
        else -> false
    }
    fun paneChromeColor(route: NavKey): Color? = when (route) {
        is LibraryAlbumRoute, is LibraryArtistRoute, is PlaylistDetailRoute ->
            detailTopBarColors[route]
        else -> null
    }
    LaunchedEffect(currentRoute) {
        isArtistSearchExpanded = false
        artistSearchQuery = ""
        if (currentRoute != SettingsRoute) {
            settingsTopBarTitle = null
            settingsTopBarBackAction = null
        }
    }

    fun closeHomeSearch() {
        homeSearchQuery = ""
        isHomeSearchExpanded = false
        focusManager.clearFocus()
        keyboardController?.hide()
    }

    fun closeLibrarySearch() {
        librarySearchQuery = ""
        isLibrarySearchExpanded = false
        focusManager.clearFocus()
        keyboardController?.hide()
    }

    fun closePlaylistsSearch() {
        playlistsSearchQuery = ""
        isPlaylistsSearchExpanded = false
        focusManager.clearFocus()
        keyboardController?.hide()
    }

    fun openSettings() {
        if (topLevelBackStack.backStack.lastOrNull() != SettingsRoute) {
            topLevelBackStack.add(SettingsRoute)
        }
    }

    fun openTrackOptions(
        track: Track,
        onRemoveFromPlaylist: (() -> Unit)? = null,
    ) {
        trackOptionsRequest = TrackOptionsRequest(
            track = track,
            onRemoveFromPlaylist = onRemoveFromPlaylist,
        )
    }

    fun openAddToPlaylist(source: PlaylistSource) {
        playlistSheetSession++
        playlistSource = source
    }

    fun navigateToAlbum(track: Track) {
        val albumId = track.albumId ?: return
        val albumTitle = track.albumTitle ?: return
        trackOptionsRequest = null
        topLevelBackStack.addTopLevel(LibraryRoute)
        topLevelBackStack.add(LibraryAlbumRoute(albumId, resources.displayAlbumTitle(albumTitle)))
    }

    fun navigateToArtist(track: Track) {
        trackOptionsRequest = null
        topLevelBackStack.addTopLevel(LibraryRoute)
        topLevelBackStack.add(
            LibraryArtistRoute(
                track.artist.id,
                resources.displayArtistName(track.artist.name),
            ),
        )
    }

    fun canAddTrackToQueue(track: Track): Boolean =
        playbackState.currentTrack?.id?.let { currentTrackId ->
            track.id != currentTrackId
        } ?: false

    fun toggleTrackLikedWithToast(trackId: String) {
        playbackViewModel.toggleTrackLiked(trackId) { result ->
            val likedPlaylistName = resources.getString(R.string.app_playlist_liked_name)
            Toast.makeText(
                context,
                when (result) {
                    ToggleLikedTrackResult.Added ->
                        resources.getString(R.string.app_toast_track_added_to_liked, likedPlaylistName)
                    ToggleLikedTrackResult.Removed ->
                        resources.getString(R.string.app_toast_track_removed_from_liked, likedPlaylistName)
                },
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    fun addTrackToQueueWithToast(track: Track) {
        playbackViewModel.addQueueItem(track) {
            Toast.makeText(
                context,
                resources.getString(R.string.app_toast_added_to_queue),
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    fun playNextTrackWithToast(track: Track) {
        playbackViewModel.playNext(track) {
            Toast.makeText(
                context,
                resources.getString(R.string.app_toast_will_play_next),
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    fun handleTrackSelectionAction(action: TrackSelectionAction) {
        when (action) {
            is TrackSelectionAction.AddToPlaylist -> openAddToPlaylist(
                PlaylistSource.TrackCollectionSource(
                    title = if (action.tracks.size == 1) {
                        action.tracks.first().title
                    } else {
                        resources.getString(R.string.app_track_collection_selected_title)
                    },
                    artworkUri = action.tracks.firstNotNullOfOrNull(Track::artworkUri),
                    trackIds = action.tracks.map(Track::id),
                ),
            )
            is TrackSelectionAction.Like -> playbackViewModel.likeTracks(action.tracks.map(Track::id)) { added ->
                val likedPlaylistName = resources.getString(R.string.app_playlist_liked_name)
                Toast.makeText(
                    context,
                    resources.getQuantityString(
                        R.plurals.app_toast_tracks_added_to_liked,
                        added,
                        added,
                        likedPlaylistName,
                    ),
                    Toast.LENGTH_SHORT,
                ).show()
            }
            is TrackSelectionAction.Unlike -> playbackViewModel.unlikeTracks(action.tracks.map(Track::id)) { removed ->
                val likedPlaylistName = resources.getString(R.string.app_playlist_liked_name)
                Toast.makeText(
                    context,
                    resources.getQuantityString(
                        R.plurals.app_toast_tracks_removed_from_liked,
                        removed,
                        removed,
                        likedPlaylistName,
                    ),
                    Toast.LENGTH_SHORT,
                ).show()
            }
            is TrackSelectionAction.AddToQueue -> playbackViewModel.addQueueItems(action.tracks) { added ->
                Toast.makeText(
                    context,
                    resources.getQuantityString(
                        R.plurals.app_toast_tracks_added_to_queue,
                        added,
                        added,
                    ),
                    Toast.LENGTH_SHORT,
                ).show()
            }
            is TrackSelectionAction.EditMetadata -> {
                val trackIds = action.tracks.map(Track::id)
                // A single track gets the full editor, which also covers its title.
                if (trackIds.size == 1) editMetadataTrackId = trackIds.single() else bulkEditTrackIds = trackIds
            }
            is TrackSelectionAction.PlayNext -> playbackViewModel.playNextTracks(action.tracks) { added ->
                Toast.makeText(
                    context,
                    resources.getQuantityString(
                        R.plurals.app_toast_tracks_will_play_next,
                        added,
                        added,
                    ),
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }
    }

    BackHandler(enabled = (isOnHomeRoot && isHomeSearchExpanded) || (isOnLibraryRoot && isLibrarySearchExpanded) || (isOnPlaylistsRoot && isPlaylistsSearchExpanded) || isArtistSearchExpanded) {
        if (isOnHomeRoot && isHomeSearchExpanded) {
            closeHomeSearch()
        } else if (isOnLibraryRoot && isLibrarySearchExpanded) {
            closeLibrarySearch()
        } else if (isOnPlaylistsRoot && isPlaylistsSearchExpanded) {
            closePlaylistsSearch()
        } else if (isArtistSearchExpanded) {
            artistSearchQuery = ""
            isArtistSearchExpanded = false
        }
    }

    LaunchedEffect(isHomeSearchExpanded, isLibrarySearchExpanded, isPlaylistsSearchExpanded, isArtistSearchExpanded) {
        if (isArtistSearchExpanded && listPaneRoute != null) {
            searchFocusRequester.requestFocus()
            keyboardController?.show()
        } else if (isLibrarySearchExpanded && listPaneRoute != null) {
            listPaneSearchFocusRequester.requestFocus()
            keyboardController?.show()
        } else if (isHomeSearchExpanded || isLibrarySearchExpanded || isPlaylistsSearchExpanded || isArtistSearchExpanded) {
            searchFocusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    LaunchedEffect(visibleListRoute) {
        if (visibleListRoute != HomeRoute && isHomeSearchExpanded) {
            closeHomeSearch()
        }
        if (visibleListRoute != LibraryRoute && isLibrarySearchExpanded) {
            closeLibrarySearch()
        }
        if (visibleListRoute != PlaylistsRoute && isPlaylistsSearchExpanded) {
            closePlaylistsSearch()
        }
    }

    LaunchedEffect(deepLinkUri) {
        if (deepLinkUri != null) {
            if (deepLinkUri.scheme == "catlytics" && deepLinkUri.host == "nowplaying") {
                if (topLevelBackStack.backStack.lastOrNull() != NowPlayingRoute) {
                    topLevelBackStack.add(NowPlayingRoute)
                }
            }
            onDeepLinkHandled()
        }
    }

    fun closeCurrentDestination() {
        topLevelBackStack.removeLast()
    }

    // Al elegir otro elemento en la lista con un detalle ya abierto al lado, ese detalle se
    // reemplaza en vez de acumularse; así atrás vuelve a la lista. Lee el estado al momento del
    // toque porque las pantallas pueden conservar este callback entre recomposiciones.
    fun openFromListPane(key: NavKey) {
        val backStack = topLevelBackStack.backStack
        val indices = if (currentIsTwoPane) {
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
            detailIndex == null -> topLevelBackStack.add(key)
            backStack[detailIndex] == key -> Unit
            else -> topLevelBackStack.replaceAbove(anchor = backStack[indices.listIndex], key = key)
        }
    }

    BackHandler(enabled = showStartupLoading) {}
    PrefetchAdjacentPlaybackArtwork(
        queue = playbackState.queue,
        currentIndex = playbackState.currentIndex,
    )
    // Barra superior de cada pantalla, con su propio estado de scroll. En lista-detalle el detalle
    // no muestra la flecha atrás: la lista sigue visible al lado.
    val routeTopBar: @Composable (NavKey, TopAppBarScrollBehavior) -> Unit = { route, scrollBehavior ->
        val routeTopLevelDestination = TopLevelDestination.entries.firstOrNull { it.route == route }
        val onBack: (() -> Unit)? = if (isTwoPane && route == detailPaneRoute) {
            null
        } else {
            ::closeCurrentDestination
        }
        val routeSearchFocusRequester = if (route == listPaneRoute) {
            listPaneSearchFocusRequester
        } else {
            searchFocusRequester
        }
        when {
            route is CreatePlaylistRoute -> Unit
            route is LibraryAlbumRoute -> {
                LibraryDetailTopAppBar(
                    title = detailTopBarTitles[route] ?: route.albumTitle,
                    onBack = onBack,
                    containerColor = Color.Transparent,
                    scrollBehavior = scrollBehavior,
                )
            }
            route is LibraryArtistRoute -> {
                LibraryDetailTopAppBar(
                    title = "",
                    onBack = onBack,
                    containerColor = Color.Transparent,
                    supportsSearch = true,
                    isSearchExpanded = isArtistSearchExpanded,
                    searchQuery = artistSearchQuery,
                    onSearchQueryChange = { artistSearchQuery = it },
                    onSearchActionClick = {
                        if (isArtistSearchExpanded) {
                            artistSearchQuery = ""
                            isArtistSearchExpanded = false
                        } else {
                            isArtistSearchExpanded = true
                        }
                    },
                    searchPlaceholderRes = R.string.app_search_placeholder_artist,
                    searchFocusRequester = routeSearchFocusRequester,
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
            routeTopLevelDestination != null -> {
                val supportsSearch = route == HomeRoute || route == LibraryRoute || route == PlaylistsRoute
                val isSearchExpanded = when {
                    route == HomeRoute -> isHomeSearchExpanded
                    route == LibraryRoute -> isLibrarySearchExpanded
                    route == PlaylistsRoute -> isPlaylistsSearchExpanded
                    else -> false
                }
                val currentSearchQuery = when {
                    route == HomeRoute -> homeSearchQuery
                    route == LibraryRoute -> librarySearchQuery
                    route == PlaylistsRoute -> playlistsSearchQuery
                    else -> ""
                }
                val searchPlaceholderRes = when {
                    route == HomeRoute -> R.string.app_search_placeholder_home
                    route == LibraryRoute -> R.string.app_search_placeholder_library
                    route == PlaylistsRoute -> R.string.app_search_placeholder_playlists
                    else -> R.string.app_search_placeholder_default
                }

                TopLevelTopAppBar(
                    title = stringResource(routeTopLevelDestination.labelRes),
                    supportsSearch = supportsSearch,
                    isSearchExpanded = isSearchExpanded,
                    searchQuery = currentSearchQuery,
                    searchFocusRequester = routeSearchFocusRequester,
                    onSearchQueryChange = { newValue ->
                        if (route == HomeRoute) {
                            homeSearchQuery = newValue
                        } else if (route == LibraryRoute) {
                            librarySearchQuery = newValue
                        } else if (route == PlaylistsRoute) {
                            playlistsSearchQuery = newValue
                        }
                    },
                    onSearchActionClick = {
                        if (route == HomeRoute) {
                            if (isHomeSearchExpanded) {
                                closeHomeSearch()
                            } else {
                                isHomeSearchExpanded = true
                            }
                        } else if (route == LibraryRoute) {
                            if (isLibrarySearchExpanded) {
                                closeLibrarySearch()
                            } else {
                                isLibrarySearchExpanded = true
                            }
                        } else if (route == PlaylistsRoute) {
                            if (isPlaylistsSearchExpanded) {
                                closePlaylistsSearch()
                            } else {
                                isPlaylistsSearchExpanded = true
                            }
                        }
                    },
                    searchPlaceholderRes = searchPlaceholderRes,
                    onSettingsClick = ::openSettings,
                    scrollBehavior = scrollBehavior,
                )
            }
            route == SettingsRoute -> {
                SettingsTopAppBar(
                    title = settingsTopBarTitle ?: stringResource(R.string.app_top_bar_title_settings),
                    onBack = settingsTopBarBackAction ?: ::closeCurrentDestination,
                    scrollBehavior = scrollBehavior,
                )
            }
        }
    }

    val statusBarProtectionHeight = statusBarHeight
    // Cada pantalla se envuelve en su propio Scaffold con su barra superior y su estado de scroll,
    // así funcionan igual en un panel o lado a lado en lista-detalle.
    val routePane: @Composable (NavKey, @Composable () -> Unit) -> Unit = { route, content ->
        val hasTopBar = route != NowPlayingRoute && route !is CreatePlaylistRoute
        if (hasTopBar) {
            val isRouteSearchExpanded = isSearchExpanded(route)
            RoutePane(
                containerColor = paneChromeColor(route) ?: MaterialTheme.colorScheme.background,
                statusBarHeight = statusBarProtectionHeight,
                canScroll = !isRouteSearchExpanded,
                scrollResetKey = if (route == SettingsRoute) {
                    settingsTopBarTitle to isRouteSearchExpanded
                } else {
                    isRouteSearchExpanded
                },
                showPaneDivider = isTwoPane && route == detailPaneRoute,
                topBar = { scrollBehavior -> routeTopBar(route, scrollBehavior) },
                content = content,
            )
        } else {
            content()
        }
    }
    val currentRoutePane by rememberUpdatedState(routePane)
    fun withRoutePanes(
        provider: (NavKey) -> NavEntry<NavKey>,
    ): (NavKey) -> NavEntry<NavKey> = { key ->
        val entry = provider(key)
        NavEntry(navEntry = entry, content = { route: NavKey ->
            currentRoutePane(route) { entry.Content() }
        })
    }

    val listDetailSceneStrategy = rememberListDetailSceneStrategy<NavKey>(
        // Atrás quita solo el detalle y vuelve a la lista con el placeholder: con el
        // comportamiento por defecto seguiría quitando entradas hasta cambiar de pestaña.
        backNavigationBehavior = BackNavigationBehavior.PopLatest,
        directive = paneScaffoldDirective,
    )
    val navigationSuiteState = rememberNavigationSuiteScaffoldState()
    LaunchedEffect(isNavigationVisible) {
        if (isNavigationVisible) navigationSuiteState.show() else navigationSuiteState.hide()
    }

    StartupLoadingGate(
        isLoading = showStartupLoading,
        composeContent = !isLibraryRefreshRunning,
        modifier = modifier.fillMaxSize(),
    ) {
        CatlyticsNavigationSuiteScaffold(
            selectedRoute = topLevelBackStack.topLevelKey,
            onDestinationSelected = topLevelBackStack::addTopLevel,
            navigationSuiteType = navigationSuiteType,
            state = navigationSuiteState,
            modifier = Modifier.fillMaxSize(),
        ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onBackground,
        bottomBar = {
            if (!isFullScreenRoute) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    playbackState.currentTrack?.let { track ->
                        CatlyticsMiniPlayer(
                            // Centrado y con ancho acotado en pantallas anchas. Sin barra inferior
                            // queda sobre la barra de gestos del sistema (si la barra está visible,
                            // el NavigationSuiteScaffold ya consumió ese inset).
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .widthIn(max = MINI_PLAYER_MAX_WIDTH)
                                .windowInsetsPadding(
                                    WindowInsets.navigationBars.only(WindowInsetsSides.Bottom),
                                )
                                .padding(bottom = if (isNavigationBarShown) 0.dp else 8.dp),
                            title = track.title.displayTrackTitle(),
                            artist = track.artist.name.displayArtistName(),
                            isPlaying = playbackState.status == PlaybackStatus.Playing,
                            isBuffering = playbackState.status == PlaybackStatus.Buffering,
                            progress = if (playbackState.durationMillis > 0L) {
                                (playbackState.positionMillis.toFloat() / playbackState.durationMillis)
                                    .coerceIn(0f, 1f)
                            } else {
                                0f
                            },
                            onTogglePlayback = playbackViewModel::togglePlayback,
                            onSkipPrevious = playbackViewModel::skipPrevious,
                            onSkipNext = playbackViewModel::skipNext,
                            onClick = {
                                if (topLevelBackStack.backStack.lastOrNull() != NowPlayingRoute) {
                                    topLevelBackStack.add(NowPlayingRoute)
                                }
                            },
                            hazeState = hazeState,
                            accentColor = miniPlayerAccent
                                ?.takeIf { (uri, _) -> uri == track.artworkUri && uri != null }
                                ?.second
                                ?: Color.Unspecified,
                            artwork = { artworkModifier ->
                                PlaybackArtwork(
                                    artworkUri = track.artworkUri,
                                    seed = track.title,
                                    contentDescription = stringResource(
                                        R.string.app_content_description_artwork,
                                        track.title.displayTrackTitle(),
                                    ),
                                    modifier = artworkModifier,
                                    onSuccess = { bitmap ->
                                        val artworkUri = track.artworkUri
                                        coroutineScope.launch {
                                            miniPlayerAccent = artworkUri to bitmap.extractArtworkAccentColor()
                                        }
                                    },
                                )
                            },
                        )
                    }

                }
            }
        },
    ) { innerPadding ->
        // Las pantallas se dibujan detrás del mini reproductor y reciben su alto como padding.
        // Se congela mientras hay una ruta a pantalla completa para que la pantalla de abajo no
        // salte durante la transición.
        var lastBottomPadding by remember { mutableStateOf(0.dp) }
        val currentBottomPadding = innerPadding.calculateBottomPadding()
        SideEffect {
            if (!isFullScreenRoute) {
                lastBottomPadding = currentBottomPadding
            }
        }
        val bottomPaddingToUse = if (isFullScreenRoute) lastBottomPadding else currentBottomPadding
        val bottomPaddingState = rememberUpdatedState(bottomPaddingToUse)

        Box(modifier = Modifier.fillMaxSize()) {
            NavDisplay(
                modifier = Modifier
                    .fillMaxSize()
                    .hazeSource(hazeState)
                    .background(MaterialTheme.colorScheme.background),
                backStack = topLevelBackStack.backStack,
                onBack = ::closeCurrentDestination,
                sceneStrategies = listOf(listDetailSceneStrategy, SinglePaneSceneStrategy()),

                transitionSpec = {
                    navigationForwardTransition()
                },
                popTransitionSpec = {
                    navigationBackTransition()
                },
                predictivePopTransitionSpec = {
                    navigationBackTransition()
                },
                entryProvider = withRoutePanes(entryProvider {
                    homeEntry(
                        searchQuery = { homeSearchQuery },
                        onTrackOptions = { track -> openTrackOptions(track) },
                        likedTrackIds = { likedTrackIds },
                        onTrackSelectionAction = ::handleTrackSelectionAction,
                        onNavigateToStatistics = {
                            topLevelBackStack.addTopLevel(StatisticsRoute)
                        },
                        onNavigateToDailyPlaylist = {
                            topLevelBackStack.add(DailyPlaylistRoute)
                        },
                        onNavigateToRecentlyAdded = {
                            topLevelBackStack.add(RecentlyAddedRoute)
                        },
                        onNavigateToFavorites = {
                            topLevelBackStack.addTopLevel(PlaylistsRoute)
                            topLevelBackStack.add(
                                PlaylistDetailRoute(
                                    playlistId = LIKED_PLAYLIST_ID,
                                    playlistName = likedPlaylistName,
                                ),
                            )
                        },
                        hasAudioPermission = { hasAudioPermission },
                        onRequestAudioPermission = {
                            audioPermissionLauncher.launch(audioPermission)
                        },
                        startupError = { startupError },
                        onContentReady = { isHomeContentReady = true },
                        bottomPadding = { bottomPaddingState.value },
                    )
                    libraryEntry(
                        searchQuery = { librarySearchQuery },
                        artistSearchQuery = { artistSearchQuery },
                        onArtistSearchQueryChange = { artistSearchQuery = it },
                        onDestinationSelected = topLevelBackStack::add,
                        onListDestinationSelected = ::openFromListPane,
                        onAddToPlaylist = ::openAddToPlaylist,
                        onTrackOptions = { track -> openTrackOptions(track) },
                        likedTrackIds = { likedTrackIds },
                        currentTrackId = { playbackState.currentTrack?.id },
                        isCurrentTrackPlaying = {
                            playbackState.status == PlaybackStatus.Playing
                        },
                        onTrackSelectionAction = ::handleTrackSelectionAction,
                        onLibraryDetailTopBarColorChange = { route, color ->
                            detailTopBarColors = detailTopBarColors + (route to color)
                        },
                        onLibraryDetailTitleChange = { route, title ->
                            detailTopBarTitles = detailTopBarTitles + (route to title)
                        },
                        hasAudioPermission = { hasAudioPermission },
                        onRequestAudioPermission = {
                            audioPermissionLauncher.launch(audioPermission)
                        },
                        bottomPadding = { bottomPaddingState.value },
                    )
                    playlistsEntry(
                        searchQuery = { playlistsSearchQuery },
                        onDestinationSelected = topLevelBackStack::add,
                        onListDestinationSelected = ::openFromListPane,
                        onPlaylistDeleted = ::closeCurrentDestination,
                        onTrackOptions = { track, onRemoveFromPlaylist ->
                            openTrackOptions(track, onRemoveFromPlaylist)
                        },
                        likedTrackIds = { likedTrackIds },
                        onTrackSelectionAction = ::handleTrackSelectionAction,
                        bottomPadding = { bottomPaddingState.value },
                        onPlaylistDetailTopBarColorChange = { route, color ->
                            detailTopBarColors = detailTopBarColors + (route to color)
                        },
                        onCloseCreatePlaylist = ::closeCurrentDestination,
                        onPlaylistCreated = { route, playlist ->
                            closeCurrentDestination()
                            if (route.openDetailOnCreate) {
                                topLevelBackStack.add(
                                    PlaylistDetailRoute(
                                        playlistId = playlist.id,
                                        playlistName = playlist.name,
                                    ),
                                )
                            } else {
                                Toast.makeText(
                                    context,
                                    resources.getQuantityString(
                                        PlaylistsR.plurals.create_playlist_toast_created,
                                        playlist.trackIds.size,
                                        playlist.trackIds.size,
                                        playlist.name,
                                    ),
                                    Toast.LENGTH_SHORT,
                                ).show()
                            }
                        },
                    )
                    settingsEntry(
                        appVersion = appVersion,
                        librariesRes = R.raw.aboutlibraries,
                        hasAudioPermission = { hasAudioPermission },
                        onRequestAudioPermission = {
                            audioPermissionLauncher.launch(audioPermission)
                        },
                        bottomPadding = { bottomPaddingState.value },
                        onTopBarTitleChange = { title -> settingsTopBarTitle = title },
                        onTopBarBackActionChange = { action -> settingsTopBarBackAction = action },
                    )
                    statisticsEntry(
                        bottomPadding = { bottomPaddingState.value },
                        onNavigateToExplore = {
                            if (topLevelBackStack.backStack.lastOrNull() != StatisticsExploreRoute) {
                                topLevelBackStack.add(StatisticsExploreRoute)
                            }
                        },
                    )
                    entry<NowPlayingRoute>(
                        metadata = metadata {
                            put(NavDisplay.TransitionKey) {
                                verticalSheetEnterTransition()
                            }
                            put(NavDisplay.PopTransitionKey) {
                                verticalSheetExitTransition()
                            }
                            put(NavDisplay.PredictivePopTransitionKey) { _ ->
                                verticalSheetExitTransition()
                            }
                        },
                    ) {
                        NowPlayingScreen(
                            playbackState = playbackState,
                            onBack = ::closeCurrentDestination,
                            onTogglePlayback = playbackViewModel::togglePlayback,
                            onSkipPrevious = playbackViewModel::skipPrevious,
                            onSkipNext = playbackViewModel::skipNext,
                            onSeekTo = playbackViewModel::seekTo,
                            onSeekBackward10Seconds = playbackViewModel::seekBackward10Seconds,
                            onSeekForward10Seconds = playbackViewModel::seekForward10Seconds,
                            onToggleShuffle = playbackViewModel::toggleShuffle,
                            onCycleRepeatMode = playbackViewModel::cycleRepeatMode,
                            onShareTrack = context::shareTrack,
                            onPlayQueueItem = playbackViewModel::playQueueItem,
                            onMoveQueueItem = playbackViewModel::moveQueueItem,
                            onRemoveQueueItem = playbackViewModel::removeQueueItem,
                            onTrackOptions = { track -> openTrackOptions(track) },
                            canAddCurrentTrackToQueue = playbackState.currentTrack
                                ?.let(::canAddTrackToQueue)
                                ?: false,
                            onAddCurrentTrackToPlaylist = {
                                playbackState.currentTrack?.let { track ->
                                    openAddToPlaylist(PlaylistSource.TrackSource(track.id))
                                }
                            },
                            onToggleCurrentTrackLikedFromOptions = {
                                playbackState.currentTrack?.let { track ->
                                    toggleTrackLikedWithToast(track.id)
                                }
                            },
                            onPlayNextCurrentTrack = {
                                playbackState.currentTrack?.let(::playNextTrackWithToast)
                            },
                            onAddCurrentTrackToQueue = {
                                playbackState.currentTrack?.let(::addTrackToQueueWithToast)
                            },
                            onGoToCurrentTrackAlbum = {
                                playbackState.currentTrack?.let(::navigateToAlbum)
                            },
                            onGoToCurrentTrackArtist = {
                                playbackState.currentTrack?.let(::navigateToArtist)
                            },
                            onEditCurrentTrackMetadata = {
                                editMetadataTrackId = playbackState.currentTrack?.id
                            },
                            isCurrentTrackLiked = isCurrentTrackLiked,
                            queueSourceTitle = queueSourcePlaylist?.let { (id, name) ->
                                if (id == LIKED_PLAYLIST_ID) {
                                    stringResource(R.string.app_playlist_liked_name)
                                } else {
                                    name
                                }
                            } ?: stringResource(R.string.app_now_playing_from_library),
                            sleepTimerState = sleepTimerState,
                            onStartSleepTimer = playbackViewModel::startSleepTimer,
                            onCancelSleepTimer = playbackViewModel::cancelSleepTimer,
                            onAddCurrentTrackToLiked = {
                                playbackViewModel.toggleCurrentTrackLiked { result ->
                                    val likedPlaylistName =
                                        resources.getString(R.string.app_playlist_liked_name)
                                    Toast.makeText(
                                        context,
                                        when (result) {
                                            ToggleLikedTrackResult.Added -> resources.getString(
                                                R.string.app_toast_track_added_to_liked,
                                                likedPlaylistName,
                                            )
                                            ToggleLikedTrackResult.Removed -> resources.getString(
                                                R.string.app_toast_track_removed_from_liked,
                                                likedPlaylistName,
                                            )
                                        },
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                }
                            },
                        )
                    }
                }),
            )

        }
    }
        }
        CatlyticsAppSheets(
            trackOptionsRequest = trackOptionsRequest,
            likedTrackIds = likedTrackIds,
            canAddTrackToQueue = ::canAddTrackToQueue,
            onDismissTrackOptions = { trackOptionsRequest = null },
            onAddTrackToPlaylist = { track ->
                trackOptionsRequest = null
                openAddToPlaylist(PlaylistSource.TrackSource(track.id))
            },
            onToggleTrackLiked = { track ->
                trackOptionsRequest = null
                toggleTrackLikedWithToast(track.id)
            },
            onPlayNextTrack = { track ->
                trackOptionsRequest = null
                playNextTrackWithToast(track)
            },
            onAddTrackToQueue = { track ->
                trackOptionsRequest = null
                addTrackToQueueWithToast(track)
            },
            onGoToAlbum = ::navigateToAlbum,
            onGoToArtist = ::navigateToArtist,
            onEditTrackMetadata = { track ->
                trackOptionsRequest = null
                editMetadataTrackId = track.id
            },
            editMetadataTrackId = editMetadataTrackId,
            onDismissEditMetadata = { editMetadataTrackId = null },
            bulkEditTrackIds = bulkEditTrackIds,
            onDismissBulkEdit = { bulkEditTrackIds = null },
            playlistSource = playlistSource,
            playlistSheetSession = playlistSheetSession,
            onDismissPlaylistSheet = { playlistSource = null },
            onCreatePlaylist = { trackIds ->
                playlistSource = null
                topLevelBackStack.add(
                    CreatePlaylistRoute(
                        initialTrackIds = trackIds,
                        openDetailOnCreate = false,
                    ),
                )
            },
        )
    }
}

private const val PANE_DIVIDER_ALPHA = 0.5f
private val PANE_DIVIDER_WIDTH = 1.dp
private val MINI_PLAYER_MAX_WIDTH = 720.dp

private fun isListPaneRoute(route: NavKey): Boolean =
    route == LibraryRoute || route == PlaylistsRoute

private fun isDetailPaneRoute(route: NavKey): Boolean =
    route is LibraryAlbumRoute ||
        route is LibraryArtistRoute ||
        route is LibraryFolderRoute ||
        route is PlaylistDetailRoute

/**
 * Scaffold de una pantalla: barra superior propia con scroll `enterAlways` independiente y
 * fondo del panel. El contenido se dibuja detrás de la barra y recibe su alto por [LocalPaneChrome].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RoutePane(
    containerColor: Color,
    statusBarHeight: Dp,
    canScroll: Boolean,
    scrollResetKey: Any?,
    showPaneDivider: Boolean,
    topBar: @Composable (TopAppBarScrollBehavior) -> Unit,
    content: @Composable () -> Unit,
) {
    val topBarState = rememberSaveable(saver = TopAppBarState.Saver) {
        TopAppBarState(
            initialHeightOffsetLimit = -Float.MAX_VALUE,
            initialHeightOffset = 0f,
            initialContentOffset = 0f,
        )
    }
    val currentCanScroll by rememberUpdatedState(canScroll)
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(
        state = topBarState,
        canScroll = remember { { currentCanScroll } },
    )
    LaunchedEffect(scrollResetKey) {
        topBarState.heightOffset = 0f
        topBarState.contentOffset = 0f
    }
    val dividerColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = PANE_DIVIDER_ALPHA)
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .then(
                if (showPaneDivider) {
                    Modifier.drawWithContent {
                        drawContent()
                        drawRect(color = dividerColor, size = Size(PANE_DIVIDER_WIDTH.toPx(), size.height))
                    }
                } else {
                    Modifier
                },
            ),
        containerColor = containerColor,
        contentColor = MaterialTheme.colorScheme.onBackground,
        contentWindowInsets = WindowInsets.systemBars
            .union(WindowInsets.displayCutout)
            .only(WindowInsetsSides.Horizontal),
        topBar = {
            TopBarPaneSegment(
                statusBarColor = containerColor,
                collapsedFraction = { scrollBehavior.state.collapsedFraction },
            ) {
                topBar(scrollBehavior)
            }
        },
    ) { innerPadding ->
        val layoutDirection = LocalLayoutDirection.current
        val paneChrome = PaneChrome(
            contentPadding = PaddingValues(
                start = innerPadding.calculateStartPadding(layoutDirection),
                top = calculateTopContentPadding(
                    scaffoldTopPadding = innerPadding.calculateTopPadding(),
                    statusBarPadding = statusBarHeight,
                    collapsedFraction = scrollBehavior.state.collapsedFraction,
                ),
                end = innerPadding.calculateEndPadding(layoutDirection),
            ),
            collapsedFraction = { scrollBehavior.state.collapsedFraction },
        )
        CompositionLocalProvider(LocalPaneChrome provides paneChrome) {
            content()
        }
    }
}

/** Segmento de la barra superior con su propia protección de la barra de estado. */
@Composable
private fun TopBarPaneSegment(
    statusBarColor: Color,
    collapsedFraction: () -> Float,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(modifier = modifier) {
        StatusBarProtection(
            color = statusBarColor,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .graphicsLayer { alpha = collapsedFraction() },
        )
        content()
    }
}

internal fun calculateTopContentPadding(
    scaffoldTopPadding: androidx.compose.ui.unit.Dp,
    statusBarPadding: androidx.compose.ui.unit.Dp,
    collapsedFraction: Float,
): androidx.compose.ui.unit.Dp =
    (scaffoldTopPadding - statusBarPadding * collapsedFraction.coerceIn(0f, 1f))
        .coerceAtLeast(0.dp)

private fun requiredAudioPermission(): String =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
