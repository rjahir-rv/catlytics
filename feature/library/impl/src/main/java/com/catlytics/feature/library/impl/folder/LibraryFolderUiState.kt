package com.catlytics.feature.library.impl.folder

import com.catlytics.core.model.LibraryFolderContent

internal sealed interface LibraryFolderUiState {
    data object Loading : LibraryFolderUiState
    data object NotFound : LibraryFolderUiState
    data class Success(val content: LibraryFolderContent) : LibraryFolderUiState
    data class Error(val message: String) : LibraryFolderUiState
}
