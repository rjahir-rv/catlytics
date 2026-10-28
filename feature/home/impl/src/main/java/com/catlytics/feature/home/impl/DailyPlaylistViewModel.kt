package com.catlytics.feature.home.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.catlytics.core.domain.usecase.home.GenerateDailyPlaylistUseCase
import com.catlytics.core.domain.usecase.library.ObserveLibraryUseCase
import com.catlytics.core.domain.usecase.playback.ObserveNowPlayingUseCase
import com.catlytics.core.domain.usecase.playback.PlayShuffledQueueUseCase
import com.catlytics.core.domain.usecase.playback.PlayTrackUseCase
import com.catlytics.core.model.Track
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

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
