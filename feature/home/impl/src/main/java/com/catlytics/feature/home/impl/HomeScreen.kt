package com.catlytics.feature.home.impl

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.catlytics.core.designsystem.component.TrackSelectionHost
import com.catlytics.core.designsystem.component.rememberTrackSelectionState
import com.catlytics.core.designsystem.theme.CatlyticsTheme
import com.catlytics.core.model.Artist
import com.catlytics.core.model.Track
import com.catlytics.core.model.TrackSelectionAction

@Composable
internal fun HomeRoute(
    searchQuery: String,
    modifier: Modifier = Modifier,
    onTrackOptions: (Track) -> Unit,
    likedTrackIds: Set<String> = emptySet(),
    onTrackSelectionAction: (TrackSelectionAction) -> Unit = {},
    onNavigateToStatistics: () -> Unit,
    onNavigateToDailyPlaylist: () -> Unit,
    onNavigateToFavorites: () -> Unit,
    hasAudioPermission: Boolean,
    onRequestPermission: () -> Unit,
    startupError: String? = null,
    onContentReady: () -> Unit = {},
    bottomPadding: () -> Dp = { 0.dp },
    scaffoldContentPadding: PaddingValues = PaddingValues(0.dp),
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val displayedUiState = startupError?.let(HomeUiState::Error) ?: uiState

    HomeScreen(
        uiState = displayedUiState,
        searchQuery = searchQuery,
        hasAudioPermission = hasAudioPermission,
        onRequestPermission = onRequestPermission,
        onTrackSelected = viewModel::onTrackSelected,
        onTopTrackSelected = viewModel::onTopTrackSelected,
        onPlayDailyPlaylist = {
            viewModel.onPlayDailyPlaylist()
            onNavigateToDailyPlaylist()
        },
        onShuffleAll = viewModel::onShuffleAll,
        onOpenFavorites = onNavigateToFavorites,
        onTrackOptions = onTrackOptions,
        likedTrackIds = likedTrackIds,
        onTrackSelectionAction = onTrackSelectionAction,
        onNavigateToStatistics = onNavigateToStatistics,
        onContentReady = onContentReady,
        bottomPadding = bottomPadding,
        scaffoldContentPadding = scaffoldContentPadding,
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
    likedTrackIds: Set<String> = emptySet(),
    onTrackSelectionAction: (TrackSelectionAction) -> Unit = {},
    modifier: Modifier = Modifier,
    onTopTrackSelected: (String) -> Unit = {},
    onPlayDailyPlaylist: () -> Unit = {},
    onShuffleAll: () -> Unit = {},
    onOpenFavorites: () -> Unit = {},
    onNavigateToStatistics: () -> Unit = {},
    onContentReady: () -> Unit = {},
    bottomPadding: () -> Dp = { 0.dp },
    scaffoldContentPadding: PaddingValues = PaddingValues(0.dp),
) {
    LaunchedEffect(uiState, hasAudioPermission) {
        if (!hasAudioPermission || uiState != HomeUiState.Loading) {
            onContentReady()
        }
    }

    val trackListState = rememberSaveable(saver = androidx.compose.foundation.lazy.LazyListState.Saver) {
        androidx.compose.foundation.lazy.LazyListState()
    }
    var areFeaturedSectionsVisible by rememberSaveable { mutableStateOf(true) }

    if (!hasAudioPermission) {
        PermissionRequiredContent(
            onRequestPermission = onRequestPermission,
            modifier = modifier
                .fillMaxSize()
                .padding(
                    start = 20.dp,
                    top = scaffoldContentPadding.calculateTopPadding() + 20.dp,
                    end = 20.dp,
                    bottom = bottomPadding(),
                ),
        )
        return
    }

    when (uiState) {
            HomeUiState.Empty -> EmptyLibraryContent(
                modifier = modifier
                    .fillMaxSize()
                    .padding(
                        start = 20.dp,
                        top = scaffoldContentPadding.calculateTopPadding() + 20.dp,
                        end = 20.dp,
                        bottom = bottomPadding(),
                    ),
            )
            is HomeUiState.Error -> ErrorContent(
                message = uiState.message,
                modifier = modifier.padding(
                    start = 20.dp,
                    top = scaffoldContentPadding.calculateTopPadding() + 20.dp,
                    end = 20.dp,
                ),
            )
            HomeUiState.Loading -> LoadingContent(
                modifier = modifier.padding(top = scaffoldContentPadding.calculateTopPadding()),
            )
            is HomeUiState.Success -> {
                val filteredTracks = uiState.tracks.filterByQuery(searchQuery)
                if (filteredTracks.isEmpty() && searchQuery.isNotBlank()) {
                    NoSearchResultsContent(
                        modifier = modifier.padding(
                            start = 20.dp,
                            top = scaffoldContentPadding.calculateTopPadding() + 20.dp,
                            end = 20.dp,
                        ),
                    )
                } else {
                    val selectionState = rememberTrackSelectionState()
                    val selection = selectionState.value
                    val selectedTracks = selection.selectedTracks(uiState.tracks)
                    TrackSelectionHost(
                        selection = selection,
                        onSelectionChange = { selectionState.value = it },
                        visibleIds = filteredTracks.map(Track::id),
                        selectedTracks = selectedTracks,
                        likedTrackIds = likedTrackIds,
                        currentTrackId = uiState.currentTrackId,
                        topInset = scaffoldContentPadding.calculateTopPadding(),
                        bottomInset = bottomPadding(),
                        onAction = onTrackSelectionAction,
                        modifier = modifier.fillMaxSize(),
                    ) {
                        HomeTrackList(
                            tracks = filteredTracks,
                            playbackQueue = uiState.tracks,
                            dailyPlaylistTrackCount = uiState.dailyPlaylistTrackCount,
                            canShuffleAll = uiState.canShuffleAll,
                            favoriteTrackCount = uiState.favoriteTracks.size,
                            recentlyPlayedTracks = uiState.recentlyPlayedTracks,
                            topTracks = uiState.topTracks,
                            currentTrackId = uiState.currentTrackId,
                            isCurrentTrackPlaying = uiState.isCurrentTrackPlaying,
                            onTrackSelected = onTrackSelected,
                            onPlayDailyPlaylist = onPlayDailyPlaylist,
                            onShuffleAll = onShuffleAll,
                            onOpenFavorites = onOpenFavorites,
                            modifier = Modifier.fillMaxSize(),
                            state = trackListState,
                            contentPadding = PaddingValues(
                                top = scaffoldContentPadding.calculateTopPadding() + 28.dp +
                                    if (selection.active) 64.dp else 0.dp,
                                bottom = bottomPadding() + 20.dp +
                                    if (selection.active) 72.dp else 0.dp,
                            ),
                            onTrackOptions = onTrackOptions,
                            selectedTrackIds = selection.selectedIds,
                            selectionActive = selection.active,
                            onTrackLongClick = { track ->
                                selectionState.value = selection.onTrackLongClick(track.id)
                            },
                            onRecentlyPlayedTrackSelected = { track ->
                                onTrackSelected(track, uiState.tracks)
                            },
                            onTopTrackSelected = onTopTrackSelected,
                            onNavigateToStatistics = onNavigateToStatistics,
                            showHighlights = searchQuery.isBlank(),
                            areFeaturedSectionsVisible = areFeaturedSectionsVisible,
                            onToggleFeaturedSections = {
                                areFeaturedSectionsVisible = !areFeaturedSectionsVisible
                            },
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
