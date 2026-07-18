package com.catlytics.feature.home.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.catlytics.core.designsystem.theme.CatlyticsTheme
import com.catlytics.core.model.Artist
import com.catlytics.core.model.Track

@Composable
internal fun HomeRoute(
    searchQuery: String,
    modifier: Modifier = Modifier,
    onTrackOptions: (Track) -> Unit,
    onNavigateToStatistics: () -> Unit,
    hasAudioPermission: Boolean,
    onRequestPermission: () -> Unit,
    startupError: String? = null,
    bottomPadding: () -> Dp = { 0.dp },
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    HomeScreen(
        uiState = startupError?.let(HomeUiState::Error) ?: uiState,
        searchQuery = searchQuery,
        hasAudioPermission = hasAudioPermission,
        onRequestPermission = onRequestPermission,
        onTrackSelected = viewModel::onTrackSelected,
        onTrackOptions = onTrackOptions,
        onNavigateToStatistics = onNavigateToStatistics,
        bottomPadding = bottomPadding,
        modifier = modifier,
    )
}

@Composable
internal fun HomeScreen(
    uiState: HomeUiState,
    searchQuery: String,
    hasAudioPermission: Boolean,
    onRequestPermission: () -> Unit,
    onTrackSelected: (Track, List<Track>) -> Unit,
    onTrackOptions: (Track) -> Unit,
    modifier: Modifier = Modifier,
    onNavigateToStatistics: () -> Unit = {},
    bottomPadding: () -> Dp = { 0.dp },
) {
    val trackListState = rememberSaveable(saver = androidx.compose.foundation.lazy.LazyListState.Saver) {
        androidx.compose.foundation.lazy.LazyListState()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(start = 20.dp, top = 20.dp, end = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (!hasAudioPermission) {
            PermissionRequiredContent(
                onRequestPermission = onRequestPermission,
                modifier = Modifier
                    .weight(1f)
                    .padding(bottom = bottomPadding()),
            )
            return@Column
        }

        when (uiState) {
            HomeUiState.Empty -> EmptyLibraryContent(
                modifier = Modifier
                    .weight(1f)
                    .padding(bottom = bottomPadding()),
            )
            is HomeUiState.Error -> ErrorContent(message = uiState.message)
            HomeUiState.Loading -> LoadingContent()
            is HomeUiState.Success -> {
                val filteredTracks = uiState.tracks.filterByQuery(searchQuery)
                if (filteredTracks.isEmpty() && searchQuery.isNotBlank()) {
                    NoSearchResultsContent()
                } else {
                    HomeTrackList(
                        tracks = filteredTracks,
                        recentlyPlayedTracks = uiState.recentlyPlayedTracks,
                        topTracks = uiState.topTracks,
                        currentTrackId = uiState.currentTrackId,
                        isCurrentTrackPlaying = uiState.isCurrentTrackPlaying,
                        onTrackSelected = onTrackSelected,
                        modifier = Modifier.weight(1f),
                        state = trackListState,
                        contentPadding = PaddingValues(
                            top = 8.dp,
                            bottom = bottomPadding() + 20.dp,
                        ),
                        onTrackOptions = onTrackOptions,
                        onRecentlyPlayedTrackSelected = { track ->
                            onTrackSelected(track, uiState.tracks)
                        },
                        onNavigateToStatistics = onNavigateToStatistics,
                        showHighlights = searchQuery.isBlank(),
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    CatlyticsTheme {
        HomeScreen(
            uiState = HomeUiState.Success(
                tracks = listOf(
                    Track(
                        id = "track-current",
                        title = "Electric Feel",
                        artist = Artist(id = "artist-preview", name = "MGMT"),
                        durationMillis = 186_000,
                        mediaUri = "content://media/external/audio/media/1",
                    ),
                    Track(
                        id = "track-preview",
                        title = "Canción local con un título bastante largo",
                        artist = Artist(id = "artist-local", name = "Artista local"),
                        durationMillis = 242_000,
                        mediaUri = "content://media/external/audio/media/2",
                    ),
                ),
                currentTrackId = "track-current",
            ),
            searchQuery = "",
            hasAudioPermission = true,
            onRequestPermission = {},
            onTrackSelected = { _, _ -> },
            onTrackOptions = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPermissionRequiredPreview() {
    CatlyticsTheme {
        HomeScreen(
            uiState = HomeUiState.Empty,
            searchQuery = "",
            hasAudioPermission = false,
            onRequestPermission = {},
            onTrackSelected = { _, _ -> },
            onTrackOptions = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenEmptyLibraryPreview() {
    CatlyticsTheme {
        HomeScreen(
            uiState = HomeUiState.Empty,
            searchQuery = "",
            hasAudioPermission = true,
            onRequestPermission = {},
            onTrackSelected = { _, _ -> },
            onTrackOptions = {},
        )
    }
}
