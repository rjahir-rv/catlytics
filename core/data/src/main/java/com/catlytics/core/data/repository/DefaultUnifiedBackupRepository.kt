package com.catlytics.core.data.repository

import android.content.Context
import androidx.core.net.toUri
import com.catlytics.core.domain.repository.UnifiedBackupRepository
import com.catlytics.core.model.BackupOptions
import com.catlytics.core.model.PlaylistExportResult
import com.catlytics.core.model.PlaylistImportResult
import com.catlytics.core.model.StatisticsExportResult
import com.catlytics.core.model.StatisticsImportMode
import com.catlytics.core.model.StatisticsImportResult
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
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultUnifiedBackupRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val statisticsBackupRepository: DefaultStatisticsBackupRepository,
    private val playlistBackupRepository: DefaultPlaylistBackupRepository,
) : UnifiedBackupRepository {

    internal var ioDispatcher: CoroutineDispatcher = Dispatchers.IO

    internal constructor(
        context: Context,
        statisticsBackupRepository: DefaultStatisticsBackupRepository,
        playlistBackupRepository: DefaultPlaylistBackupRepository,
        ioDispatcher: CoroutineDispatcher,
    ) : this(context, statisticsBackupRepository, playlistBackupRepository) {
        this.ioDispatcher = ioDispatcher
    }

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = false
    }

    override fun observeSummary(): Flow<UnifiedBackupSummary> = combine(
        statisticsBackupRepository.observeLocalSummary(),
        playlistBackupRepository.observeLocalSummary(),
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

            var statsExportResult: StatisticsExportResult? = null
            val (events, aliases) = if (options.includeStatistics) {
                val (evs, als) = statisticsBackupRepository.exportEventsAndAliases()
                statsExportResult = StatisticsExportResult(eventCount = evs.size, artistAliasCount = als.size)
                evs to als
            } else {
                emptyList<PlaybackEventDto>() to emptyList<ArtistAliasDto>()
            }

            var playlistExportResult: PlaylistExportResult? = null
            val playlistDtos = if (options.includePlaylists) {
                val (dtos, result) = playlistBackupRepository.exportPlaylistDtos()
                playlistExportResult = result
                dtos
            } else {
                emptyList()
            }

            val document = StatisticsBackupDocument(
                format = BACKUP_FORMAT,
                schemaVersion = SUPPORTED_SCHEMA_VERSION,
                exportedAtMillis = System.currentTimeMillis(),
                appVersion = appVersion,
                events = events,
                artistAliases = aliases,
                playlists = playlistDtos,
            )

            context.contentResolver.openOutputStream(uri.toUri())?.use { output ->
                writeDocument(document, SizeLimitedOutputStream(output, MAX_BACKUP_BYTES))
                output.flush()
            } ?: error("No se pudo abrir el archivo de destino para exportar el respaldo.")

            UnifiedExportResult(
                statistics = statsExportResult,
                playlists = playlistExportResult,
            )
        }
    }

    override suspend fun previewFromUri(uri: String): Result<UnifiedBackupPreview> =
        withContext(ioDispatcher) {
            runSuspendCatching {
                val document = readDocument(uri)
                validateDocument(document)

                val statsPreview = if (document.events.isNotEmpty() || document.artistAliases.isNotEmpty()) {
                    statisticsBackupRepository.previewFromDocument(document)
                } else null

                val playlistPreview = if (document.playlists.isNotEmpty()) {
                    playlistBackupRepository.previewFromPlaylists(
                        document.playlists,
                        document.schemaVersion,
                        document.exportedAtMillis,
                    )
                } else null

                UnifiedBackupPreview(
                    schemaVersion = document.schemaVersion,
                    exportedAtMillis = document.exportedAtMillis,
                    statistics = statsPreview,
                    playlists = playlistPreview,
                )
            }
        }

    override suspend fun importFromUri(
        uri: String,
        options: BackupOptions,
        mode: StatisticsImportMode,
    ): Result<UnifiedImportResult> = withContext(ioDispatcher) {
        runSuspendCatching {
            val document = readDocument(uri)
            validateDocument(document)

            var statsImportResult: StatisticsImportResult? = null
            if (options.includeStatistics && (document.events.isNotEmpty() || document.artistAliases.isNotEmpty())) {
                statsImportResult = statisticsBackupRepository.restoreStatistics(document, mode)
            }

            var playlistImportResult: PlaylistImportResult? = null
            if (options.includePlaylists && document.playlists.isNotEmpty()) {
                playlistImportResult = playlistBackupRepository.restoreFromPlaylists(document.playlists, mode)
            }

            UnifiedImportResult(
                statistics = statsImportResult,
                playlists = playlistImportResult,
            )
        }
    }

    private fun readDocument(uri: String): StatisticsBackupDocument {
        return context.contentResolver.openInputStream(uri.toUri())?.use { input ->
            decodeDocument(SizeLimitedInputStream(input, MAX_BACKUP_BYTES))
        } ?: error("No se pudo abrir el archivo de respaldo.")
    }

    private fun validateDocument(document: StatisticsBackupDocument) {
        if (document.format != BACKUP_FORMAT &&
            document.format != PLAYLIST_BACKUP_FORMAT &&
            document.format != LEGACY_STATISTICS_FORMAT
        ) {
            error("Formato de archivo no reconocido (${document.format}).")
        }
        if (document.schemaVersion !in MIN_SUPPORTED_SCHEMA_VERSION..SUPPORTED_SCHEMA_VERSION) {
            error(
                "Versión de respaldo no soportada (v${document.schemaVersion}). " +
                    "Esta app admite v$SUPPORTED_SCHEMA_VERSION.",
            )
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    private fun writeDocument(
        document: StatisticsBackupDocument,
        output: OutputStream,
    ) {
        json.encodeToStream(document, output)
    }

    @OptIn(ExperimentalSerializationApi::class)
    private fun decodeDocument(input: InputStream): StatisticsBackupDocument = try {
        json.decodeFromStream<StatisticsBackupDocument>(input)
    } catch (error: BackupTooLargeException) {
        throw error
    } catch (error: Exception) {
        throw IllegalArgumentException("El archivo no es un respaldo válido de Catlytics.", error)
    }

    companion object {
        const val BACKUP_FORMAT = "catlytics.backup"
        const val PLAYLIST_BACKUP_FORMAT = "catlytics.playlists.backup"
        const val LEGACY_STATISTICS_FORMAT = "catlytics.statistics.backup"
        const val SUPPORTED_SCHEMA_VERSION = 3
        const val MIN_SUPPORTED_SCHEMA_VERSION = 1
        internal const val MAX_BACKUP_BYTES = 64L * 1024L * 1024L
    }
}
