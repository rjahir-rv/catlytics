package com.catlytics.core.data.repository

import android.content.Context
import com.catlytics.core.domain.repository.UnifiedBackupRepository
import com.catlytics.core.model.BackupOptions
import com.catlytics.core.model.StatisticsImportMode
import com.catlytics.core.model.UnifiedBackupPreview
import com.catlytics.core.model.UnifiedBackupSummary
import com.catlytics.core.model.UnifiedExportResult
import com.catlytics.core.model.UnifiedImportResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultUnifiedBackupRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val statisticsSection: StatisticsBackupSection,
    private val playlistSection: PlaylistBackupSection,
) : UnifiedBackupRepository {

    internal var ioDispatcher: CoroutineDispatcher = Dispatchers.IO

    internal constructor(
        context: Context,
        statisticsSection: StatisticsBackupSection,
        playlistSection: PlaylistBackupSection,
        ioDispatcher: CoroutineDispatcher,
    ) : this(context, statisticsSection, playlistSection) {
        this.ioDispatcher = ioDispatcher
    }

    override fun observeSummary(): Flow<UnifiedBackupSummary> = combine(
        statisticsSection.observeSummary(),
        playlistSection.observeSummary(),
    ) { stats, playlists ->
        UnifiedBackupSummary(statistics = stats, playlists = playlists)
    }

    override suspend fun exportToUri(
        uri: String,
        options: BackupOptions,
        appVersion: String,
    ): Result<UnifiedExportResult> = withContext(ioDispatcher) {
        runSuspendCatching {
            require(options.includeStatistics || options.includePlaylists) {
                "Debes seleccionar al menos un tipo de dato para exportar."
            }

            val statistics = if (options.includeStatistics) statisticsSection.export() else null
            val playlists = if (options.includePlaylists) playlistSection.export() else null

            val document = BackupDocument(
                format = BackupCodec.UNIFIED_FORMAT,
                schemaVersion = BackupCodec.SUPPORTED_SCHEMA_VERSION,
                exportedAtMillis = System.currentTimeMillis(),
                appVersion = appVersion,
                events = statistics?.events.orEmpty(),
                artistAliases = statistics?.artistAliases.orEmpty(),
                trackMetadataOverrides = statistics?.trackMetadataOverrides.orEmpty(),
                artistArtworks = statistics?.artistArtworks.orEmpty(),
                playlists = playlists?.first.orEmpty(),
            )
            BackupCodec.write(context, uri, document)

            UnifiedExportResult(
                statistics = statistics?.toExportResult(),
                playlists = playlists?.second,
            )
        }
    }

    override suspend fun previewFromUri(uri: String): Result<UnifiedBackupPreview> =
        withContext(ioDispatcher) {
            runSuspendCatching {
                val document = BackupCodec.read(context, uri)
                UnifiedBackupPreview(
                    schemaVersion = document.schemaVersion,
                    exportedAtMillis = document.exportedAtMillis,
                    statistics = if (document.hasStatistics) statisticsSection.preview(document) else null,
                    playlists = if (document.playlists.isNotEmpty()) {
                        playlistSection.preview(
                            document.playlists,
                            document.schemaVersion,
                            document.exportedAtMillis,
                        )
                    } else {
                        null
                    },
                )
            }
        }

    override suspend fun importFromUri(
        uri: String,
        options: BackupOptions,
        mode: StatisticsImportMode,
    ): Result<UnifiedImportResult> = withContext(ioDispatcher) {
        runSuspendCatching {
            val document = BackupCodec.read(context, uri)
            UnifiedImportResult(
                statistics = if (options.includeStatistics && document.hasStatistics) {
                    statisticsSection.restore(document, mode)
                } else {
                    null
                },
                playlists = if (options.includePlaylists && document.playlists.isNotEmpty()) {
                    playlistSection.restore(document.playlists, mode)
                } else {
                    null
                },
            )
        }
    }
}
