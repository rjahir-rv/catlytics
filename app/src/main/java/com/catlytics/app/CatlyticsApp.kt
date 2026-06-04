package com.catlytics.app

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
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
import com.catlytics.core.designsystem.R
import com.catlytics.core.designsystem.component.CatlyticsMiniPlayer
import com.catlytics.core.model.PlaybackStatus
import com.catlytics.core.navigation.TopLevelBackStack
import com.catlytics.feature.home.api.HomeRoute
import com.catlytics.feature.home.impl.homeEntry
import com.catlytics.feature.library.impl.libraryEntry
import com.catlytics.feature.playlists.impl.playlistsEntry
import com.catlytics.feature.statistics.impl.statisticsEntry

@Composable
fun CatlyticsApp(
    modifier: Modifier = Modifier,
    playbackViewModel: PlaybackViewModel = hiltViewModel(),
) {
    val topLevelBackStack = remember { TopLevelBackStack(HomeRoute) }
    val playbackState by playbackViewModel.playbackState.collectAsStateWithLifecycle()
    val isNowPlayingVisible = topLevelBackStack.backStack.lastOrNull() == NowPlayingRoute

    fun closeNowPlaying() {
        topLevelBackStack.removeLast()
    }

    Scaffold(
        modifier = modifier,
        bottomBar = {
            if (!isNowPlayingVisible) {
                Column {
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
                                    contentDescription = "Caratula de ${track.title}",
                                    placeholder = painterResource(id = R.drawable.placeholder_album),
                                    error = painterResource(id = R.drawable.placeholder_album),
                                    fallback = painterResource(id = R.drawable.placeholder_album),
                                    contentScale = ContentScale.Crop,
                                    modifier = artworkModifier,
                                )
                            },
                        )
                    }
                    CatlyticsBottomBar(
                        selectedRoute = topLevelBackStack.topLevelKey,
                        onDestinationSelected = topLevelBackStack::addTopLevel,
                    )
                }
            }
        },
    ) { innerPadding ->
        NavDisplay(
            modifier = Modifier.padding(innerPadding),
            backStack = topLevelBackStack.backStack,
            onBack = { closeNowPlaying() },
            transitionSpec = {
                fadeIn() togetherWith fadeOut()
            },
            popTransitionSpec = {
                fadeIn() togetherWith fadeOut()
            },
            entryProvider = entryProvider {
                homeEntry()
                libraryEntry()
                playlistsEntry()
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
                        onBack = ::closeNowPlaying,
                        onTogglePlayback = playbackViewModel::togglePlayback,
                        onSkipPrevious = playbackViewModel::skipPrevious,
                        onSkipNext = playbackViewModel::skipNext,
                        onSeekTo = playbackViewModel::seekTo,
                    )
                }
            },
        )
    }
}

private const val NOW_PLAYING_TRANSITION_MILLIS = 450

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
