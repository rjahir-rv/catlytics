package com.catlytics.feature.home.impl

import com.catlytics.core.model.Track

internal sealed interface HomeUiState {
    data object Loading : HomeUiState
    data object Empty : HomeUiState
    data class Success(
        val tracks: List<Track>,
        val currentTrackId: String? = null,
        val likedTrackIds: Set<String> = emptySet(),
    ) : HomeUiState
    data class Error(val message: String) : HomeUiState
}
