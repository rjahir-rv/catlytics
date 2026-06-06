package com.catlytics.app.playback

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.catlytics.core.domain.usecase.CycleRepeatModeUseCase
import com.catlytics.core.domain.usecase.ObservePlaybackStateUseCase
import com.catlytics.core.domain.usecase.RestorePlaybackSessionUseCase
import com.catlytics.core.domain.usecase.SeekPlaybackUseCase
import com.catlytics.core.domain.usecase.SkipPlaybackUseCase
import com.catlytics.core.domain.usecase.ToggleShuffleUseCase
import com.catlytics.core.domain.usecase.TogglePlaybackUseCase
import com.catlytics.core.model.PlaybackState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class PlaybackViewModel @Inject constructor(
    observePlaybackStateUseCase: ObservePlaybackStateUseCase,
    private val togglePlaybackUseCase: TogglePlaybackUseCase,
    private val skipPlaybackUseCase: SkipPlaybackUseCase,
    private val seekPlaybackUseCase: SeekPlaybackUseCase,
    private val toggleShuffleUseCase: ToggleShuffleUseCase,
    private val cycleRepeatModeUseCase: CycleRepeatModeUseCase,
    private val restorePlaybackSessionUseCase: RestorePlaybackSessionUseCase,
) : ViewModel() {
    val playbackState: StateFlow<PlaybackState> = observePlaybackStateUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = PlaybackState(),
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
}
