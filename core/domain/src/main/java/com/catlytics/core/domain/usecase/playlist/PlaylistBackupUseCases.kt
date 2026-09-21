package com.catlytics.core.domain.usecase.playlist

import com.catlytics.core.domain.repository.PlaylistBackupRepository
import com.catlytics.core.model.PlaylistBackupPreview
import com.catlytics.core.model.PlaylistBackupSummary
import com.catlytics.core.model.PlaylistExportResult
import com.catlytics.core.model.PlaylistImportResult
import com.catlytics.core.model.StatisticsImportMode
import kotlinx.coroutines.flow.Flow

class ObservePlaylistBackupSummaryUseCase(
    private val repository: PlaylistBackupRepository,
) {
    operator fun invoke(): Flow<PlaylistBackupSummary> = repository.observeLocalSummary()
}

class ExportPlaylistsBackupUseCase(
    private val repository: PlaylistBackupRepository,
) {
    suspend operator fun invoke(
        uri: String,
        appVersion: String,
    ): Result<PlaylistExportResult> = repository.exportToUri(uri, appVersion)
}

class PreviewPlaylistsBackupUseCase(
    private val repository: PlaylistBackupRepository,
) {
    suspend operator fun invoke(uri: String): Result<PlaylistBackupPreview> =
        repository.previewFromUri(uri)
}

class ImportPlaylistsBackupUseCase(
    private val repository: PlaylistBackupRepository,
) {
    suspend operator fun invoke(
        uri: String,
        mode: StatisticsImportMode = StatisticsImportMode.Merge,
    ): Result<PlaylistImportResult> = repository.importFromUri(uri, mode)
}

class ExportPlaylistToM3uUseCase(
    private val repository: PlaylistBackupRepository,
) {
    suspend operator fun invoke(
        playlistId: String,
        uri: String,
    ): Result<Unit> = repository.exportPlaylistToM3uUri(playlistId, uri)
}
