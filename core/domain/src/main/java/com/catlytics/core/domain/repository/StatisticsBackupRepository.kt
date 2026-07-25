package com.catlytics.core.domain.repository

import com.catlytics.core.model.StatisticsBackupPreview
import com.catlytics.core.model.StatisticsBackupSummary
import com.catlytics.core.model.StatisticsExportResult
import com.catlytics.core.model.StatisticsImportMode
import com.catlytics.core.model.StatisticsImportResult
import kotlinx.coroutines.flow.Flow

/**
 * Export / import of listening statistics to a user-chosen document (SAF Uri as string).
 * Domain stays free of Android types; the data layer resolves content Uris.
 */
interface StatisticsBackupRepository {
    fun observeLocalSummary(): Flow<StatisticsBackupSummary>

    suspend fun exportToUri(uri: String, appVersion: String): Result<StatisticsExportResult>

    suspend fun previewFromUri(uri: String): Result<StatisticsBackupPreview>

    suspend fun importFromUri(
        uri: String,
        mode: StatisticsImportMode,
    ): Result<StatisticsImportResult>
}
