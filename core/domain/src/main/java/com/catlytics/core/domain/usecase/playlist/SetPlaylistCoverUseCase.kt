package com.catlytics.core.domain.usecase.playlist

import com.catlytics.core.domain.repository.PlaylistRepository

class SetPlaylistCoverUseCase(
    private val repository: PlaylistRepository,
) {
    suspend operator fun invoke(playlistId: String, artworkUri: String?) =
        repository.setPlaylistArtwork(playlistId, artworkUri)
}
