package com.catlytics.app.ui

import android.Manifest
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation.BackNavigationBehavior
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.material3.adaptive.navigationsuite.rememberNavigationSuiteScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.scene.SinglePaneSceneStrategy
import androidx.navigation3.ui.NavDisplay
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import androidx.navigationevent.compose.rememberNavigationEventDispatcherOwner
import com.catlytics.app.R
import com.catlytics.app.navigation.TopLevelDestination
import com.catlytics.app.navigation.isDetailPaneRoute
import com.catlytics.app.navigation.isListPaneRoute
import com.catlytics.app.navigation.rememberAppNavigator
import com.catlytics.app.playback.MiniPlayerHost
import com.catlytics.app.playback.NowPlayingHost
import com.catlytics.app.playback.PlaybackViewModel
import com.catlytics.app.playback.PrefetchAdjacentPlaybackArtwork
import com.catlytics.app.ui.chrome.CatlyticsNavigationSuiteScaffold
import com.catlytics.app.ui.sheet.CatlyticsAppSheets
import com.catlytics.app.ui.sheet.TrackOptionsRequest
import com.catlytics.core.designsystem.text.UiText
import com.catlytics.core.designsystem.text.displayAlbumTitle
import com.catlytics.core.designsystem.text.displayArtistName
import com.catlytics.core.model.PlaybackStatus
import com.catlytics.core.model.PlaylistSource
import com.catlytics.core.model.Track
import com.catlytics.core.model.TrackSelectionAction
import com.catlytics.core.navigation.findListDetailIndices
import com.catlytics.core.navigation.navigationBackTransition
import com.catlytics.core.navigation.navigationForwardTransition
import com.catlytics.core.navigation.rememberTopLevelBackStack
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
import com.catlytics.feature.playlists.impl.R as PlaylistsR
import com.catlytics.feature.settings.api.SettingsRoute
import com.catlytics.feature.statistics.api.StatisticsExploreRoute
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

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

    val statusBarHeight = WindowInsets.statusBars
        .asPaddingValues()
        .calculateTopPadding()
    val topLevelBackStack = rememberTopLevelBackStack(HomeRoute)
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val searchFocusRequester = remember { FocusRequester() }
    val search = rememberAppSearchState()
    var playlistSource by remember { mutableStateOf<PlaylistSource?>(null) }
    var trackOptionsRequest by remember { mutableStateOf<TrackOptionsRequest?>(null) }
    var editMetadataTrackId by rememberSaveable { mutableStateOf<String?>(null) }
    var bulkEditTrackIds by remember { mutableStateOf<List<String>?>(null) }
    val detailChrome = rememberDetailChromeState(topLevelBackStack)
    var settingsTopBarTitle by remember { mutableStateOf<String?>(null) }
    var settingsTopBarBackAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    val hazeState = rememberHazeState()
    val appVersion = remember(context) {
        context.packageManager
            .getPackageInfo(context.packageName, 0)
            .versionName
            .orEmpty()
    }
    val appFeedback = rememberAppFeedback()
    LaunchedEffect(playbackViewModel) {
        playbackViewModel.messages.collect(appFeedback::show)
    }
    val playbackState by playbackViewModel.playbackState.collectAsStateWithLifecycle()
    val playbackProgress = playbackViewModel.progress.collectAsStateWithLifecycle()
    val likedTrackIds by playbackViewModel.likedTrackIds.collectAsStateWithLifecycle()
    val windowAdaptiveInfo = currentWindowAdaptiveInfoV2()
    val navigationSuiteType = NavigationSuiteScaffoldDefaults.navigationSuiteType(windowAdaptiveInfo)
    val isNavigationRail = navigationSuiteType == NavigationSuiteType.WideNavigationRailCollapsed ||
        navigationSuiteType == NavigationSuiteType.WideNavigationRailExpanded
    val paneScaffoldDirective = remember(windowAdaptiveInfo) {
        calculatePaneScaffoldDirective(windowAdaptiveInfo).copy(horizontalPartitionSpacerSize = 0.dp)
    }
    val isTwoPane = paneScaffoldDirective.maxHorizontalPartitions > 1
    val navigator = rememberAppNavigator(topLevelBackStack, isTwoPane)
    val isNowPlayingOpen = navigator.isNowPlayingOpen
    val contentBackStack = navigator.contentBackStack
    val currentRoute = contentBackStack.lastOrNull()
    // Lista y detalle visibles a la vez en ventanas anchas (ver ListDetailSceneStrategy).
    val listDetailIndices = if (isTwoPane) {
        findListDetailIndices(
            items = contentBackStack,
            isList = ::isListPaneRoute,
            isDetail = ::isDetailPaneRoute,
        )
    } else {
        null
    }
    val listPaneRoute = listDetailIndices?.let { contentBackStack[it.listIndex] }
    val detailPaneRoute = listDetailIndices?.detailIndex?.let { contentBackStack[it] }
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
    // Now Playing no cuenta: tapa la barra de navegación y el mini reproductor sin ocultarlos.
    val isFullScreenRoute = currentRoute is CreatePlaylistRoute
    // En pantallas grandes la navegación nunca se oculta (tampoco en pantallas completas).
    val isNavigationVisible = isNavigationRail ||
        (!isFullScreenRoute && selectedTopLevelDestination != null)
    val isNavigationBarShown = isNavigationVisible && !isNavigationRail
    LaunchedEffect(currentRoute) {
        search.artist.clear()
        if (currentRoute != SettingsRoute) {
            settingsTopBarTitle = null
            settingsTopBarBackAction = null
        }
    }

    fun closeSearch(field: SearchField) {
        field.clear()
        focusManager.clearFocus()
        keyboardController?.hide()
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
        playlistSource = source
    }

    fun navigateToAlbum(track: Track) {
        val albumId = track.albumId ?: return
        val albumTitle = track.albumTitle ?: return
        trackOptionsRequest = null
        navigator.openLibraryDetail(LibraryAlbumRoute(albumId, resources.displayAlbumTitle(albumTitle)))
    }

    fun navigateToArtist(track: Track) {
        trackOptionsRequest = null
        navigator.openLibraryDetail(
            LibraryArtistRoute(track.artist.id, resources.displayArtistName(track.artist.name)),
        )
    }

    fun canAddTrackToQueue(track: Track): Boolean =
        playbackState.currentTrack?.id?.let { currentTrackId ->
            track.id != currentTrackId
        } ?: false

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
            is TrackSelectionAction.Like -> playbackViewModel.likeTracks(action.tracks.map(Track::id))
            is TrackSelectionAction.Unlike -> playbackViewModel.unlikeTracks(action.tracks.map(Track::id))
            is TrackSelectionAction.AddToQueue -> playbackViewModel.addQueueItems(action.tracks)
            is TrackSelectionAction.EditMetadata -> {
                val trackIds = action.tracks.map(Track::id)
                // A single track gets the full editor, which also covers its title.
                if (trackIds.size == 1) editMetadataTrackId = trackIds.single() else bulkEditTrackIds = trackIds
            }
            is TrackSelectionAction.PlayNext -> playbackViewModel.playNextTracks(action.tracks)
        }
    }

    val isAnySearchOpen = (isOnHomeRoot && search.home.isExpanded) ||
        (isOnLibraryRoot && search.library.isExpanded) ||
        (isOnPlaylistsRoot && search.playlists.isExpanded) ||
        search.artist.isExpanded
    BackHandler(enabled = !isNowPlayingOpen && isAnySearchOpen) {
        if (isOnHomeRoot && search.home.isExpanded) {
            closeSearch(search.home)
        } else if (isOnLibraryRoot && search.library.isExpanded) {
            closeSearch(search.library)
        } else if (isOnPlaylistsRoot && search.playlists.isExpanded) {
            closeSearch(search.playlists)
        } else if (search.artist.isExpanded) {
            search.artist.clear()
        }
    }

    LaunchedEffect(
        search.home.isExpanded,
        search.library.isExpanded,
        search.playlists.isExpanded,
        search.artist.isExpanded,
    ) {
        if (search.artist.isExpanded && listPaneRoute != null) {
            searchFocusRequester.requestFocus()
            keyboardController?.show()
        } else if (search.library.isExpanded && listPaneRoute != null) {
            listPaneSearchFocusRequester.requestFocus()
            keyboardController?.show()
        } else if (
            search.home.isExpanded ||
            search.library.isExpanded ||
            search.playlists.isExpanded ||
            search.artist.isExpanded
        ) {
            searchFocusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    LaunchedEffect(visibleListRoute) {
        if (visibleListRoute != HomeRoute && search.home.isExpanded) {
            closeSearch(search.home)
        }
        if (visibleListRoute != LibraryRoute && search.library.isExpanded) {
            closeSearch(search.library)
        }
        if (visibleListRoute != PlaylistsRoute && search.playlists.isExpanded) {
            closeSearch(search.playlists)
        }
    }

    LaunchedEffect(deepLinkUri) {
        if (deepLinkUri != null) {
            if (deepLinkUri.scheme == "catlytics" && deepLinkUri.host == "nowplaying") {
                navigator.openNowPlaying()
            }
            onDeepLinkHandled()
        }
    }

    BackHandler(enabled = showStartupLoading) {}
    PrefetchAdjacentPlaybackArtwork(
        queue = playbackState.queue,
        currentIndex = playbackState.currentIndex,
    )

    // Cada pantalla se envuelve en su propio Scaffold con su barra superior y su estado de scroll,
    // así funcionan igual en un panel o lado a lado en lista-detalle.
    val routePane: @Composable (NavKey, @Composable () -> Unit) -> Unit = { route, content ->
        val hasTopBar = route !is CreatePlaylistRoute
        if (hasTopBar) {
            val isRouteSearchExpanded = search.isExpanded(route)
            RoutePane(
                chromeColor = detailChrome.chromeColor(route),
                statusBarHeight = statusBarHeight,
                canScroll = !isRouteSearchExpanded,
                scrollResetKey = if (route == SettingsRoute) {
                    settingsTopBarTitle to isRouteSearchExpanded
                } else {
                    isRouteSearchExpanded
                },
                showPaneDivider = isTwoPane && route == detailPaneRoute,
                topBar = { scrollBehavior ->
                    RouteTopBar(
                        route = route,
                        scrollBehavior = scrollBehavior,
                        search = search,
                        searchFocusRequester = if (route == listPaneRoute) {
                            listPaneSearchFocusRequester
                        } else {
                            searchFocusRequester
                        },
                        albumTitle = detailChrome.titles[route],
                        settingsTitle = settingsTopBarTitle,
                        onBack = if (isTwoPane && route == detailPaneRoute) null else navigator::closeCurrent,
                        onSettingsBack = settingsTopBarBackAction ?: navigator::closeCurrent,
                        onCloseSearch = ::closeSearch,
                        onSettingsClick = navigator::openSettings,
                    )
                },
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
            selectedRoute = navigator.topLevelKey,
            onDestinationSelected = navigator::addTopLevel,
            navigationSuiteType = navigationSuiteType,
            state = navigationSuiteState,
            modifier = Modifier
                .fillMaxSize()
                // Con Now Playing abierta, TalkBack no recorre lo que queda tapado debajo.
                .then(if (isNowPlayingOpen) Modifier.clearAndSetSemantics {} else Modifier),
        ) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.onBackground,
                bottomBar = {
                    MiniPlayerHost(
                        isVisible = !isFullScreenRoute,
                        track = playbackState.currentTrack,
                        status = playbackState.status,
                        progress = { playbackProgress.value.fraction },
                        isNavigationBarShown = isNavigationBarShown,
                        hazeState = hazeState,
                        onTogglePlayback = playbackViewModel::togglePlayback,
                        onSkipPrevious = playbackViewModel::skipPrevious,
                        onSkipNext = playbackViewModel::skipNext,
                        onClick = navigator::openNowPlaying,
                    )
                },
            ) { innerPadding ->
                // Las pantallas se dibujan detrás del mini reproductor y reciben su alto como padding.
                // Se congela mientras se crea una playlist (pantalla completa, sin mini reproductor) para
                // que la pantalla de abajo no salte durante la transición.
                var lastBottomPadding by remember { mutableStateOf(0.dp) }
                val currentBottomPadding = innerPadding.calculateBottomPadding()
                SideEffect {
                    if (!isFullScreenRoute) {
                        lastBottomPadding = currentBottomPadding
                    }
                }
                val bottomPaddingToUse = if (isFullScreenRoute) lastBottomPadding else currentBottomPadding
                val bottomPaddingState = rememberUpdatedState(bottomPaddingToUse)
                val imePadding = WindowInsets.ime.asPaddingValues()
                val screenBottomPadding: () -> Dp = remember(bottomPaddingState, imePadding) {
                    { maxOf(bottomPaddingState.value, imePadding.calculateBottomPadding()) }
                }
                val contentBackDispatcherOwner = rememberNavigationEventDispatcherOwner(
                    enabled = !isNowPlayingOpen,
                )
                Box(modifier = Modifier.fillMaxSize()) {
                    CompositionLocalProvider(LocalNavigationEventDispatcherOwner provides contentBackDispatcherOwner) {
                        NavDisplay(
                            modifier = Modifier
                                .fillMaxSize()
                                .hazeSource(hazeState)
                                .background(MaterialTheme.colorScheme.background),
                            backStack = contentBackStack,
                            onBack = navigator::closeCurrent,
                            sceneStrategies = listOf(listDetailSceneStrategy, SinglePaneSceneStrategy()),
                            entryDecorators = listOf(
                                rememberSaveableStateHolderNavEntryDecorator(),
                                rememberViewModelStoreNavEntryDecorator(),
                                detailChrome.pruneDecorator,
                            ),
                            transitionSpec = { navigationForwardTransition() },
                            popTransitionSpec = { navigationBackTransition() },
                            predictivePopTransitionSpec = { navigationBackTransition() },
                            entryProvider = withRoutePanes(
                                entryProvider {
                                    appEntries(
                                        navigator = navigator,
                                        search = search,
                                        detailChrome = detailChrome,
                                        likedTrackIds = { likedTrackIds },
                                        currentTrackId = { playbackState.currentTrack?.id },
                                        isCurrentTrackPlaying = { playbackState.status == PlaybackStatus.Playing },
                                        onTrackOptions = ::openTrackOptions,
                                        onAddToPlaylist = ::openAddToPlaylist,
                                        onTrackSelectionAction = ::handleTrackSelectionAction,
                                        hasAudioPermission = { hasAudioPermission },
                                        onRequestAudioPermission = { audioPermissionLauncher.launch(audioPermission) },
                                        startupError = { startupError },
                                        onHomeContentReady = { isHomeContentReady = true },
                                        likedPlaylistName = likedPlaylistName,
                                        appVersion = appVersion,
                                        onSettingsTopBarTitleChange = { title -> settingsTopBarTitle = title },
                                        onSettingsTopBarBackActionChange = { action ->
                                            settingsTopBarBackAction = action
                                        },
                                        onPlaylistCreatedInBackground = { playlist ->
                                            appFeedback.show(
                                                UiText.Plural(
                                                    id = PlaylistsR.plurals.create_playlist_toast_created,
                                                    count = playlist.trackIds.size,
                                                    args = listOf(playlist.trackIds.size, playlist.name),
                                                ),
                                            )
                                        },
                                        bottomPadding = screenBottomPadding,
                                    )
                                },
                            ),
                        )
                    }
                }
            }
        }
        NowPlayingHost(
            isOpen = isNowPlayingOpen,
            playbackViewModel = playbackViewModel,
            playbackState = playbackState,
            progress = { playbackProgress.value },
            onClose = navigator::closeNowPlaying,
            canAddTrackToQueue = ::canAddTrackToQueue,
            onTrackOptions = { track -> openTrackOptions(track) },
            onAddToPlaylist = { track -> openAddToPlaylist(PlaylistSource.TrackSource(track.id)) },
            onGoToAlbum = ::navigateToAlbum,
            onGoToArtist = ::navigateToArtist,
            onEditMetadata = { track -> editMetadataTrackId = track.id },
        )
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
                playbackViewModel.toggleTrackLiked(track.id)
            },
            onPlayNextTrack = { track ->
                trackOptionsRequest = null
                playbackViewModel.playNext(track)
            },
            onAddTrackToQueue = { track ->
                trackOptionsRequest = null
                playbackViewModel.addQueueItem(track)
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
            onDismissPlaylistSheet = { playlistSource = null },
            onCreatePlaylist = { trackIds ->
                playlistSource = null
                navigator.openCreatePlaylist(trackIds)
            },
        )
    }
}

private fun requiredAudioPermission(): String =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
