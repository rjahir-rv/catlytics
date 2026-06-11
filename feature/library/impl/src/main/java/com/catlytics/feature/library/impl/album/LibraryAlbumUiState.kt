package com.catlytics.feature.library.impl.album

import com.catlytics.core.model.AlbumContent

internal sealed interface LibraryAlbumUiState {
    data object Loading : LibraryAlbumUiState
    data object NotFound : LibraryAlbumUiState
    data class Success(val content: AlbumContent) : LibraryAlbumUiState
    data class Error(val message: String) : LibraryAlbumUiState
}
