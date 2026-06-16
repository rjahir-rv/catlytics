package com.catlytics.app

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.metadata
import androidx.navigation3.ui.NavDisplay
import coil3.compose.AsyncImage
import com.catlytics.app.navigation.TopLevelDestination
import com.catlytics.app.playback.NowPlayingRoute
import com.catlytics.app.playback.NowPlayingScreen
import com.catlytics.app.playback.PlaybackViewModel
import com.catlytics.app.playback.shareTrack
import com.catlytics.core.designsystem.R
import com.catlytics.core.designsystem.component.CatlyticsMiniPlayer
import com.catlytics.core.model.PlaybackStatus
import com.catlytics.core.model.PlaylistSource
import com.catlytics.core.navigation.TopLevelBackStack
import com.catlytics.feature.home.api.HomeRoute
import com.catlytics.feature.home.impl.homeEntry
import com.catlytics.feature.library.impl.navigation.libraryEntry
import com.catlytics.feature.library.api.LibraryAlbumRoute
import com.catlytics.feature.library.api.LibraryArtistRoute
import com.catlytics.feature.library.api.LibraryFolderRoute
import com.catlytics.feature.playlists.impl.playlistsEntry
import com.catlytics.feature.playlists.impl.AddToPlaylistSheet
import com.catlytics.feature.playlists.api.PlaylistDetailRoute
import com.catlytics.feature.settings.api.SettingsRoute
import com.catlytics.feature.settings.impl.settingsEntry
import com.catlytics.feature.statistics.impl.statisticsEntry

@Composable
fun CatlyticsApp(
    modifier: Modifier = Modifier,
    playbackViewModel: PlaybackViewModel = hiltViewModel(),
    deepLinkUri: Uri? = null,
    onDeepLinkHandled: () -> Unit = {},
) {
    val context = LocalContext.current
    val topLevelBackStack = remember { TopLevelBackStack(HomeRoute) }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val searchFocusRequester = remember { FocusRequester() }
    var isHomeSearchExpanded by rememberSaveable { mutableStateOf(false) }
    var homeSearchQuery by rememberSaveable { mutableStateOf("") }
    var playlistSource by remember { mutableStateOf<PlaylistSource?>(null) }
    var miniPlayerHeightPx by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val appVersion = remember(context) {
        context.packageManager
            .getPackageInfo(context.packageName, 0)
            .versionName
            .orEmpty()
    }
    val playbackState by playbackViewModel.playbackState.collectAsStateWithLifecycle()
    val currentRoute = topLevelBackStack.backStack.lastOrNull()
    val currentTopLevelDestination = TopLevelDestination.entries
        .firstOrNull { it.route == currentRoute }
    val selectedTopLevelDestination = when (currentRoute) {
        is LibraryAlbumRoute, is LibraryArtistRoute, is LibraryFolderRoute ->
            TopLevelDestination.Library
        is PlaylistDetailRoute -> TopLevelDestination.Playlists
        else -> currentTopLevelDestination
    }
    val isNowPlayingVisible = currentRoute == NowPlayingRoute
    val isSettingsVisible = currentRoute == SettingsRoute
    val isMiniPlayerVisible = !isNowPlayingVisible && playbackState.currentTrack != null
    val miniPlayerContentPadding = with(density) {
        if (isMiniPlayerVisible) miniPlayerHeightPx.toDp() + MINI_PLAYER_CONTENT_GAP else 0.dp
    }

    fun closeHomeSearch() {
        homeSearchQuery = ""
        isHomeSearchExpanded = false
        focusManager.clearFocus()
        keyboardController?.hide()
    }

    fun openSettings() {
        if (topLevelBackStack.backStack.lastOrNull() != SettingsRoute) {
            topLevelBackStack.add(SettingsRoute)
        }
    }

    BackHandler(enabled = currentRoute == HomeRoute && isHomeSearchExpanded) {
        closeHomeSearch()
    }

    LaunchedEffect(isHomeSearchExpanded) {
        if (isHomeSearchExpanded) {
            searchFocusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    LaunchedEffect(currentRoute) {
        if (currentRoute != HomeRoute && isHomeSearchExpanded) {
            closeHomeSearch()
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

    Scaffold(
        modifier = modifier,
        topBar = {
            when {
                currentRoute is LibraryAlbumRoute -> {
                    LibraryDetailTopAppBar(
                        title = currentRoute.albumTitle,
                        onBack = ::closeCurrentDestination,
                    )
                }
                currentRoute is LibraryArtistRoute -> {
                    LibraryDetailTopAppBar(
                        title = currentRoute.artistName,
                        onBack = ::closeCurrentDestination,
                    )
                }
                currentRoute is LibraryFolderRoute -> {
                    LibraryDetailTopAppBar(
                        title = currentRoute.folderName,
                        onBack = ::closeCurrentDestination,
                    )
                }
                currentRoute is PlaylistDetailRoute -> {
                    // No title here to avoid duplicating the name shown in the detail header
                    LibraryDetailTopAppBar(
                        title = "",
                        onBack = ::closeCurrentDestination,
                    )
                }
                currentTopLevelDestination != null -> {
                    TopLevelTopAppBar(
                        title = currentTopLevelDestination.label,
                        isHome = currentRoute == HomeRoute,
                        isSearchExpanded = isHomeSearchExpanded,
                        searchQuery = homeSearchQuery,
                        searchFocusRequester = searchFocusRequester,
                        onSearchQueryChange = { homeSearchQuery = it },
                        onSearchActionClick = {
                            if (isHomeSearchExpanded) {
                                closeHomeSearch()
                            } else {
                                isHomeSearchExpanded = true
                            }
                        },
                        onSettingsClick = ::openSettings,
                    )
                }
                isSettingsVisible -> {
                    SettingsTopAppBar(onBack = ::closeCurrentDestination)
                }
            }
        },
        bottomBar = {
            if (selectedTopLevelDestination != null && !isNowPlayingVisible) {
                CatlyticsBottomBar(
                    selectedRoute = topLevelBackStack.topLevelKey,
                    onDestinationSelected = topLevelBackStack::addTopLevel,
                )
            }
        },
    ) { innerPadding ->
        val navigationContentPadding = if (isNowPlayingVisible) {
            PaddingValues(0.dp)
        } else {
            innerPadding
        }

        Box(modifier = Modifier.fillMaxSize()) {
            NavDisplay(
                modifier = Modifier
                    .padding(navigationContentPadding)
                    .padding(bottom = miniPlayerContentPadding),
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
                        onAddToPlaylist = { playlistSource = it },
                    )
                    libraryEntry(
                        onDestinationSelected = topLevelBackStack::add,
                        onAddToPlaylist = { playlistSource = it },
                    )
                    playlistsEntry(onDestinationSelected = topLevelBackStack::add)
                    settingsEntry(appVersion = appVersion)
                    statisticsEntry()
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
                            onAddToPlaylist = { playlistSource = PlaylistSource.TrackSource(it.id) },
                        )
                    }
                },
            )

            if (!isNowPlayingVisible) {
                playbackState.currentTrack?.let { track ->
                    CatlyticsMiniPlayer(
                        title = track.title,
                        artist = track.artist.name,
                        isPlaying = playbackState.status == PlaybackStatus.Playing,
                        isBuffering = playbackState.status == PlaybackStatus.Buffering,
                        positionMillis = playbackState.positionMillis,
                        durationMillis = playbackState.durationMillis,
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
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = innerPadding.calculateBottomPadding() + 8.dp)
                            .onSizeChanged { size -> miniPlayerHeightPx = size.height }
                    )
                }
            }
        }
    }
    playlistSource?.let { source ->
        AddToPlaylistSheet(source = source, onDismiss = { playlistSource = null })
    }
}

