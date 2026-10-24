package com.catlytics.core.domain.usecase.playlist

import com.catlytics.core.domain.repository.PlaylistRepository
import javax.inject.Inject

class ObservePlaylistsUseCase @Inject constructor(
    private val playlistRepository: PlaylistRepository,
) {
    operator fun invoke() = playlistRepository.observePlaylists()
}
