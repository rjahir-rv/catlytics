package com.catlytics.feature.home.impl

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.catlytics.core.designsystem.R
import com.catlytics.core.domain.repository.HomePreferencesRepository
import com.catlytics.core.domain.usecase.library.ObserveRecentlyAddedTracksUseCase
import com.catlytics.core.domain.usecase.playback.ObserveNowPlayingUseCase
import com.catlytics.core.domain.usecase.playback.PlayShuffledQueueUseCase
import com.catlytics.core.domain.usecase.playback.PlayTrackUseCase
import com.catlytics.core.model.RecentAddedWindow
import com.catlytics.core.model.Track
import com.catlytics.core.model.TrackSelectionAction
import com.catlytics.feature.home.impl.R as HomeR
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal sealed interface RecentlyAddedUiState {
    data object Loading : RecentlyAddedUiState
    data class Empty(val windowDays: Int) : RecentlyAddedUiState
    data class Success(
        val tracks: List<Track>,
        val currentTrackId: String? = null,
        val isCurrentTrackPlaying: Boolean = false,
        val windowDays: Int = RecentAddedWindow.Days21.days,
        val showNewTrackBadge: Boolean = true,
    ) : RecentlyAddedUiState
}

@HiltViewModel
internal class RecentlyAddedViewModel @Inject constructor(
    observeRecentlyAddedTracksUseCase: ObserveRecentlyAddedTracksUseCase,
    observeNowPlayingUseCase: ObserveNowPlayingUseCase,
    homePreferencesRepository: HomePreferencesRepository,
    private val playTrackUseCase: PlayTrackUseCase,
    private val playShuffledQueueUseCase: PlayShuffledQueueUseCase,
) : ViewModel() {
    val uiState: StateFlow<RecentlyAddedUiState> = combine(
        observeRecentlyAddedTracksUseCase().catch { emit(emptyList()) },
        observeNowPlayingUseCase(),
        homePreferencesRepository.observeHomeRecommendationsSettings(),
    ) { recentlyAddedTracks, nowPlaying, homeRecommendationsSettings ->
        if (recentlyAddedTracks.isEmpty()) {
            RecentlyAddedUiState.Empty(homeRecommendationsSettings.recentAddedWindow.days)
        } else {
            RecentlyAddedUiState.Success(
                tracks = recentlyAddedTracks,
                currentTrackId = nowPlaying.trackId,
                isCurrentTrackPlaying = nowPlaying.isPlaying,
                windowDays = homeRecommendationsSettings.recentAddedWindow.days,
                showNewTrackBadge = homeRecommendationsSettings.showNewTrackBadge,
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = RecentlyAddedUiState.Loading,
    )

    fun onTrackSelected(track: Track, queue: List<Track>) {
        viewModelScope.launch {
            playTrackUseCase(track, queue)
        }
    }

    fun onPlayAll() {
        val tracks = (uiState.value as? RecentlyAddedUiState.Success)?.tracks.orEmpty()
        val firstTrack = tracks.firstOrNull() ?: return
        viewModelScope.launch {
            playTrackUseCase(firstTrack, tracks)
        }
    }

    fun onShuffle() {
        val tracks = (uiState.value as? RecentlyAddedUiState.Success)?.tracks.orEmpty()
        if (tracks.size < MIN_SHUFFLE_TRACK_COUNT) return
        viewModelScope.launch {
            playShuffledQueueUseCase(tracks)
        }
    }
}

@Composable
internal fun RecentlyAddedRoute(
    modifier: Modifier = Modifier,
    onTrackOptions: (Track) -> Unit,
    likedTrackIds: Set<String> = emptySet(),
    onTrackSelectionAction: (TrackSelectionAction) -> Unit = {},
    bottomPadding: () -> Dp = { 0.dp },
    scaffoldContentPadding: PaddingValues = PaddingValues(0.dp),
    viewModel: RecentlyAddedViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    RecentlyAddedScreen(
        uiState = uiState,
        onTrackSelected = viewModel::onTrackSelected,
        onPlayAll = viewModel::onPlayAll,
        onShuffle = viewModel::onShuffle,
        onTrackOptions = onTrackOptions,
        likedTrackIds = likedTrackIds,
        onTrackSelectionAction = onTrackSelectionAction,
        bottomPadding = bottomPadding,
        scaffoldContentPadding = scaffoldContentPadding,
        modifier = modifier,
    )
}

@Composable
internal fun RecentlyAddedScreen(
    modifier: Modifier = Modifier,
    uiState: RecentlyAddedUiState,
    onTrackSelected: (Track, List<Track>) -> Unit,
    onTrackOptions: (Track) -> Unit,
    likedTrackIds: Set<String> = emptySet(),
    onTrackSelectionAction: (TrackSelectionAction) -> Unit = {},
    onPlayAll: () -> Unit = {},
    onShuffle: () -> Unit = {},
    bottomPadding: () -> Dp = { 0.dp },
    scaffoldContentPadding: PaddingValues = PaddingValues(0.dp),
) {
    when (uiState) {
        RecentlyAddedUiState.Loading -> TrackCollectionLoading(modifier)

        is RecentlyAddedUiState.Empty -> TrackCollectionEmpty(
            message = stringResource(HomeR.string.recently_added_empty_message, uiState.windowDays),
            modifier = modifier,
        )

        is RecentlyAddedUiState.Success -> TrackCollectionContent(
            tracks = uiState.tracks,
            currentTrackId = uiState.currentTrackId,
            isCurrentTrackPlaying = uiState.isCurrentTrackPlaying,
            summary = pluralStringResource(
                HomeR.plurals.recently_added_tracks_count,
                uiState.tracks.size,
                uiState.tracks.size,
                uiState.windowDays,
            ),
            badgeLabel = if (uiState.showNewTrackBadge) {
                stringResource(HomeR.string.home_new_track_badge)
            } else {
                null
            },
            onTrackSelected = onTrackSelected,
            onTrackOptions = onTrackOptions,
            onPlayAll = onPlayAll,
            onShuffle = onShuffle,
            likedTrackIds = likedTrackIds,
            onTrackSelectionAction = onTrackSelectionAction,
            bottomPadding = bottomPadding,
            scaffoldContentPadding = scaffoldContentPadding,
            modifier = modifier,
        )
    }
}
