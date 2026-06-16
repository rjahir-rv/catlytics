package com.catlytics.feature.home.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.catlytics.core.domain.usecase.library.ObserveLibraryUseCase
import com.catlytics.core.domain.usecase.library.RefreshLibraryUseCase
import com.catlytics.core.domain.usecase.playback.ObservePlaybackStateUseCase
import com.catlytics.core.domain.usecase.playback.PlayTrackUseCase
import com.catlytics.core.domain.usecase.playlist.ObservePlaylistsUseCase
import com.catlytics.core.domain.usecase.playlist.ToggleLikedTrackResult
import com.catlytics.core.domain.usecase.playlist.ToggleLikedTrackUseCase
import com.catlytics.core.model.LIKED_PLAYLIST_ID
import com.catlytics.core.model.Track
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
internal class HomeViewModel @Inject constructor(
    observeLibraryUseCase: ObserveLibraryUseCase,
    observePlaybackStateUseCase: ObservePlaybackStateUseCase,
    observePlaylistsUseCase: ObservePlaylistsUseCase,
    private val refreshLibraryUseCase: RefreshLibraryUseCase,
    private val playTrackUseCase: PlayTrackUseCase,
    private val toggleLikedTrackUseCase: ToggleLikedTrackUseCase,
) : ViewModel() {
    private val refreshError = MutableStateFlow<String?>(null)
    private val isRefreshing = MutableStateFlow(false)
    private var hasRequestedInitialRefresh = false

    val uiState: StateFlow<HomeUiState> = combine(
        observeLibraryUseCase()
            .catch { throwable -> emit(emptyList()) },
        observePlaybackStateUseCase(),
        observePlaylistsUseCase(),
        refreshError,
        isRefreshing,
    ) { tracks, playbackState, playlists, error, refreshing ->
        when {
            refreshing -> HomeUiState.Loading
            error != null -> HomeUiState.Error(error)
            tracks.isEmpty() -> HomeUiState.Empty
            else -> HomeUiState.Success(
                tracks = tracks,
                currentTrackId = playbackState.currentTrack?.id,
                likedTrackIds = playlists
                    .firstOrNull { it.id == LIKED_PLAYLIST_ID }
                    ?.trackIds
                    .orEmpty()
                    .toSet(),
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState.Loading,
    )

    fun refreshLibrary() {
        viewModelScope.launch {
            refreshError.value = null
            isRefreshing.value = true
            try {
                runCatching {
                    refreshLibraryUseCase()
                }.onFailure { throwable ->
                    refreshError.update {
                        throwable.message ?: "No se pudo cargar la biblioteca musical."
                    }
                }
            } finally {
                isRefreshing.value = false
            }
        }
    }

    fun refreshLibraryOnce() {
        if (hasRequestedInitialRefresh) return
        hasRequestedInitialRefresh = true
        refreshLibrary()
    }

    fun onTrackSelected(track: Track, queue: List<Track>) {
        viewModelScope.launch {
            playTrackUseCase(track, queue)
        }
    }

    fun toggleTrackLiked(trackId: String, onResult: (ToggleLikedTrackResult) -> Unit) {
        viewModelScope.launch {
            onResult(toggleLikedTrackUseCase(trackId))
        }
    }
}
