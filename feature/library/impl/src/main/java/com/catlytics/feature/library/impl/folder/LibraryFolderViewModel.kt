package com.catlytics.feature.library.impl.folder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.catlytics.core.designsystem.text.UiText
import com.catlytics.core.domain.usecase.library.ObserveFolderContentUseCase
import com.catlytics.core.domain.usecase.playback.PlayTrackUseCase
import com.catlytics.core.model.Track
import com.catlytics.feature.library.impl.R
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
internal class LibraryFolderViewModel @Inject constructor(
    private val observeFolderContentUseCase: ObserveFolderContentUseCase,
    private val playTrackUseCase: PlayTrackUseCase,
) : ViewModel() {
    private val folderId = MutableStateFlow<String?>(null)

    val uiState = folderId
        .filterNotNull()
        .flatMapLatest(observeFolderContentUseCase::invoke)
        .map { content ->
            content?.let(LibraryFolderUiState::Success) ?: LibraryFolderUiState.NotFound
        }
        .catch { error ->
            emit(
                LibraryFolderUiState.Error(
                    UiText.Resource(R.string.library_folder_load_error),
                ),
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = LibraryFolderUiState.Loading,
        )

    fun openFolder(id: String) {
        folderId.value = id
    }

    fun playTrack(track: Track, queue: List<Track>) {
        viewModelScope.launch {
            playTrackUseCase(track, queue)
        }
    }
}
