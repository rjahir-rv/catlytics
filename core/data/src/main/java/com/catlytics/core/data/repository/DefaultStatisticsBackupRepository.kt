package com.catlytics.core.data.repository

import android.content.Context
import androidx.core.net.toUri
import com.catlytics.core.domain.repository.PlaybackEventRepository
import com.catlytics.core.domain.repository.StatisticsBackupRepository
import com.catlytics.core.model.PlaybackEvent
import com.catlytics.core.model.StatisticsBackupPreview
import com.catlytics.core.model.StatisticsBackupSummary
import com.catlytics.core.model.StatisticsExportResult
import com.catlytics.core.model.StatisticsImportMode
import com.catlytics.core.model.StatisticsImportResult
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Singleton
class DefaultStatisticsBackupRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val playbackEventRepository: PlaybackEventRepository,
) : StatisticsBackupRepository {
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    override fun observeLocalSummary(): Flow<StatisticsBackupSummary> =
        playbackEventRepository.observeBackupSummary()

    override suspend fun exportToUri(
        uri: String,
        appVersion: String,
    ): Result<StatisticsExportResult> = withContext(ioDispatcher) {
        runCatching {
            val events = playbackEventRepository.getAllEvents()
            val document = StatisticsBackupDocument(
                format = BACKUP_FORMAT,
                schemaVersion = SUPPORTED_SCHEMA_VERSION,
                exportedAtMillis = System.currentTimeMillis(),
                appVersion = appVersion,
                events = events.map { it.toDto() },
            )
            val payload = json.encodeToString(StatisticsBackupDocument.serializer(), document)
            context.contentResolver.openOutputStream(uri.toUri())?.use { output ->
                output.write(payload.toByteArray(Charsets.UTF_8))
                output.flush()
            } ?: error("No se pudo abrir el archivo de destino para exportar.")
            StatisticsExportResult(eventCount = events.size)
        }
    }

    override suspend fun previewFromUri(uri: String): Result<StatisticsBackupPreview> =
        withContext(ioDispatcher) {
            runCatching {
                val document = readDocument(uri)
                validateDocument(document)
                val timestamps = document.events.map { it.timestamp }
                StatisticsBackupPreview(
                    schemaVersion = document.schemaVersion,
                    exportedAtMillis = document.exportedAtMillis,
                    eventCount = document.events.size,
                    firstEventMillis = timestamps.minOrNull(),
                    lastEventMillis = timestamps.maxOrNull(),
                )
            }
        }

    override suspend fun importFromUri(
        uri: String,
        mode: StatisticsImportMode,
    ): Result<StatisticsImportResult> = withContext(ioDispatcher) {
        runCatching {
            val document = readDocument(uri)
            validateDocument(document)
            val parsedEvents = document.events.map { it.toDomain() }
            val totalInFile = parsedEvents.size

            when (mode) {
                StatisticsImportMode.Replace -> {
                    playbackEventRepository.deleteAllEvents()
                    playbackEventRepository.insertEvents(parsedEvents)
                    StatisticsImportResult(
                        importedCount = totalInFile,
                        skippedDuplicateCount = 0,
                        totalInFile = totalInFile,
                    )
                }
                StatisticsImportMode.Merge -> {
                    val existing = playbackEventRepository.getEventFingerprints().toMutableSet()
                    val toInsert = ArrayList<PlaybackEvent>(parsedEvents.size)
                    var skipped = 0
                    for (event in parsedEvents) {
                        val fingerprint = playbackEventFingerprint(
                            trackId = event.trackId,
                            timestamp = event.timestamp,
                            durationListenedMillis = event.durationListenedMillis,
                        )
                        if (fingerprint in existing) {
                            skipped++
                        } else {
                            existing.add(fingerprint)
                            toInsert.add(event)
                        }
                    }
                    playbackEventRepository.insertEvents(toInsert)
                    StatisticsImportResult(
                        importedCount = toInsert.size,
                        skippedDuplicateCount = skipped,
                        totalInFile = totalInFile,
                    )
                }
            }
        }
    }

    private fun readDocument(uri: String): StatisticsBackupDocument {
        val raw = context.contentResolver.openInputStream(uri.toUri())?.use { input ->
            input.readBytes().toString(Charsets.UTF_8)
        } ?: error("No se pudo abrir el archivo de respaldo.")
        return try {
            json.decodeFromString(StatisticsBackupDocument.serializer(), raw)
        } catch (error: Exception) {
            throw IllegalArgumentException("El archivo no es un respaldo de estadísticas válido.", error)
        }
    }

    private fun validateDocument(document: StatisticsBackupDocument) {
        if (document.format != BACKUP_FORMAT) {
            error("Formato de archivo no reconocido.")
        }
        if (document.schemaVersion != SUPPORTED_SCHEMA_VERSION) {
            error(
                "Versión de respaldo no soportada (v${document.schemaVersion}). " +
                    "Esta app admite v$SUPPORTED_SCHEMA_VERSION.",
            )
        }
        document.events.forEachIndexed { index, event ->
            require(event.trackId.isNotBlank()) { "Evento $index: trackId vacío." }
            require(event.timestamp > 0L) { "Evento $index: timestamp inválido." }
            require(event.durationListenedMillis > 0L) {
                "Evento $index: duración escuchada inválida."
            }
        }
    }

    companion object {
        const val BACKUP_FORMAT = "catlytics.statistics.backup"
        const val SUPPORTED_SCHEMA_VERSION = 1
    }
}

@Serializable
internal data class StatisticsBackupDocument(
    val format: String,
    val schemaVersion: Int,
    val exportedAtMillis: Long,
    val appVersion: String = "",
    val events: List<PlaybackEventDto> = emptyList(),
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
