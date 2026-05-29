package com.catlytics.core.domain.usecase

import com.catlytics.core.domain.repository.PlaylistRepository

class ObservePlaylistsUseCase(
    private val playlistRepository: PlaylistRepository,
) {
    operator fun invoke() = playlistRepository.observePlaylists()
}
