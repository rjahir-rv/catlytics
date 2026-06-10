package com.catlytics.feature.library.impl.root

import com.catlytics.core.model.LibraryFolder

internal sealed interface LibraryUiState {
    data object Loading : LibraryUiState
    data object Empty : LibraryUiState
    data class Success(val folders: List<LibraryFolder>) : LibraryUiState
    data class Error(val message: String) : LibraryUiState
}
