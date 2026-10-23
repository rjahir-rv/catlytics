package com.catlytics.core.data.repository

import android.content.Context
import androidx.core.net.toUri
import androidx.room.withTransaction
import com.catlytics.core.data.local.room.CatlyticsDatabase
import com.catlytics.core.domain.repository.PlaybackEventRepository
import com.catlytics.core.domain.repository.ArtistIdentityRepository
import com.catlytics.core.domain.repository.StatisticsBackupRepository
import com.catlytics.core.model.PlaybackEvent
import com.catlytics.core.model.Artist
import com.catlytics.core.model.ArtistAlias
import com.catlytics.core.model.StatisticsBackupPreview
import com.catlytics.core.model.StatisticsBackupSummary
import com.catlytics.core.model.StatisticsExportResult
import com.catlytics.core.model.BackupException
import com.catlytics.core.model.StatisticsImportMode
import com.catlytics.core.model.StatisticsImportResult
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream

@Singleton
class DefaultStatisticsBackupRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val playbackEventRepository: PlaybackEventRepository,
    private val artistIdentityRepository: ArtistIdentityRepository,
    private val database: CatlyticsDatabase,
    private val trackMetadataRepository: RoomTrackMetadataRepository,
) : StatisticsBackupRepository {
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    override fun observeLocalSummary(): Flow<StatisticsBackupSummary> = combine(
        playbackEventRepository.observeBackupSummary(),
        artistIdentityRepository.observeAliases(),
        trackMetadataRepository.observeOverrides(),
    ) { summary, aliases, overrides ->
        summary.copy(artistAliasCount = aliases.size, metadataEditCount = overrides.size)
    }

    /** Statistics travel with aliases and metadata edits, since both regroup the history. */
    internal suspend fun exportStatisticsSection(): StatisticsBackupSection = StatisticsBackupSection(
        events = playbackEventRepository.getAllEvents().map { it.toDto() },
        artistAliases = artistIdentityRepository.getAliases().map { it.toDto() },
        trackMetadataOverrides = trackMetadataRepository.exportForBackup(),
    )

    internal fun previewFromDocument(document: StatisticsBackupDocument): StatisticsBackupPreview {
        return StatisticsBackupPreview(
            schemaVersion = document.schemaVersion,
            exportedAtMillis = document.exportedAtMillis,
            eventCount = document.events.size,
            firstEventMillis = document.events.minOfOrNull { it.timestamp },
            lastEventMillis = document.events.maxOfOrNull { it.timestamp },
            artistAliasCount = document.artistAliases.size,
            metadataEditCount = document.trackMetadataOverrides.size,
        )
    }

    internal suspend fun restoreStatistics(
        document: StatisticsBackupDocument,
        mode: StatisticsImportMode,
    ): StatisticsImportResult {
        val parsedEvents = document.events.map { it.toDomain() }
        val parsedAliases = document.artistAliases.map { it.toDomain() }
        val totalInFile = parsedEvents.size
        val overrides = trackMetadataRepository.prepareBackupRestore(document.trackMetadataOverrides)

        return try {
            when (mode) {
                StatisticsImportMode.Replace -> {
                    database.withTransaction {
                        playbackEventRepository.replaceEvents(parsedEvents)
                        if (document.schemaVersion >= ALIAS_SCHEMA_VERSION) {
                            artistIdentityRepository.replaceAliases(parsedAliases)
                        }
                        if (document.schemaVersion >= METADATA_SCHEMA_VERSION) {
                            trackMetadataRepository.replaceFromBackup(overrides)
                        }
                    }
                    StatisticsImportResult(
                        importedCount = totalInFile,
                        skippedDuplicateCount = 0,
                        totalInFile = totalInFile,
                        importedArtistAliasCount = parsedAliases.size,
                        importedMetadataEditCount = overrides.size,
                    )
                }
                StatisticsImportMode.Merge -> {
                    val (importedCount, importedAliasCount, importedEditCount) = database.withTransaction {
                        Triple(
                            playbackEventRepository.insertEventsIfAbsent(parsedEvents),
                            artistIdentityRepository.mergeAliases(parsedAliases),
                            trackMetadataRepository.mergeFromBackup(overrides),
                        )
                    }
                    StatisticsImportResult(
                        importedCount = importedCount,
                        skippedDuplicateCount = totalInFile - importedCount,
                        totalInFile = totalInFile,
                        importedArtistAliasCount = importedAliasCount,
                        importedMetadataEditCount = importedEditCount,
                    )
                }
            }
        } finally {
            // Drops artwork written for edits that were skipped, rolled back or replaced.
            withContext(NonCancellable) { trackMetadataRepository.deleteUnreferencedArtwork() }
        }
    }

    override suspend fun exportToUri(
        uri: String,
        appVersion: String,
    ): Result<StatisticsExportResult> = withContext(ioDispatcher) {
        runSuspendCatching {
            val section = exportStatisticsSection()
            val document = StatisticsBackupDocument(
                format = BackupDocumentValidator.STATISTICS_FORMAT,
                schemaVersion = BackupDocumentValidator.SUPPORTED_SCHEMA_VERSION,
                exportedAtMillis = System.currentTimeMillis(),
                appVersion = appVersion,
                events = section.events,
                artistAliases = section.artistAliases,
                trackMetadataOverrides = section.trackMetadataOverrides,
            )
            context.contentResolver.openOutputStream(uri.toUri())?.use { output ->
                writeDocument(
                    document,
                    SizeLimitedOutputStream(output, BackupDocumentValidator.MAX_BACKUP_BYTES),
                )
                output.flush()
            } ?: error("No se pudo abrir el archivo de destino para exportar.")
            section.toExportResult()
        }
    }

    override suspend fun previewFromUri(uri: String): Result<StatisticsBackupPreview> =
        withContext(ioDispatcher) {
            runSuspendCatching {
                val document = readDocument(uri)
                BackupDocumentValidator.validate(document)
                previewFromDocument(document)
            }
        }

    override suspend fun importFromUri(
        uri: String,
        mode: StatisticsImportMode,
    ): Result<StatisticsImportResult> = withContext(ioDispatcher) {
        runSuspendCatching {
            val document = readDocument(uri)
            BackupDocumentValidator.validate(document)
            restoreStatistics(document, mode)
        }
    }

    private fun readDocument(uri: String): StatisticsBackupDocument {
        return context.contentResolver.openInputStream(uri.toUri())?.use { input ->
            decodeDocument(SizeLimitedInputStream(input, BackupDocumentValidator.MAX_BACKUP_BYTES))
        } ?: error("No se pudo abrir el archivo de respaldo.")
    }

    @OptIn(
        ExperimentalSerializationApi::class,
        InternalSerializationApi::class,
    )
    private fun writeDocument(
        document: StatisticsBackupDocument,
        output: OutputStream,
    ) {
        json.encodeToStream(document, output)
    }

    @OptIn(
        ExperimentalSerializationApi::class,
        InternalSerializationApi::class,
    )
    private fun decodeDocument(input: InputStream): StatisticsBackupDocument = try {
        json.decodeFromStream<StatisticsBackupDocument>(input)
    } catch (error: BackupException) {
        throw error
    } catch (error: Exception) {
        throw BackupException.InvalidFormat("Not a Catlytics backup document.", error)
    }

    companion object {
        const val ALIAS_SCHEMA_VERSION = 2
        const val METADATA_SCHEMA_VERSION = 4
    }
}

