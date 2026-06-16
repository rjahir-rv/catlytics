package com.catlytics.app.playback

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.catlytics.core.domain.usecase.playback.CycleRepeatModeUseCase
import com.catlytics.core.domain.usecase.playback.MoveQueueItemUseCase
import com.catlytics.core.domain.usecase.playback.ObservePlaybackStateUseCase
import com.catlytics.core.domain.usecase.playback.PlayQueueItemUseCase
import com.catlytics.core.domain.usecase.playback.RemoveQueueItemUseCase
import com.catlytics.core.domain.usecase.playback.RestorePlaybackSessionUseCase
import com.catlytics.core.domain.usecase.playback.SeekPlaybackUseCase
import com.catlytics.core.domain.usecase.playback.SkipPlaybackUseCase
import com.catlytics.core.domain.usecase.playback.ToggleShuffleUseCase
import com.catlytics.core.domain.usecase.playback.TogglePlaybackUseCase
import com.catlytics.core.domain.usecase.playlist.ObserveIsTrackLikedUseCase
import com.catlytics.core.domain.usecase.playlist.ToggleLikedTrackResult
import com.catlytics.core.domain.usecase.playlist.ToggleLikedTrackUseCase
import com.catlytics.core.model.PlaybackState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PlaybackViewModel @Inject constructor(
    observePlaybackStateUseCase: ObservePlaybackStateUseCase,
    observeIsTrackLikedUseCase: ObserveIsTrackLikedUseCase,
    private val togglePlaybackUseCase: TogglePlaybackUseCase,
    private val skipPlaybackUseCase: SkipPlaybackUseCase,
    private val seekPlaybackUseCase: SeekPlaybackUseCase,
    private val toggleShuffleUseCase: ToggleShuffleUseCase,
    private val cycleRepeatModeUseCase: CycleRepeatModeUseCase,
    private val playQueueItemUseCase: PlayQueueItemUseCase,
    private val moveQueueItemUseCase: MoveQueueItemUseCase,
    private val removeQueueItemUseCase: RemoveQueueItemUseCase,
    private val restorePlaybackSessionUseCase: RestorePlaybackSessionUseCase,
    private val toggleLikedTrackUseCase: ToggleLikedTrackUseCase,
) : ViewModel() {
    val playbackState: StateFlow<PlaybackState> = observePlaybackStateUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = PlaybackState(),
        )

    val isCurrentTrackLiked: StateFlow<Boolean> = playbackState
        .map { it.currentTrack?.id }
        .distinctUntilChanged()
        .flatMapLatest(observeIsTrackLikedUseCase::invoke)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = false,
        )

    init {
        viewModelScope.launch {
            restorePlaybackSessionUseCase()
        }
    }

    fun togglePlayback() {
        viewModelScope.launch {
            togglePlaybackUseCase()
        }
    }

    fun skipNext() {
        viewModelScope.launch {
            skipPlaybackUseCase.next()
        }
    }

    fun skipPrevious() {
        viewModelScope.launch {
            skipPlaybackUseCase.previous()
        }
    }

    fun seekTo(positionMillis: Long) {
        viewModelScope.launch {
            seekPlaybackUseCase(positionMillis)
        }
    }

    fun toggleShuffle() {
        viewModelScope.launch {
            toggleShuffleUseCase(!playbackState.value.isShuffleEnabled)
        }
    }

    fun cycleRepeatMode() {
        viewModelScope.launch {
            cycleRepeatModeUseCase(playbackState.value.repeatMode)
        }
    }

    fun playQueueItem(index: Int) {
        viewModelScope.launch {
            playQueueItemUseCase(index)
        }
    }

    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        viewModelScope.launch {
            moveQueueItemUseCase(fromIndex, toIndex)
        }
    }

    fun removeQueueItem(index: Int) {
        viewModelScope.launch {
            removeQueueItemUseCase(index)
        }
    }

    fun toggleCurrentTrackLiked(onResult: (ToggleLikedTrackResult) -> Unit) {
        val trackId = playbackState.value.currentTrack?.id ?: return
        viewModelScope.launch {
            onResult(toggleLikedTrackUseCase(trackId))
        }
    }
}
