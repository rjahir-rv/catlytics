package com.catlytics.core.domain.repository

import com.catlytics.core.model.BackupOptions
import com.catlytics.core.model.StatisticsImportMode
import com.catlytics.core.model.UnifiedBackupPreview
import com.catlytics.core.model.UnifiedBackupSummary
import com.catlytics.core.model.UnifiedExportResult
import com.catlytics.core.model.UnifiedImportResult
import kotlinx.coroutines.flow.Flow

interface UnifiedBackupRepository {
    fun observeSummary(): Flow<UnifiedBackupSummary>

    suspend fun exportToUri(
        uri: String,
        options: BackupOptions,
        appVersion: String,
    ): Result<UnifiedExportResult>

    suspend fun previewFromUri(uri: String): Result<UnifiedBackupPreview>

    suspend fun importFromUri(
        uri: String,
        options: BackupOptions,
        mode: StatisticsImportMode,
    ): Result<UnifiedImportResult>
}
