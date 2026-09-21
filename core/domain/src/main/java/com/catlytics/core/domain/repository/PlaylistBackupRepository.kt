package com.catlytics.core.domain.repository

import com.catlytics.core.model.PlaylistBackupPreview
import com.catlytics.core.model.PlaylistBackupSummary
import com.catlytics.core.model.PlaylistExportResult
import com.catlytics.core.model.PlaylistImportResult
import com.catlytics.core.model.StatisticsImportMode
import kotlinx.coroutines.flow.Flow

/**
 * Interface for exporting and importing playlists and favorites to a user document.
 */
interface PlaylistBackupRepository {
    fun observeLocalSummary(): Flow<PlaylistBackupSummary>

    suspend fun exportToUri(uri: String, appVersion: String): Result<PlaylistExportResult>

    suspend fun previewFromUri(uri: String): Result<PlaylistBackupPreview>

    suspend fun importFromUri(
        uri: String,
        mode: StatisticsImportMode,
    ): Result<PlaylistImportResult>

    suspend fun exportPlaylistToM3uUri(playlistId: String, uri: String): Result<Unit>
}