private const val NOW_PLAYING_TRANSITION_MILLIS = 450
private const val NAVIGATION_TRANSITION_MILLIS = 280
private val MINI_PLAYER_CONTENT_GAP = 8.dp

private fun navigationForwardTransition() =
    (slideInHorizontally(
        animationSpec = tween(
            durationMillis = NAVIGATION_TRANSITION_MILLIS,
            easing = FastOutSlowInEasing,
        ),
        initialOffsetX = { fullWidth -> fullWidth / 8 },
    ) + fadeIn(
        animationSpec = tween(durationMillis = NAVIGATION_TRANSITION_MILLIS),
    )) togetherWith (slideOutHorizontally(
        animationSpec = tween(
            durationMillis = NAVIGATION_TRANSITION_MILLIS,
            easing = FastOutSlowInEasing,
        ),
        targetOffsetX = { fullWidth -> -fullWidth / 16 },
    ) + fadeOut(
        animationSpec = tween(durationMillis = NAVIGATION_TRANSITION_MILLIS),
    ))

private fun navigationBackTransition() =
    (slideInHorizontally(
        animationSpec = tween(
            durationMillis = NAVIGATION_TRANSITION_MILLIS,
            easing = FastOutSlowInEasing,
        ),
        initialOffsetX = { fullWidth -> -fullWidth / 16 },
    ) + fadeIn(
        animationSpec = tween(durationMillis = NAVIGATION_TRANSITION_MILLIS),
    )) togetherWith (slideOutHorizontally(
        animationSpec = tween(
            durationMillis = NAVIGATION_TRANSITION_MILLIS,
            easing = FastOutSlowInEasing,
        ),
        targetOffsetX = { fullWidth -> fullWidth / 8 },
    ) + fadeOut(
        animationSpec = tween(durationMillis = NAVIGATION_TRANSITION_MILLIS),
    ))

private fun nowPlayingEnterTransition() =
    slideInVertically(
        animationSpec = tween(
            durationMillis = NOW_PLAYING_TRANSITION_MILLIS,
            easing = FastOutSlowInEasing,
        ),
        initialOffsetY = { fullHeight -> fullHeight },
    ) + fadeIn(
        animationSpec = tween(
            durationMillis = NOW_PLAYING_TRANSITION_MILLIS,
            easing = FastOutSlowInEasing,
        ),
    ) togetherWith fadeOut(
        animationSpec = tween(
            durationMillis = NOW_PLAYING_TRANSITION_MILLIS,
            easing = FastOutSlowInEasing,
        ),
    )

private fun nowPlayingExitTransition() =
    (fadeIn(
        animationSpec = tween(
            durationMillis = NOW_PLAYING_TRANSITION_MILLIS,
            easing = FastOutSlowInEasing,
        ),
    ) togetherWith slideOutVertically(
        animationSpec = tween(
            durationMillis = NOW_PLAYING_TRANSITION_MILLIS,
            easing = FastOutSlowInEasing,
        ),
        targetOffsetY = { fullHeight -> fullHeight },
    )).apply {
        targetContentZIndex = -1f
    }

@Composable
private fun CatlyticsBottomBar(
    selectedRoute: Any,
    onDestinationSelected: (androidx.navigation3.runtime.NavKey) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar(
        modifier = modifier,
    ) {
        TopLevelDestination.entries.forEach { destination ->
            NavigationBarItem(
                selected = selectedRoute == destination.route,
                onClick = { onDestinationSelected(destination.route) },
                icon = {
                    Icon(
                        painter = painterResource(destination.iconRes),
                        contentDescription = destination.label,
                    )
                },
                label = {
                    Text(destination.label)
                },
            )
        }
    }
}
