package com.catlytics.feature.home.impl

import androidx.compose.foundation.layout.PaddingValues
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
import com.catlytics.core.domain.usecase.home.GenerateDailyPlaylistUseCase
import com.catlytics.core.domain.usecase.library.ObserveLibraryUseCase
import com.catlytics.core.domain.usecase.playback.ObserveNowPlayingUseCase
import com.catlytics.core.domain.usecase.playback.PlayShuffledQueueUseCase
import com.catlytics.core.domain.usecase.playback.PlayTrackUseCase
import com.catlytics.core.model.Track
import com.catlytics.core.model.TrackSelectionAction
import com.catlytics.feature.home.impl.R as HomeR
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal sealed interface DailyPlaylistUiState {
    data object Loading : DailyPlaylistUiState
    data object Empty : DailyPlaylistUiState
    data class Success(
        val tracks: List<Track>,
        val currentTrackId: String? = null,
        val isCurrentTrackPlaying: Boolean = false,
    ) : DailyPlaylistUiState
}

@HiltViewModel
internal class DailyPlaylistViewModel @Inject constructor(
    observeLibraryUseCase: ObserveLibraryUseCase,
    observeNowPlayingUseCase: ObserveNowPlayingUseCase,
    generateDailyPlaylistUseCase: GenerateDailyPlaylistUseCase,
    private val playTrackUseCase: PlayTrackUseCase,
    private val playShuffledQueueUseCase: PlayShuffledQueueUseCase,
) : ViewModel() {
    val uiState: StateFlow<DailyPlaylistUiState> = combine(
        observeLibraryUseCase()
            .catch { emit(emptyList()) }
            .map(generateDailyPlaylistUseCase::invoke),
        observeNowPlayingUseCase(),
    ) { dailyTracks, nowPlaying ->
        if (dailyTracks.isEmpty()) {
            DailyPlaylistUiState.Empty
        } else {
            DailyPlaylistUiState.Success(
                tracks = dailyTracks,
                currentTrackId = nowPlaying.trackId,
                isCurrentTrackPlaying = nowPlaying.isPlaying,
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DailyPlaylistUiState.Loading,
    )

    fun onTrackSelected(track: Track, queue: List<Track>) {
        viewModelScope.launch {
            playTrackUseCase(track, queue)
        }
    }

    fun onPlayAll() {
        val tracks = (uiState.value as? DailyPlaylistUiState.Success)?.tracks.orEmpty()
        val firstTrack = tracks.firstOrNull() ?: return
        viewModelScope.launch {
            playTrackUseCase(firstTrack, tracks)
        }
    }

    fun onShuffle() {
        val tracks = (uiState.value as? DailyPlaylistUiState.Success)?.tracks.orEmpty()
        if (tracks.size < MIN_SHUFFLE_TRACK_COUNT) return
        viewModelScope.launch {
            playShuffledQueueUseCase(tracks)
        }
    }
}

@Composable
internal fun DailyPlaylistRoute(
    modifier: Modifier = Modifier,
    onTrackOptions: (Track) -> Unit,
    likedTrackIds: Set<String> = emptySet(),
    onTrackSelectionAction: (TrackSelectionAction) -> Unit = {},
    bottomPadding: () -> Dp = { 0.dp },
    scaffoldContentPadding: PaddingValues = PaddingValues(0.dp),
    viewModel: DailyPlaylistViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    DailyPlaylistScreen(
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
internal fun DailyPlaylistScreen(
    modifier: Modifier = Modifier,
    uiState: DailyPlaylistUiState,
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
        DailyPlaylistUiState.Loading -> TrackCollectionLoading(modifier)

        DailyPlaylistUiState.Empty -> TrackCollectionEmpty(
            message = stringResource(HomeR.string.daily_playlist_empty_message),
            modifier = modifier,
        )

        is DailyPlaylistUiState.Success -> TrackCollectionContent(
            tracks = uiState.tracks,
            currentTrackId = uiState.currentTrackId,
            isCurrentTrackPlaying = uiState.isCurrentTrackPlaying,
            title = stringResource(HomeR.string.daily_playlist_title),
            summary = pluralStringResource(
                HomeR.plurals.daily_playlist_tracks_count,
                uiState.tracks.size,
                uiState.tracks.size,
            ),
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
