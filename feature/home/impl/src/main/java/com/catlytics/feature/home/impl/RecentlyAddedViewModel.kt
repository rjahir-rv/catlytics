package com.catlytics.feature.home.impl

import androidx.compose.foundation.layout.size
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.catlytics.core.domain.repository.HomePreferencesRepository
import com.catlytics.core.domain.usecase.library.ObserveRecentlyAddedTracksUseCase
import com.catlytics.core.domain.usecase.playback.ObserveNowPlayingUseCase
import com.catlytics.core.domain.usecase.playback.PlayShuffledQueueUseCase
import com.catlytics.core.domain.usecase.playback.PlayTrackUseCase
import com.catlytics.core.model.RecentAddedWindow
import com.catlytics.core.model.Track
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

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
