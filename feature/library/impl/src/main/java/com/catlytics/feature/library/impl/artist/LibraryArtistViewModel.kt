package com.catlytics.feature.library.impl.artist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.catlytics.core.domain.usecase.library.ObserveArtistContentUseCase
import com.catlytics.core.domain.usecase.playback.PlayTrackUseCase
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
internal class LibraryArtistViewModel @Inject constructor(
    private val observeArtistContentUseCase: ObserveArtistContentUseCase,
    private val playTrackUseCase: PlayTrackUseCase,
) : ViewModel() {
    private val artistId = MutableStateFlow<String?>(null)

    val uiState = artistId
        .filterNotNull()
        .flatMapLatest(observeArtistContentUseCase::invoke)
        .map { content ->
            content?.let(LibraryArtistUiState::Success) ?: LibraryArtistUiState.NotFound
        }
        .catch { error ->
            emit(
                LibraryArtistUiState.Error(
                    error.message ?: "No se pudo cargar el contenido del artista.",
                ),
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = LibraryArtistUiState.Loading,
        )

    fun openArtist(id: String) {
        artistId.value = id
    }

    fun playTrack(track: Track, queue: List<Track>) {
        viewModelScope.launch {
            playTrackUseCase(track, queue)
        }
    }
}