@OptIn(
    ExperimentalSerializationApi::class,
    InternalSerializationApi::class,
)
@Serializable
internal data class StatisticsBackupDocument(
    val format: String,
    val schemaVersion: Int,
    val exportedAtMillis: Long,
    val appVersion: String = "",
    val events: List<PlaybackEventDto> = emptyList(),
    val artistAliases: List<ArtistAliasDto> = emptyList(),
    val playlists: List<PlaylistBackupDto> = emptyList(),
    val trackMetadataOverrides: List<TrackMetadataOverrideDto> = emptyList(),
) {
    val hasStatistics: Boolean
        get() = events.isNotEmpty() || artistAliases.isNotEmpty() || trackMetadataOverrides.isNotEmpty()
}

internal data class StatisticsBackupSection(
    val events: List<PlaybackEventDto>,
    val artistAliases: List<ArtistAliasDto>,
    val trackMetadataOverrides: List<TrackMetadataOverrideDto>,
) {
    fun toExportResult() = StatisticsExportResult(
        eventCount = events.size,
        artistAliasCount = artistAliases.size,
        metadataEditCount = trackMetadataOverrides.size,
    )
}
@OptIn(
    ExperimentalSerializationApi::class,
    InternalSerializationApi::class,
)
@Serializable
internal data class ArtistAliasDto(
    val sourceArtistId: String,
    val sourceArtistName: String,
    val targetArtistId: String,
    val targetArtistName: String,
)
@OptIn(
    ExperimentalSerializationApi::class,
    InternalSerializationApi::class,
)
@Serializable
internal data class PlaybackEventDto(
    val trackId: String,
    val trackTitle: String,
    val artistId: String,
    val artistName: String,
    val albumId: String? = null,
    val albumTitle: String? = null,
    val artworkUri: String? = null,
    val durationListenedMillis: Long,
    val trackDurationMillis: Long = 0L,
    val timestamp: Long,
)

private fun PlaybackEvent.toDto() = PlaybackEventDto(
    trackId = trackId,
    trackTitle = trackTitle,
    artistId = artistId,
    artistName = artistName,
    albumId = albumId,
    albumTitle = albumTitle,
    artworkUri = artworkUri,
    durationListenedMillis = durationListenedMillis,
    trackDurationMillis = trackDurationMillis,
    timestamp = timestamp,
)

private fun PlaybackEventDto.toDomain() = PlaybackEvent(
    trackId = trackId,
    trackTitle = trackTitle,
    artistId = artistId,
    artistName = artistName,
    albumId = albumId,
    albumTitle = albumTitle,
    artworkUri = artworkUri,
    durationListenedMillis = durationListenedMillis,
    trackDurationMillis = trackDurationMillis,
    timestamp = timestamp,
)

private fun ArtistAlias.toDto() = ArtistAliasDto(
    sourceArtistId = source.id,
    sourceArtistName = source.name,
    targetArtistId = target.id,
    targetArtistName = target.name,
)

private fun ArtistAliasDto.toDomain() = ArtistAlias(
    source = Artist(sourceArtistId, sourceArtistName),
    target = Artist(targetArtistId, targetArtistName),
)
