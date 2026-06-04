package com.catlytics.app

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
import androidx.compose.ui.res.painterResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.catlytics.app.navigation.TopLevelDestination
import com.catlytics.app.playback.PlaybackViewModel
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

    Scaffold(
        modifier = modifier,
        bottomBar = {
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
                    )
                }
                CatlyticsBottomBar(
                    selectedRoute = topLevelBackStack.topLevelKey,
                    onDestinationSelected = topLevelBackStack::addTopLevel,
                )
            }
        },
    ) { innerPadding ->
        NavDisplay(
            modifier = Modifier.padding(innerPadding),
            backStack = topLevelBackStack.backStack,
            onBack = { topLevelBackStack.removeLast() },
            entryProvider = entryProvider {
                homeEntry()
                libraryEntry()
                playlistsEntry()
                statisticsEntry()
            },
        )
    }
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
