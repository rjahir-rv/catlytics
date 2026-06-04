package com.catlytics.feature.home.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.catlytics.core.domain.usecase.ObserveLibraryUseCase
import com.catlytics.core.domain.usecase.PlayTrackUseCase
import com.catlytics.core.domain.usecase.RefreshLibraryUseCase
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
    private val refreshLibraryUseCase: RefreshLibraryUseCase,
    private val playTrackUseCase: PlayTrackUseCase,
) : ViewModel() {
    private val refreshError = MutableStateFlow<String?>(null)
    private val isRefreshing = MutableStateFlow(false)

    val uiState: StateFlow<HomeUiState> = combine(
        observeLibraryUseCase()
            .catch { throwable -> emit(emptyList()) },
        refreshError,
        isRefreshing,
    ) { tracks, error, refreshing ->
        when {
            refreshing -> HomeUiState.Loading
            error != null -> HomeUiState.Error(error)
            tracks.isEmpty() -> HomeUiState.Empty
            else -> HomeUiState.Success(tracks)
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

    fun onTrackSelected(track: Track, queue: List<Track>) {
        viewModelScope.launch {
            playTrackUseCase(track, queue)
        }
    }
}
