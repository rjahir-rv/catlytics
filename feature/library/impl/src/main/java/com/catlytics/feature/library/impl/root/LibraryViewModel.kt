package com.catlytics.feature.library.impl.root

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.catlytics.core.domain.usecase.ObserveAlbumsUseCase
import com.catlytics.core.domain.usecase.ObserveLibraryFoldersUseCase
import com.catlytics.core.domain.usecase.RefreshLibraryUseCase
import com.catlytics.core.domain.usecase.SetFolderVisibilityUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.collections.emptyList

@HiltViewModel
internal class LibraryViewModel @Inject constructor(
    observeAlbumsUseCase: ObserveAlbumsUseCase,
    observeLibraryFoldersUseCase: ObserveLibraryFoldersUseCase,
    private val refreshLibraryUseCase: RefreshLibraryUseCase,
    private val setFolderVisibilityUseCase: SetFolderVisibilityUseCase,
) : ViewModel() {
    private val refreshError = MutableStateFlow<String?>(null)
    private val isRefreshing = MutableStateFlow(false)
    private var hasRequestedInitialRefresh = false

    val uiState: StateFlow<LibraryUiState> = combine(
        observeAlbumsUseCase().catch { emit(emptyList()) },
        observeLibraryFoldersUseCase().catch { emit(emptyList()) },
        refreshError,
        isRefreshing,
    ) { albums, folders, error, refreshing ->
        when {
            refreshing -> LibraryUiState.Loading
            error != null -> LibraryUiState.Error(error)
            albums.isEmpty() && folders.isEmpty() -> LibraryUiState.Empty
            else -> LibraryUiState.Success(albums = albums, folders = folders)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = LibraryUiState.Loading,
    )

    fun refreshLibraryOnce() {
        if (hasRequestedInitialRefresh) return
        hasRequestedInitialRefresh = true
        viewModelScope.launch {
            refreshError.value = null
            isRefreshing.value = true
            try {
                runCatching { refreshLibraryUseCase() }
                    .onFailure { error ->
                        refreshError.value = error.message
                            ?: "No se pudieron cargar las carpetas musicales."
                    }
            } finally {
                isRefreshing.value = false
            }
        }
    }

    fun setFolderVisible(folderId: String, visible: Boolean) {
        viewModelScope.launch {
            setFolderVisibilityUseCase(folderId, visible)
        }
    }
}
