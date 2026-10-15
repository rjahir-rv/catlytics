package com.catlytics.feature.library.impl.album

import com.catlytics.core.designsystem.text.UiText
import com.catlytics.core.model.AlbumContent
import com.catlytics.core.model.AlbumTrackSort

internal sealed interface LibraryAlbumUiState {
    data object Loading : LibraryAlbumUiState
    data object NotFound : LibraryAlbumUiState
    data class Success(
        val content: AlbumContent,
        val sort: AlbumTrackSort = AlbumTrackSort.TrackNumber,
    ) : LibraryAlbumUiState
    data class Error(val message: UiText) : LibraryAlbumUiState
}
