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
import java.io.FilterInputStream
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
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
        runSuspendCatching {
            val events = playbackEventRepository.getAllEvents()
            val document = StatisticsBackupDocument(
                format = BACKUP_FORMAT,
                schemaVersion = SUPPORTED_SCHEMA_VERSION,
                exportedAtMillis = System.currentTimeMillis(),
                appVersion = appVersion,
                events = events.map { it.toDto() },
            )
            context.contentResolver.openOutputStream(uri.toUri())?.use { output ->
                writeDocument(
                    document,
                    SizeLimitedOutputStream(output, MAX_BACKUP_BYTES),
                )
                output.flush()
            } ?: error("No se pudo abrir el archivo de destino para exportar.")
            StatisticsExportResult(eventCount = events.size)
        }
    }

    override suspend fun previewFromUri(uri: String): Result<StatisticsBackupPreview> =
        withContext(ioDispatcher) {
            runSuspendCatching {
                val document = readDocument(uri)
                validateDocument(document)
                StatisticsBackupPreview(
                    schemaVersion = document.schemaVersion,
                    exportedAtMillis = document.exportedAtMillis,
                    eventCount = document.events.size,
                    firstEventMillis = document.events.minOfOrNull { it.timestamp },
                    lastEventMillis = document.events.maxOfOrNull { it.timestamp },
                )
            }
        }

    override suspend fun importFromUri(
        uri: String,
        mode: StatisticsImportMode,
    ): Result<StatisticsImportResult> = withContext(ioDispatcher) {
        runSuspendCatching {
            val document = readDocument(uri)
            validateDocument(document)
            val parsedEvents = document.events.map { it.toDomain() }
            val totalInFile = parsedEvents.size

            when (mode) {
                StatisticsImportMode.Replace -> {
                    playbackEventRepository.replaceEvents(parsedEvents)
                    StatisticsImportResult(
                        importedCount = totalInFile,
                        skippedDuplicateCount = 0,
                        totalInFile = totalInFile,
                    )
                }
                StatisticsImportMode.Merge -> {
                    val importedCount = playbackEventRepository.insertEventsIfAbsent(parsedEvents)
                    StatisticsImportResult(
                        importedCount = importedCount,
                        skippedDuplicateCount = totalInFile - importedCount,
                        totalInFile = totalInFile,
                    )
                }
            }
        }
    }

    private fun readDocument(uri: String): StatisticsBackupDocument {
        return context.contentResolver.openInputStream(uri.toUri())?.use { input ->
            decodeDocument(SizeLimitedInputStream(input, MAX_BACKUP_BYTES))
        } ?: error("No se pudo abrir el archivo de respaldo.")
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
        require(document.exportedAtMillis > 0L) { "Fecha de exportación inválida." }
        document.events.forEachIndexed { index, event ->
            require(event.trackId.isNotBlank()) { "Evento $index: trackId vacío." }
            require(event.trackTitle.isNotBlank()) { "Evento $index: título vacío." }
            require(event.artistId.isNotBlank()) { "Evento $index: artistId vacío." }
            require(event.artistName.isNotBlank()) { "Evento $index: artista vacío." }
            require(event.timestamp > 0L) { "Evento $index: timestamp inválido." }
            require(event.durationListenedMillis > 0L) {
                "Evento $index: duración escuchada inválida."
            }
            require(event.trackDurationMillis >= 0L) {
                "Evento $index: duración de canción inválida."
            }
        }
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
    } catch (error: BackupTooLargeException) {
        throw error
    } catch (error: Exception) {
        throw IllegalArgumentException("El archivo no es un respaldo de estadísticas válido.", error)
    }

    companion object {
        const val BACKUP_FORMAT = "catlytics.statistics.backup"
        const val SUPPORTED_SCHEMA_VERSION = 1
        internal const val MAX_BACKUP_BYTES = 64L * 1024L * 1024L
    }
}

private suspend inline fun <T> runSuspendCatching(
    crossinline block: suspend () -> T,
): Result<T> = try {
    Result.success(block())
} catch (cancellationException: CancellationException) {
    throw cancellationException
} catch (throwable: Throwable) {
    Result.failure(throwable)
}

private class BackupTooLargeException : IllegalArgumentException(
    "El respaldo supera el límite permitido de 64 MB.",
)

private class SizeLimitedInputStream(
    input: InputStream,
    private val maxBytes: Long,
) : FilterInputStream(input) {
    private var bytesRead = 0L

    override fun read(): Int {
        val value = super.read()
        if (value >= 0) accountFor(1)
        return value
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        val count = super.read(buffer, offset, length)
        if (count > 0) accountFor(count)
        return count
    }

    private fun accountFor(count: Int) {
        bytesRead += count
        if (bytesRead > maxBytes) throw BackupTooLargeException()
    }
}

private class SizeLimitedOutputStream(
    private val output: OutputStream,
    private val maxBytes: Long,
) : OutputStream() {
    private var bytesWritten = 0L

    override fun write(value: Int) {
        accountFor(1)
        output.write(value)
    }

    override fun write(buffer: ByteArray, offset: Int, length: Int) {
        accountFor(length)
        output.write(buffer, offset, length)
    }

    override fun flush() = output.flush()

    private fun accountFor(count: Int) {
        bytesWritten += count
        if (bytesWritten > maxBytes) throw BackupTooLargeException()
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
