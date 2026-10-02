package com.catlytics.feature.home.impl

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
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
import com.catlytics.core.designsystem.text.UiText
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
    onNavigateToRecentlyAdded: () -> Unit,
    hasAudioPermission: Boolean,
    onRequestPermission: () -> Unit,
    startupError: UiText? = null,
    onContentReady: () -> Unit = {},
    bottomPadding: () -> Dp = { 0.dp },
    scaffoldContentPadding: PaddingValues = PaddingValues(0.dp),
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    HomeScreen(
        uiState = uiState,
        startupError = startupError,
        searchQuery = searchQuery,
        hasAudioPermission = hasAudioPermission,
        onRequestPermission = onRequestPermission,
        onTrackSelected = viewModel::onTrackSelected,
        onTopTrackSelected = viewModel::onTopTrackSelected,
        onOpenDailyPlaylist = onNavigateToDailyPlaylist,
        onShuffleAll = viewModel::onShuffleAll,
        onOpenFavorites = onNavigateToFavorites,
        onOpenRecentlyAdded = onNavigateToRecentlyAdded,
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
    modifier: Modifier = Modifier,
    onTrackSelectionAction: (TrackSelectionAction) -> Unit = {},
    uiState: HomeUiState,
    startupError: UiText? = null,
    searchQuery: String,
    hasAudioPermission: Boolean,
    onRequestPermission: () -> Unit,
    onTrackSelected: (Track, List<Track>) -> Unit,
    onTrackOptions: (Track) -> Unit,
    likedTrackIds: Set<String> = emptySet(),
    onTopTrackSelected: (String) -> Unit = {},
    onOpenDailyPlaylist: () -> Unit = {},
    onShuffleAll: () -> Unit = {},
    onOpenFavorites: () -> Unit = {},
    onOpenRecentlyAdded: () -> Unit = {},
    onNavigateToStatistics: () -> Unit = {},
    onContentReady: () -> Unit = {},
    bottomPadding: () -> Dp = { 0.dp },
    scaffoldContentPadding: PaddingValues = PaddingValues(0.dp),
) {
    val displayedUiState = startupError?.let(HomeUiState::Error) ?: uiState

    LaunchedEffect(displayedUiState, hasAudioPermission) {
        if (!hasAudioPermission || displayedUiState != HomeUiState.Loading) {
            onContentReady()
        }
    }

    val trackListState = rememberSaveable(saver = LazyListState.Saver) {
        LazyListState()
    }
    var areFeaturedSectionsVisible by rememberSaveable { mutableStateOf(true) }

    if (!hasAudioPermission) {
        PermissionRequiredContent(
            onRequestPermission = onRequestPermission,
            modifier = modifier
                .fillMaxSize()
                .padding(
                    start = HomeDimens.ScreenPadding,
                    top = scaffoldContentPadding.calculateTopPadding() + HomeDimens.ScreenPadding,
                    end = HomeDimens.ScreenPadding,
                    bottom = bottomPadding(),
                ),
        )
        return
    }

    val stateKind = when (displayedUiState) {
        HomeUiState.Empty -> HomeStateKind.Empty
        is HomeUiState.Error -> HomeStateKind.Error
        HomeUiState.Loading -> HomeStateKind.Loading
        is HomeUiState.Success -> HomeStateKind.Success
    }
    Crossfade(
        targetState = stateKind,
        animationSpec = tween(STATE_FADE_MILLIS),
        label = "homeState",
    ) { _ ->
        when (displayedUiState) {
            HomeUiState.Empty -> EmptyLibraryContent(
                modifier = modifier
                    .fillMaxSize()
                    .padding(
                        start = HomeDimens.ScreenPadding,
                        top = scaffoldContentPadding.calculateTopPadding() + HomeDimens.ScreenPadding,
                        end = HomeDimens.ScreenPadding,
                        bottom = bottomPadding(),
                    ),
            )
            is HomeUiState.Error -> ErrorContent(
                message = displayedUiState.message,
                modifier = modifier.padding(
                    start = HomeDimens.ScreenPadding,
                    top = scaffoldContentPadding.calculateTopPadding() + HomeDimens.ScreenPadding,
                    end = HomeDimens.ScreenPadding,
                ),
            )
            HomeUiState.Loading -> LoadingContent(
                modifier = modifier.padding(top = scaffoldContentPadding.calculateTopPadding()),
            )
            is HomeUiState.Success -> {
                val filteredTracks = displayedUiState.tracks.filterByQuery(searchQuery)
                if (filteredTracks.isEmpty() && searchQuery.isNotBlank()) {
                    NoSearchResultsContent(
                        modifier = modifier.padding(
                            start = HomeDimens.ScreenPadding,
                            top = scaffoldContentPadding.calculateTopPadding() + HomeDimens.ScreenPadding,
                            end = HomeDimens.ScreenPadding,
                        ),
                    )
                } else {
                    val selectionState = rememberTrackSelectionState()
                    val selection = selectionState.value
                    val selectedTracks = selection.selectedTracks(displayedUiState.tracks)
                    TrackSelectionHost(
                        selection = selection,
                        onSelectionChange = { selectionState.value = it },
                        visibleIds = filteredTracks.map(Track::id),
                        selectedTracks = selectedTracks,
                        likedTrackIds = likedTrackIds,
                        currentTrackId = displayedUiState.currentTrackId,
                        topInset = scaffoldContentPadding.calculateTopPadding(),
                        bottomInset = bottomPadding(),
                        onAction = onTrackSelectionAction,
                        modifier = modifier.fillMaxSize(),
                    ) {
                        HomeTrackList(
                            tracks = filteredTracks,
                            playbackQueue = displayedUiState.tracks,
                            dailyPlaylistTrackCount = displayedUiState.dailyPlaylistTrackCount,
                            canShuffleAll = displayedUiState.canShuffleAll,
                            favoriteTrackCount = displayedUiState.favoriteTracks.size,
                            recentlyAddedTrackCount = displayedUiState.recentlyAddedTracks.size,
                            newTrackIds = displayedUiState.newTrackIds,
                            showRecommendedPlaylists = displayedUiState.showRecommendedPlaylists,
                            recentlyPlayedTracks = displayedUiState.recentlyPlayedTracks,
                            topTracks = displayedUiState.topTracks,
                            currentTrackId = displayedUiState.currentTrackId,
                            isCurrentTrackPlaying = displayedUiState.isCurrentTrackPlaying,
                            onTrackSelected = onTrackSelected,
                            onOpenDailyPlaylist = onOpenDailyPlaylist,
                            onShuffleAll = onShuffleAll,
                            onOpenFavorites = onOpenFavorites,
                            onOpenRecentlyAdded = onOpenRecentlyAdded,
                            modifier = Modifier.fillMaxSize(),
                            state = trackListState,
                            contentPadding = PaddingValues(
                                top = scaffoldContentPadding.calculateTopPadding() + HomeDimens.TopContentInset +
                                    if (selection.active) 64.dp else 0.dp,
                                bottom = bottomPadding() + HomeDimens.ScreenPadding +
                                    if (selection.active) 72.dp else 0.dp,
                            ),
                            onTrackOptions = onTrackOptions,
                            selectedTrackIds = selection.selectedIds,
                            selectionActive = selection.active,
                            onTrackLongClick = { track ->
                                selectionState.value = selection.onTrackLongClick(track.id)
                            },
                            onRecentlyPlayedTrackSelected = { track ->
                                onTrackSelected(track, displayedUiState.tracks)
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
}

private enum class HomeStateKind { Loading, Empty, Error, Success }

private const val STATE_FADE_MILLIS = 220

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
