package com.catlytics.feature.library.impl.album

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.catlytics.core.designsystem.text.UiText
import com.catlytics.core.domain.usecase.library.ObserveAlbumContentUseCase
import com.catlytics.core.domain.usecase.library.ObserveLibraryTrackUseCase
import com.catlytics.core.domain.usecase.playback.PlayShuffledQueueUseCase
import com.catlytics.core.domain.usecase.playback.PlayTrackUseCase
import com.catlytics.core.model.AlbumContent
import com.catlytics.core.model.Track
import com.catlytics.feature.library.impl.R
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
internal class LibraryAlbumViewModel @Inject constructor(
    private val observeAlbumContentUseCase: ObserveAlbumContentUseCase,
    private val playTrackUseCase: PlayTrackUseCase,
    private val playShuffledQueueUseCase: PlayShuffledQueueUseCase,
    private val observeLibraryTrack: ObserveLibraryTrackUseCase,
) : ViewModel() {
    private val albumId = MutableStateFlow<String?>(null)
    private var lastContent: AlbumContent? = null

    val uiState = albumId
        .filterNotNull()
        .flatMapLatest { id ->
            observeAlbumContentUseCase(id).transformLatest { content ->
                if (content != null) {
                    lastContent = content
                    emit(LibraryAlbumUiState.Success(content))
                    return@transformLatest
                }
                val newAlbumId = lastContent?.let { findMovedAlbumId(it, id) }
                if (newAlbumId != null) {
                    albumId.value = newAlbumId
                } else {
                    emit(LibraryAlbumUiState.NotFound)
                }
            }
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
        if (albumId.value == null) albumId.value = id
    }

    private suspend fun findMovedAlbumId(previous: AlbumContent, oldAlbumId: String): String? =
        previous.tracks.firstNotNullOfOrNull { track ->
            observeLibraryTrack(track.id).first()?.albumId?.takeIf { it != oldAlbumId }
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
