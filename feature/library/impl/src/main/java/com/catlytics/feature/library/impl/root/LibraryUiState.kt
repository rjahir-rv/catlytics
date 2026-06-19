package com.catlytics.feature.library.impl.root

import com.catlytics.core.model.Album
import com.catlytics.core.model.ArtistSummary
import com.catlytics.core.model.ArtistViewMode
import com.catlytics.core.model.LibraryFolder
import com.catlytics.core.model.SortDirection

internal sealed interface LibraryUiState {
    data object Loading : LibraryUiState
    data object Empty : LibraryUiState
    data class Success(
        val albums: List<Album>,
        val artists: List<ArtistSummary>,
        val artistViewMode: ArtistViewMode,
        val sortDirection: SortDirection,
        val folders: List<LibraryFolder>,
    ) : LibraryUiState
    data class Error(val message: String) : LibraryUiState
}
