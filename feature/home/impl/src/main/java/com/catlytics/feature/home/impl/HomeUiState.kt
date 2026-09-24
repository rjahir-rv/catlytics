package com.catlytics.feature.home.impl

import com.catlytics.core.model.Track
import com.catlytics.core.model.TopTrack

internal sealed interface HomeUiState {
    data object Loading : HomeUiState
    data object Empty : HomeUiState
    data class Success(
        val tracks: List<Track>,
        val dailyPlaylistTrackCount: Int = 0,
        val canShuffleAll: Boolean = false,
        val favoriteTracks: List<Track> = emptyList(),
        val recentlyPlayedTracks: List<Track> = emptyList(),
        val recentlyAddedTracks: List<Track> = emptyList(),
        val topTracks: List<TopTrack> = emptyList(),
        val currentTrackId: String? = null,
        val isCurrentTrackPlaying: Boolean = false,
        val showRecommendedPlaylists: Boolean = true,
        val showNewTrackBadge: Boolean = true,
        val newTrackIds: Set<String> = if (showNewTrackBadge) {
            recentlyAddedTracks.mapTo(mutableSetOf(), Track::id)
        } else {
            emptySet()
        },
    ) : HomeUiState
    data class Error(val message: String) : HomeUiState
}
