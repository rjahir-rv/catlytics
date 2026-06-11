package com.catlytics.feature.library.impl.album

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.catlytics.core.domain.usecase.ObserveAlbumContentUseCase
import com.catlytics.core.domain.usecase.PlayTrackUseCase
import com.catlytics.core.model.Track
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
internal class LibraryAlbumViewModel @Inject constructor(
    private val observeAlbumContentUseCase: ObserveAlbumContentUseCase,
    private val playTrackUseCase: PlayTrackUseCase,
) : ViewModel() {
    private val albumId = MutableStateFlow<String?>(null)

    val uiState = albumId
        .filterNotNull()
        .flatMapLatest(observeAlbumContentUseCase::invoke)
        .map { content ->
            content?.let(LibraryAlbumUiState::Success) ?: LibraryAlbumUiState.NotFound
        }
        .catch { error ->
            emit(
                LibraryAlbumUiState.Error(
                    error.message ?: "No se pudo cargar el contenido del álbum.",
                ),
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = LibraryAlbumUiState.Loading,
        )

    fun openAlbum(id: String) {
        albumId.value = id
    }

    fun playTrack(track: Track, queue: List<Track>) {
        viewModelScope.launch {
            playTrackUseCase(track, queue)
        }
    }
}
