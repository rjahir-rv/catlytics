package com.catlytics.feature.library.impl.album

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.catlytics.core.designsystem.text.UiText
import com.catlytics.core.domain.usecase.library.ObserveAlbumContentUseCase
import com.catlytics.core.domain.usecase.playback.PlayShuffledQueueUseCase
import com.catlytics.core.domain.usecase.playback.PlayTrackUseCase
import com.catlytics.core.model.Track
import com.catlytics.feature.library.impl.R
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
    private val playShuffledQueueUseCase: PlayShuffledQueueUseCase,
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
                    UiText.Resource(R.string.library_album_load_error),
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

    fun playShuffled(queue: List<Track>) {
        viewModelScope.launch {
            playShuffledQueueUseCase(queue)
        }
    }
}
