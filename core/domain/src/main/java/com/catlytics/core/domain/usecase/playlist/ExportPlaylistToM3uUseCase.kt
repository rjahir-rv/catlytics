package com.catlytics.core.domain.usecase.playlist

import com.catlytics.core.domain.repository.PlaylistExportRepository
import javax.inject.Inject

class ExportPlaylistToM3uUseCase @Inject constructor(
    private val repository: PlaylistExportRepository,
) {
    suspend operator fun invoke(
        playlistId: String,
        uri: String,
    ): Result<Unit> = repository.exportPlaylistToM3uUri(playlistId, uri)
}
