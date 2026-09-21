package com.catlytics.core.domain.usecase.backup

import com.catlytics.core.domain.repository.UnifiedBackupRepository
import com.catlytics.core.model.BackupOptions
import com.catlytics.core.model.StatisticsImportMode
import com.catlytics.core.model.UnifiedBackupPreview
import com.catlytics.core.model.UnifiedBackupSummary
import com.catlytics.core.model.UnifiedExportResult
import com.catlytics.core.model.UnifiedImportResult
import kotlinx.coroutines.flow.Flow

class ObserveUnifiedBackupSummaryUseCase(
    private val repository: UnifiedBackupRepository,
) {
    operator fun invoke(): Flow<UnifiedBackupSummary> = repository.observeSummary()
}

class ExportUnifiedBackupUseCase(
    private val repository: UnifiedBackupRepository,
) {
    suspend operator fun invoke(
        uri: String,
        options: BackupOptions,
        appVersion: String,
    ): Result<UnifiedExportResult> = repository.exportToUri(uri, options, appVersion)
}

class PreviewUnifiedBackupUseCase(
    private val repository: UnifiedBackupRepository,
) {
    suspend operator fun invoke(uri: String): Result<UnifiedBackupPreview> =
        repository.previewFromUri(uri)
}

class ImportUnifiedBackupUseCase(
    private val repository: UnifiedBackupRepository,
) {
    suspend operator fun invoke(
        uri: String,
        options: BackupOptions,
        mode: StatisticsImportMode,
    ): Result<UnifiedImportResult> = repository.importFromUri(uri, options, mode)
}
