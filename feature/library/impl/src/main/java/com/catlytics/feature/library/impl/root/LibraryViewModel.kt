package com.catlytics.feature.library.impl.root

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.catlytics.core.domain.usecase.library.ObserveAlbumsUseCase
import com.catlytics.core.domain.usecase.library.ObserveArtistsUseCase
import com.catlytics.core.domain.usecase.library.ObserveArtistViewModeUseCase
import com.catlytics.core.domain.usecase.library.ObserveLibraryFoldersUseCase
import com.catlytics.core.domain.usecase.library.ObserveLibrarySortDirectionUseCase
import com.catlytics.core.domain.usecase.library.RefreshLibraryUseCase
import com.catlytics.core.domain.usecase.library.SetFolderVisibilityUseCase
import com.catlytics.core.domain.usecase.library.SetArtistViewModeUseCase
import com.catlytics.core.domain.usecase.library.SetLibrarySortDirectionUseCase
import com.catlytics.core.model.ArtistViewMode
import com.catlytics.core.model.SortDirection
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
    observeArtistsUseCase: ObserveArtistsUseCase,
    observeArtistViewModeUseCase: ObserveArtistViewModeUseCase,
    observeLibraryFoldersUseCase: ObserveLibraryFoldersUseCase,
    observeLibrarySortDirectionUseCase: ObserveLibrarySortDirectionUseCase,
    private val refreshLibraryUseCase: RefreshLibraryUseCase,
    private val setFolderVisibilityUseCase: SetFolderVisibilityUseCase,
    private val setArtistViewModeUseCase: SetArtistViewModeUseCase,
    private val setLibrarySortDirectionUseCase: SetLibrarySortDirectionUseCase,
) : ViewModel() {
    private val refreshError = MutableStateFlow<String?>(null)
    private val isRefreshing = MutableStateFlow(false)
    private var hasRequestedInitialRefresh = false

    private val libraryContent = combine(
        observeAlbumsUseCase().catch { emit(emptyList()) },
        observeArtistsUseCase().catch { emit(emptyList()) },
        observeArtistViewModeUseCase().catch { emit(ArtistViewMode.List) },
        observeLibraryFoldersUseCase().catch { emit(emptyList()) },
        observeLibrarySortDirectionUseCase().catch { emit(SortDirection.Ascending) },
    ) { albums, artists, artistViewMode, folders, sortDirection ->
        LibraryContent(
            albums = albums,
            artists = artists,
            artistViewMode = artistViewMode,
            sortDirection = sortDirection,
            folders = folders,
        )
    }

    val uiState: StateFlow<LibraryUiState> = combine(
        libraryContent,
        refreshError,
        isRefreshing,
    ) { content, error, refreshing ->
        when {
            refreshing -> LibraryUiState.Loading
            error != null -> LibraryUiState.Error(error)
            content.albums.isEmpty() &&
                content.artists.isEmpty() &&
                content.folders.isEmpty() -> LibraryUiState.Empty
            else -> LibraryUiState.Success(
                albums = content.albums,
                artists = content.artists,
                artistViewMode = content.artistViewMode,
                sortDirection = content.sortDirection,
                folders = content.folders,
            )
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

    fun setArtistViewMode(viewMode: ArtistViewMode) {
        viewModelScope.launch {
            setArtistViewModeUseCase(viewMode)
        }
    }

    fun setSortDirection(direction: SortDirection) {
        viewModelScope.launch {
            setLibrarySortDirectionUseCase(direction)
        }
    }
}

private data class LibraryContent(
    val albums: List<com.catlytics.core.model.Album>,
    val artists: List<com.catlytics.core.model.ArtistSummary>,
    val artistViewMode: ArtistViewMode,
    val sortDirection: SortDirection,
    val folders: List<com.catlytics.core.model.LibraryFolder>,
)
