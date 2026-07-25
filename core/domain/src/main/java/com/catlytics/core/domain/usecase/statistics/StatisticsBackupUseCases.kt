package com.catlytics.core.domain.usecase.statistics

import com.catlytics.core.domain.repository.StatisticsBackupRepository
import com.catlytics.core.model.StatisticsBackupPreview
import com.catlytics.core.model.StatisticsBackupSummary
import com.catlytics.core.model.StatisticsExportResult
import com.catlytics.core.model.StatisticsImportMode
import com.catlytics.core.model.StatisticsImportResult
import kotlinx.coroutines.flow.Flow

class ObserveStatisticsBackupSummaryUseCase(
    private val repository: StatisticsBackupRepository,
) {
    operator fun invoke(): Flow<StatisticsBackupSummary> = repository.observeLocalSummary()
}

class ExportStatisticsBackupUseCase(
    private val repository: StatisticsBackupRepository,
) {
    suspend operator fun invoke(
        uri: String,
        appVersion: String,
    ): Result<StatisticsExportResult> = repository.exportToUri(uri, appVersion)
}

class PreviewStatisticsBackupUseCase(
    private val repository: StatisticsBackupRepository,
) {
    suspend operator fun invoke(uri: String): Result<StatisticsBackupPreview> =
        repository.previewFromUri(uri)
}

class ImportStatisticsBackupUseCase(
    private val repository: StatisticsBackupRepository,
) {
    suspend operator fun invoke(
        uri: String,
        mode: StatisticsImportMode = StatisticsImportMode.Merge,
    ): Result<StatisticsImportResult> = repository.importFromUri(uri, mode)
}
