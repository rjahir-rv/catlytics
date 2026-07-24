package com.catlytics.app.ui

import android.Manifest
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.metadata
import androidx.navigation3.ui.NavDisplay
import coil3.compose.AsyncImage
import com.catlytics.app.navigation.TopLevelDestination
import com.catlytics.app.navigation.navigationBackTransition
import com.catlytics.app.navigation.navigationForwardTransition
import com.catlytics.app.navigation.nowPlayingEnterTransition
import com.catlytics.app.navigation.nowPlayingExitTransition
import com.catlytics.app.playback.NowPlayingRoute
import com.catlytics.app.playback.NowPlayingScreen
import com.catlytics.app.playback.PlaybackViewModel
import com.catlytics.app.playback.shareTrack
import com.catlytics.app.ui.chrome.CatlyticsBottomBar
import com.catlytics.app.ui.chrome.LibraryDetailTopAppBar
import com.catlytics.app.ui.chrome.SettingsTopAppBar
import com.catlytics.app.ui.chrome.StatusBarProtection
import com.catlytics.app.ui.chrome.TopLevelTopAppBar
import com.catlytics.app.ui.sheet.CatlyticsAppSheets
import com.catlytics.app.ui.sheet.TrackOptionsRequest
import com.catlytics.core.designsystem.R
import com.catlytics.core.designsystem.component.CatlyticsMiniPlayer
import com.catlytics.core.domain.usecase.playlist.ToggleLikedTrackResult
import com.catlytics.core.model.LIKED_PLAYLIST_ID
import com.catlytics.core.model.LIKED_PLAYLIST_NAME
import com.catlytics.core.model.PlaybackStatus
import com.catlytics.core.model.PlaylistSource
import com.catlytics.core.model.Track
import com.catlytics.core.navigation.TopLevelBackStack
import com.catlytics.feature.home.api.HomeRoute
import com.catlytics.feature.home.api.DailyPlaylistRoute
import com.catlytics.feature.home.impl.homeEntry
import com.catlytics.feature.library.api.LibraryRoute
import com.catlytics.feature.library.api.LibraryAlbumRoute
import com.catlytics.feature.library.api.LibraryArtistRoute
import com.catlytics.feature.library.api.LibraryFolderRoute
import com.catlytics.feature.library.impl.navigation.libraryEntry
import com.catlytics.feature.playlists.api.PlaylistDetailRoute
import com.catlytics.feature.playlists.api.PlaylistsRoute
import com.catlytics.feature.playlists.impl.playlistsEntry
import com.catlytics.feature.settings.api.SettingsRoute
import com.catlytics.feature.settings.impl.settingsEntry
import com.catlytics.feature.statistics.impl.statisticsEntry
import com.catlytics.feature.statistics.api.StatisticsRoute

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatlyticsApp(
    modifier: Modifier = Modifier,
    playbackViewModel: PlaybackViewModel = hiltViewModel(),
    startupViewModel: AppStartupViewModel = hiltViewModel(),
    deepLinkUri: Uri? = null,
    onDeepLinkHandled: () -> Unit = {},
) {
    val context = LocalContext.current
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
    val topLevelBackStack = remember { TopLevelBackStack(HomeRoute) }
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
    var detailTopBarColor by remember { mutableStateOf<Color?>(null) }
    var settingsTopBarTitle by remember { mutableStateOf("Ajustes") }
    var settingsTopBarBackAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    val appVersion = remember(context) {
        context.packageManager
            .getPackageInfo(context.packageName, 0)
            .versionName
            .orEmpty()
    }
    val playbackState by playbackViewModel.playbackState.collectAsStateWithLifecycle()
    val isCurrentTrackLiked by playbackViewModel.isCurrentTrackLiked.collectAsStateWithLifecycle()
    val likedTrackIds by playbackViewModel.likedTrackIds.collectAsStateWithLifecycle()
    val currentRoute = topLevelBackStack.backStack.lastOrNull()
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
        is PlaylistDetailRoute -> TopLevelDestination.Playlists
        DailyPlaylistRoute -> TopLevelDestination.Home
        else -> currentTopLevelDestination
    }
    val isNowPlayingVisible = currentRoute == NowPlayingRoute
    val isSettingsVisible = currentRoute == SettingsRoute
    val isCurrentSearchExpanded = when (currentRoute) {
        HomeRoute -> isHomeSearchExpanded
        LibraryRoute -> isLibrarySearchExpanded
        PlaylistsRoute -> isPlaylistsSearchExpanded
        is LibraryArtistRoute -> isArtistSearchExpanded
        else -> false
    }
    val canTopBarScroll = rememberUpdatedState(
        !isNowPlayingVisible && !isCurrentSearchExpanded,
    )
    val topBarScrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(
        canScroll = remember { { canTopBarScroll.value } },
    )
    val detailChromeColor = when (currentRoute) {
        is LibraryAlbumRoute, is LibraryArtistRoute, is PlaylistDetailRoute -> detailTopBarColor
        else -> null
    }
    LaunchedEffect(currentRoute) {
        detailTopBarColor = null
        isArtistSearchExpanded = false
        artistSearchQuery = ""
        if (currentRoute != SettingsRoute) {
            settingsTopBarTitle = "Ajustes"
            settingsTopBarBackAction = null
        }
    }
    LaunchedEffect(currentRoute, settingsTopBarTitle, isCurrentSearchExpanded) {
        topBarScrollBehavior.state.heightOffset = 0f
        topBarScrollBehavior.state.contentOffset = 0f
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
        topLevelBackStack.add(LibraryAlbumRoute(albumId, albumTitle))
    }

    fun navigateToArtist(track: Track) {
        trackOptionsRequest = null
        topLevelBackStack.addTopLevel(LibraryRoute)
        topLevelBackStack.add(LibraryArtistRoute(track.artist.id, track.artist.name))
    }

    fun canAddTrackToQueue(track: Track): Boolean =
        playbackState.currentTrack != null &&
            playbackState.queue.none { it.id == track.id }

    fun toggleTrackLikedWithToast(trackId: String) {
        playbackViewModel.toggleTrackLiked(trackId) { result ->
            Toast.makeText(
                context,
                when (result) {
                    ToggleLikedTrackResult.Added ->
                        "Canción agregada a $LIKED_PLAYLIST_NAME"
                    ToggleLikedTrackResult.Removed ->
                        "Canción eliminada de $LIKED_PLAYLIST_NAME"
                },
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    fun addTrackToQueueWithToast(track: Track) {
        playbackViewModel.addQueueItem(track) {
            Toast.makeText(
                context,
                "${track.title} agregada a la cola",
                Toast.LENGTH_SHORT,
            ).show()
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
        if (isHomeSearchExpanded || isLibrarySearchExpanded || isPlaylistsSearchExpanded || isArtistSearchExpanded) {
            searchFocusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    LaunchedEffect(currentRoute) {
        if (currentRoute != HomeRoute && isHomeSearchExpanded) {
            closeHomeSearch()
        }
        if (currentRoute != LibraryRoute && isLibrarySearchExpanded) {
            closeLibrarySearch()
        }
        if (currentRoute != PlaylistsRoute && isPlaylistsSearchExpanded) {
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

    BackHandler(enabled = showStartupLoading) {}
    StartupLoadingGate(
        isLoading = showStartupLoading,
        composeContent = !isLibraryRefreshRunning,
        modifier = modifier.fillMaxSize(),
    ) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(topBarScrollBehavior.nestedScrollConnection),
        topBar = {
            when {
                currentRoute is LibraryAlbumRoute -> {
                    LibraryDetailTopAppBar(
                        title = currentRoute.albumTitle,
                        onBack = ::closeCurrentDestination,
                        containerColor = detailChromeColor,
                        scrollBehavior = topBarScrollBehavior,
                    )
                }
                currentRoute is LibraryArtistRoute -> {
                    LibraryDetailTopAppBar(
                        title = "",
                        onBack = ::closeCurrentDestination,
                        containerColor = detailChromeColor,
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
                        searchPlaceholder = "Buscar canciones o álbumes",
                        searchFocusRequester = searchFocusRequester,
                        scrollBehavior = topBarScrollBehavior,
                    )
                }
                currentRoute is LibraryFolderRoute -> {
                    LibraryDetailTopAppBar(
                        title = currentRoute.folderName,
                        onBack = ::closeCurrentDestination,
                        scrollBehavior = topBarScrollBehavior,
                    )
                }
                currentRoute is PlaylistDetailRoute -> {
                    LibraryDetailTopAppBar(
                        title = "",
                        onBack = ::closeCurrentDestination,
                        containerColor = detailChromeColor,
                        scrollBehavior = topBarScrollBehavior,
                    )
                    //
                }
                currentRoute == DailyPlaylistRoute -> {
                    LibraryDetailTopAppBar(
                        title = "Playlist diaria",
                        onBack = ::closeCurrentDestination,
                        scrollBehavior = topBarScrollBehavior,
                    )
                }
                currentTopLevelDestination != null -> {
                    val supportsSearch = isOnHomeRoot || isOnLibraryRoot || isOnPlaylistsRoot
                    val isSearchExpanded = when {
                        isOnHomeRoot -> isHomeSearchExpanded
                        isOnLibraryRoot -> isLibrarySearchExpanded
                        isOnPlaylistsRoot -> isPlaylistsSearchExpanded
                        else -> false
                    }
                    val currentSearchQuery = when {
                        isOnHomeRoot -> homeSearchQuery
                        isOnLibraryRoot -> librarySearchQuery
                        isOnPlaylistsRoot -> playlistsSearchQuery
                        else -> ""
                    }
                    val searchPlaceholder = when {
                        isOnHomeRoot -> "Buscar canciones"
                        isOnLibraryRoot -> "Buscar álbumes o artistas"
                        isOnPlaylistsRoot -> "Buscar playlists"
                        else -> "Buscar"
                    }

                    TopLevelTopAppBar(
                        title = currentTopLevelDestination.label,
                        supportsSearch = supportsSearch,
                        isSearchExpanded = isSearchExpanded,
                        searchQuery = currentSearchQuery,
                        searchFocusRequester = searchFocusRequester,
                        onSearchQueryChange = { newValue ->
                            if (isOnHomeRoot) {
                                homeSearchQuery = newValue
                            } else if (isOnLibraryRoot) {
                                librarySearchQuery = newValue
                            } else if (isOnPlaylistsRoot) {
                                playlistsSearchQuery = newValue
                            }
                        },
                        onSearchActionClick = {
                            if (isOnHomeRoot) {
                                if (isHomeSearchExpanded) {
                                    closeHomeSearch()
                                } else {
                                    isHomeSearchExpanded = true
                                }
                            } else if (isOnLibraryRoot) {
                                if (isLibrarySearchExpanded) {
                                    closeLibrarySearch()
                                } else {
                                    isLibrarySearchExpanded = true
                                }
                            } else if (isOnPlaylistsRoot) {
                                if (isPlaylistsSearchExpanded) {
                                    closePlaylistsSearch()
                                } else {
                                    isPlaylistsSearchExpanded = true
                                }
                            }
                        },
                        searchPlaceholder = searchPlaceholder,
                        onSettingsClick = ::openSettings,
                        scrollBehavior = topBarScrollBehavior,
                    )
                }
                isSettingsVisible -> {
                    SettingsTopAppBar(
                        title = settingsTopBarTitle,
                        onBack = settingsTopBarBackAction ?: ::closeCurrentDestination,
                        scrollBehavior = topBarScrollBehavior,
                    )
                }
            }
        },
        bottomBar = {
            if (!isNowPlayingVisible) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    playbackState.currentTrack?.let { track ->
                        CatlyticsMiniPlayer(
                            modifier = if (isSettingsVisible) {
                                Modifier.padding(bottom = 8.dp)
                            } else {
                                Modifier
                            },
                            title = track.title,
                            artist = track.artist.name,
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
                            artwork = { artworkModifier ->
                                AsyncImage(
                                    model = track.artworkUri,
                                    contentDescription = "Carátula de ${track.title}",
                                    placeholder = painterResource(id = R.drawable.placeholder_album),
                                    error = painterResource(id = R.drawable.placeholder_album),
                                    fallback = painterResource(id = R.drawable.placeholder_album),
                                    contentScale = ContentScale.Crop,
                                    modifier = artworkModifier,
                                )
                            },
                        )
                    }

                    if (selectedTopLevelDestination != null) {
                        CatlyticsBottomBar(
                            selectedRoute = topLevelBackStack.topLevelKey,
                            onDestinationSelected = topLevelBackStack::addTopLevel,
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        var lastBottomPadding by remember { mutableStateOf(0.dp) }
        val currentBottomPadding = innerPadding.calculateBottomPadding()

        var lastRegularNavigationContentPadding by remember { mutableStateOf(PaddingValues(0.dp)) }
        val contentPaddingBehindBottomBar = PaddingValues(
            start = innerPadding.calculateStartPadding(layoutDirection),
            top = innerPadding.calculateTopPadding(),
            end = innerPadding.calculateEndPadding(layoutDirection),
            bottom = 0.dp,
        )

        SideEffect {
            if (!isNowPlayingVisible) {
                lastBottomPadding = currentBottomPadding
                lastRegularNavigationContentPadding = contentPaddingBehindBottomBar
            }
        }

        val bottomPaddingToUse = if (isNowPlayingVisible) lastBottomPadding else currentBottomPadding
        val regularNavigationContentPadding = if (isNowPlayingVisible) lastRegularNavigationContentPadding else contentPaddingBehindBottomBar

        val bottomPaddingState = androidx.compose.runtime.rememberUpdatedState(bottomPaddingToUse)
        val regularPaddingState = androidx.compose.runtime.rememberUpdatedState(regularNavigationContentPadding)

        Box(modifier = Modifier.fillMaxSize()) {
            NavDisplay(
                modifier = Modifier.fillMaxSize(),
                backStack = topLevelBackStack.backStack,
                onBack = ::closeCurrentDestination,
                transitionSpec = {
                    navigationForwardTransition()
                },
                popTransitionSpec = {
                    navigationBackTransition()
                },
                predictivePopTransitionSpec = {
                    navigationBackTransition()
                },
                entryProvider = entryProvider {
                    homeEntry(
                        searchQuery = { homeSearchQuery },
                        onTrackOptions = { track -> openTrackOptions(track) },
                        onNavigateToStatistics = {
                            topLevelBackStack.addTopLevel(StatisticsRoute)
                        },
                        onNavigateToDailyPlaylist = {
                            topLevelBackStack.add(DailyPlaylistRoute)
                        },
                        onNavigateToFavorites = {
                            topLevelBackStack.addTopLevel(PlaylistsRoute)
                            topLevelBackStack.add(
                                PlaylistDetailRoute(
                                    playlistId = LIKED_PLAYLIST_ID,
                                    playlistName = LIKED_PLAYLIST_NAME,
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
                        contentPadding = { regularPaddingState.value },
                    )
                    libraryEntry(
                        searchQuery = { librarySearchQuery },
                        artistSearchQuery = { artistSearchQuery },
                        onArtistSearchQueryChange = { artistSearchQuery = it },
                        onDestinationSelected = topLevelBackStack::add,
                        onAddToPlaylist = ::openAddToPlaylist,
                        onTrackOptions = { track -> openTrackOptions(track) },
                        onLibraryDetailTopBarColorChange = { color ->
                            detailTopBarColor = color
                        },
                        hasAudioPermission = { hasAudioPermission },
                        onRequestAudioPermission = {
                            audioPermissionLauncher.launch(audioPermission)
                        },
                        bottomPadding = { bottomPaddingState.value },
                        contentPadding = { regularPaddingState.value },
                    )
                    playlistsEntry(
                        searchQuery = { playlistsSearchQuery },
                        onDestinationSelected = topLevelBackStack::add,
                        onPlaylistDeleted = ::closeCurrentDestination,
                        onTrackOptions = { track, onRemoveFromPlaylist ->
                            openTrackOptions(track, onRemoveFromPlaylist)
                        },
                        bottomPadding = { bottomPaddingState.value },
                        onPlaylistDetailTopBarColorChange = { color ->
                            detailTopBarColor = color
                        },
                        contentPadding = { regularPaddingState.value },
                    )
                    settingsEntry(
                        appVersion = appVersion,
                        hasAudioPermission = { hasAudioPermission },
                        onRequestAudioPermission = {
                            audioPermissionLauncher.launch(audioPermission)
                        },
                        bottomPadding = { bottomPaddingState.value },
                        onTopBarTitleChange = { title -> settingsTopBarTitle = title },
                        onTopBarBackActionChange = { action -> settingsTopBarBackAction = action },
                        contentPadding = { regularPaddingState.value },
                    )
                    statisticsEntry(
                        bottomPadding = { bottomPaddingState.value },
                        contentPadding = { regularPaddingState.value },
                    )
                    entry<NowPlayingRoute>(
                        metadata = metadata {
                            put(NavDisplay.TransitionKey) {
                                nowPlayingEnterTransition()
                            }
                            put(NavDisplay.PopTransitionKey) {
                                nowPlayingExitTransition()
                            }
                            put(NavDisplay.PredictivePopTransitionKey) { _ ->
                                nowPlayingExitTransition()
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
                            onAddCurrentTrackToQueue = {
                                playbackState.currentTrack?.let(::addTrackToQueueWithToast)
                            },
                            onGoToCurrentTrackAlbum = {
                                playbackState.currentTrack?.let(::navigateToAlbum)
                            },
                            onGoToCurrentTrackArtist = {
                                playbackState.currentTrack?.let(::navigateToArtist)
                            },
                            isCurrentTrackLiked = isCurrentTrackLiked,
                            onAddCurrentTrackToLiked = {
                                playbackViewModel.toggleCurrentTrackLiked { result ->
                                    Toast.makeText(
                                        context,
                                        when (result) {
                                            ToggleLikedTrackResult.Added ->
                                                "Canción agregada a $LIKED_PLAYLIST_NAME"
                                            ToggleLikedTrackResult.Removed ->
                                                "Canción eliminada de $LIKED_PLAYLIST_NAME"
                                        },
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                }
                            },
                        )
                    }
                },
            )
            if (!isNowPlayingVisible) {
                StatusBarProtection(
                    color = detailChromeColor ?: MaterialTheme.colorScheme.background,
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
            onAddTrackToQueue = { track ->
                trackOptionsRequest = null
                addTrackToQueueWithToast(track)
            },
            onGoToAlbum = ::navigateToAlbum,
            onGoToArtist = ::navigateToArtist,
            playlistSource = playlistSource,
            playlistSheetSession = playlistSheetSession,
            onDismissPlaylistSheet = { playlistSource = null },
        )
    }
}

private fun requiredAudioPermission(): String =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
