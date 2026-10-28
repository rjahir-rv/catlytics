package com.catlytics.core.data.repository

import androidx.room.withTransaction
import com.catlytics.core.data.local.room.CatlyticsDatabase
import com.catlytics.core.domain.repository.ArtistIdentityRepository
import com.catlytics.core.domain.repository.PlaybackEventRepository
import com.catlytics.core.model.StatisticsBackupPreview
import com.catlytics.core.model.StatisticsBackupSummary
import com.catlytics.core.model.StatisticsExportResult
import com.catlytics.core.model.StatisticsImportMode
import com.catlytics.core.model.StatisticsImportResult
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StatisticsBackupSection @Inject constructor(
    private val playbackEventRepository: PlaybackEventRepository,
    private val artistIdentityRepository: ArtistIdentityRepository,
    private val database: CatlyticsDatabase,
    private val trackMetadataRepository: RoomTrackMetadataRepository,
    private val artistArtworkRepository: RoomArtistArtworkRepository,
) {
    fun observeSummary(): Flow<StatisticsBackupSummary> = combine(
        playbackEventRepository.observeBackupSummary(),
        artistIdentityRepository.observeAliases(),
        trackMetadataRepository.observeOverrides(),
        artistArtworkRepository.observeOverrides(),
    ) { summary, aliases, overrides, artistArtworks ->
        summary.copy(
            artistAliasCount = aliases.size,
            metadataEditCount = overrides.size + artistArtworks.size,
        )
    }

    internal suspend fun export(): StatisticsBackupPayload = StatisticsBackupPayload(
        events = playbackEventRepository.getAllEvents().map { it.toDto() },
        artistAliases = artistIdentityRepository.getAliases().map { it.toDto() },
        trackMetadataOverrides = trackMetadataRepository.exportForBackup(),
        artistArtworks = artistArtworkRepository.exportForBackup(),
    )

    internal fun preview(document: BackupDocument): StatisticsBackupPreview = StatisticsBackupPreview(
        schemaVersion = document.schemaVersion,
        exportedAtMillis = document.exportedAtMillis,
        eventCount = document.events.size,
        firstEventMillis = document.events.minOfOrNull { it.timestamp },
        lastEventMillis = document.events.maxOfOrNull { it.timestamp },
        artistAliasCount = document.artistAliases.size,
        metadataEditCount = document.trackMetadataOverrides.size + document.artistArtworks.size,
    )

    internal suspend fun restore(
        document: BackupDocument,
        mode: StatisticsImportMode,
    ): StatisticsImportResult {
        val parsedEvents = document.events.map { it.toDomain() }
        val parsedAliases = document.artistAliases.map { it.toDomain() }
        val totalInFile = parsedEvents.size
        val overrides = trackMetadataRepository.prepareBackupRestore(document.trackMetadataOverrides)
        val artistArtworks = artistArtworkRepository.prepareBackupRestore(document.artistArtworks)

        return try {
            when (mode) {
                StatisticsImportMode.Replace -> {
                    database.withTransaction {
                        playbackEventRepository.replaceEvents(parsedEvents)
                        if (document.schemaVersion >= BackupCodec.ALIAS_SCHEMA_VERSION) {
                            artistIdentityRepository.replaceAliases(parsedAliases)
                        }
                        if (document.schemaVersion >= BackupCodec.METADATA_SCHEMA_VERSION) {
                            trackMetadataRepository.replaceFromBackup(overrides)
                        }
                        if (document.schemaVersion >= BackupCodec.ARTIST_ARTWORK_SCHEMA_VERSION) {
                            artistArtworkRepository.replaceFromBackup(artistArtworks)
                        }
                    }
                    StatisticsImportResult(
                        importedCount = totalInFile,
                        skippedDuplicateCount = 0,
                        totalInFile = totalInFile,
                        importedArtistAliasCount = parsedAliases.size,
                        importedMetadataEditCount = overrides.size + artistArtworks.size,
                    )
                }
                StatisticsImportMode.Merge -> {
                    val (importedCount, importedAliasCount, importedEditCount) = database.withTransaction {
                        Triple(
                            playbackEventRepository.insertEventsIfAbsent(parsedEvents),
                            artistIdentityRepository.mergeAliases(parsedAliases),
                            trackMetadataRepository.mergeFromBackup(overrides) +
                                artistArtworkRepository.mergeFromBackup(artistArtworks),
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
            withContext(NonCancellable) {
                trackMetadataRepository.deleteUnreferencedArtwork()
                artistArtworkRepository.deleteUnreferencedArtwork()
            }
        }
    }
}

internal data class StatisticsBackupPayload(
    val events: List<PlaybackEventDto>,
    val artistAliases: List<ArtistAliasDto>,
    val trackMetadataOverrides: List<TrackMetadataOverrideDto>,
    val artistArtworks: List<ArtistArtworkDto>,
) {
    fun toExportResult() = StatisticsExportResult(
        eventCount = events.size,
        artistAliasCount = artistAliases.size,
        metadataEditCount = trackMetadataOverrides.size + artistArtworks.size,
    )
}
