package com.catlytics.feature.library.impl.artist

import com.catlytics.core.model.ArtistContent

internal sealed interface LibraryArtistUiState {
    data object Loading : LibraryArtistUiState
    data object NotFound : LibraryArtistUiState
    data class Success(
        val content: ArtistContent,
        val searchQuery: String = "",
    ) : LibraryArtistUiState
    data class Error(val message: String) : LibraryArtistUiState
}
