package com.catlytics.core.data.repository

import android.content.Context
import androidx.core.net.toUri
import com.catlytics.core.data.local.LocalDataSource
import com.catlytics.core.data.model.TrackEntity
import com.catlytics.core.domain.repository.PlaylistBackupRepository
import com.catlytics.core.domain.repository.PlaylistRepository
import com.catlytics.core.model.LIKED_PLAYLIST_ID
import com.catlytics.core.model.LIKED_PLAYLIST_NAME
import com.catlytics.core.model.Playlist
import com.catlytics.core.model.PlaylistBackupPreview
import com.catlytics.core.model.PlaylistBackupSummary
import com.catlytics.core.model.PlaylistExportResult
import com.catlytics.core.model.PlaylistImportResult
import com.catlytics.core.model.StatisticsImportMode
import com.catlytics.core.model.artistIdentityKey
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.text.Normalizer
import java.util.Base64
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream

@Singleton
class DefaultPlaylistBackupRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val playlistRepository: PlaylistRepository,
    private val localDataSource: LocalDataSource,
) : PlaylistBackupRepository {

    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    override fun observeLocalSummary(): Flow<PlaylistBackupSummary> =
        playlistRepository.observePlaylists().map { playlists ->
            val liked = playlists.firstOrNull { it.id == LIKED_PLAYLIST_ID }
            val nonLiked = playlists.filterNot { it.id == LIKED_PLAYLIST_ID }
            PlaylistBackupSummary(
                playlistCount = nonLiked.size,
                totalTracks = nonLiked.sumOf { it.trackIds.size },
                likedTracksCount = liked?.trackIds?.size ?: 0,
            )
        }

    internal suspend fun exportPlaylistDtos(): Pair<List<PlaylistBackupDto>, PlaylistExportResult> {
        val playlists = playlistRepository.observePlaylists().first()
        val tracksById = localDataSource.observeTracks().first().associateBy { it.id }

        val playlistDtos = playlists.map { playlist ->
            val trackDtos = playlist.trackIds.map { trackId ->
                val track = tracksById[trackId]
                PlaylistTrackBackupDto(
                    trackId = trackId,
                    title = track?.title ?: "Canción sin título",
                    artistName = track?.artistName ?: "Artista desconocido",
                    albumTitle = track?.albumTitle,
                    durationMillis = track?.durationMillis ?: 0L,
                    relativePath = track?.folderPath,
                    trackNumber = track?.trackNumber,
                )
            }
            val coverBase64 = playlist.artworkUri?.let(::tryReadCoverBase64)
            PlaylistBackupDto(
                id = playlist.id,
                name = playlist.name,
                description = playlist.description,
                isLikedPlaylist = playlist.id == LIKED_PLAYLIST_ID,
                coverBase64 = coverBase64,
                tracks = trackDtos,
            )
        }
        val nonLiked = playlists.filterNot { it.id == LIKED_PLAYLIST_ID }
        val exportResult = PlaylistExportResult(
            playlistCount = nonLiked.size,
            trackCount = nonLiked.sumOf { it.trackIds.size },
        )
        return playlistDtos to exportResult
    }

    internal suspend fun previewFromPlaylists(
        playlists: List<PlaylistBackupDto>,
        schemaVersion: Int,
        exportedAtMillis: Long,
    ): PlaylistBackupPreview {
        val localTracks = localDataSource.observeTracks().first()
        val reconciler = TrackReconciler(localTracks)

        var totalTracksInBackup = 0
        var matchedTracksCount = 0
        var missingTracksCount = 0
        var likedTracksInBackup = 0
        val missingTrackNames = mutableListOf<String>()

        playlists.forEach { playlistDto ->
            if (playlistDto.isLikedPlaylist || playlistDto.id == LIKED_PLAYLIST_ID) {
                likedTracksInBackup = playlistDto.tracks.size
            }
            totalTracksInBackup += playlistDto.tracks.size
            playlistDto.tracks.forEach { trackDto ->
                val matched = reconciler.reconcile(trackDto)
                if (matched != null) {
                    matchedTracksCount++
                } else {
                    missingTracksCount++
                    val name = "${trackDto.artistName} - ${trackDto.title}"
                    if (name !in missingTrackNames && missingTrackNames.size < 50) {
                        missingTrackNames.add(name)
                    }
                }
            }
        }

        val nonLikedCount = playlists.count {
            !it.isLikedPlaylist && it.id != LIKED_PLAYLIST_ID
        }

        return PlaylistBackupPreview(
            schemaVersion = schemaVersion,
            exportedAtMillis = exportedAtMillis,
            playlistCount = nonLikedCount,
            totalTracksInBackup = totalTracksInBackup,
            matchedTracksCount = matchedTracksCount,
            missingTracksCount = missingTracksCount,
            likedTracksCount = likedTracksInBackup,
            missingTrackNames = missingTrackNames,
        )
    }

    internal suspend fun restoreFromPlaylists(
        playlists: List<PlaylistBackupDto>,
        mode: StatisticsImportMode,
    ): PlaylistImportResult {
        val localTracks = localDataSource.observeTracks().first()
        val reconciler = TrackReconciler(localTracks)

        var totalMatched = 0
        var totalMissing = 0
        val missingTrackNames = mutableListOf<String>()

        val restoredPlaylists = playlists.map { playlistDto ->
            val matchedIds = mutableListOf<String>()
            playlistDto.tracks.forEach { trackDto ->
                val matched = reconciler.reconcile(trackDto)
                if (matched != null) {
                    matchedIds.add(matched.id)
                    totalMatched++
                } else {
                    totalMissing++
                    val name = "${trackDto.artistName} - ${trackDto.title}"
                    if (name !in missingTrackNames && missingTrackNames.size < 50) {
                        missingTrackNames.add(name)
                    }
                }
            }

            val coverUri = playlistDto.coverBase64?.let { base64 ->
                saveCoverFromBase64(playlistDto.id, base64)
            }

            val playlistId = if (playlistDto.isLikedPlaylist || playlistDto.id == LIKED_PLAYLIST_ID) {
                LIKED_PLAYLIST_ID
            } else {
                playlistDto.id
            }

            val playlistName = if (playlistId == LIKED_PLAYLIST_ID) {
                LIKED_PLAYLIST_NAME
            } else {
                playlistDto.name
            }

            Playlist(
                id = playlistId,
                name = playlistName,
                trackIds = matchedIds.distinct(),
                artworkUri = coverUri,
                description = playlistDto.description,
            )
        }

        playlistRepository.restorePlaylists(restoredPlaylists, mode)

        val nonLikedCount = restoredPlaylists.count { it.id != LIKED_PLAYLIST_ID }
        return PlaylistImportResult(
            importedPlaylistsCount = nonLikedCount,
            matchedTracksCount = totalMatched,
            missingTracksCount = totalMissing,
            missingTrackNames = missingTrackNames,
        )
    }

    override suspend fun exportToUri(
        uri: String,
        appVersion: String,
    ): Result<PlaylistExportResult> = withContext(ioDispatcher) {
        runSuspendCatching {
            val (playlistDtos, exportResult) = exportPlaylistDtos()
            val document = PlaylistBackupDocument(
                format = BACKUP_FORMAT,
                schemaVersion = SUPPORTED_SCHEMA_VERSION,
                exportedAtMillis = System.currentTimeMillis(),
                appVersion = appVersion,
                playlists = playlistDtos,
            )

            context.contentResolver.openOutputStream(uri.toUri())?.use { output ->
                writeDocument(document, SizeLimitedOutputStream(output, MAX_BACKUP_BYTES))
                output.flush()
            } ?: error("No se pudo abrir el archivo de destino para exportar playlists.")

            exportResult
        }
    }

    override suspend fun previewFromUri(uri: String): Result<PlaylistBackupPreview> =
        withContext(ioDispatcher) {
            runSuspendCatching {
                val document = readDocument(uri)
                validateDocument(document)
                previewFromPlaylists(document.playlists, document.schemaVersion, document.exportedAtMillis)
            }
        }

    override suspend fun importFromUri(
        uri: String,
        mode: StatisticsImportMode,
    ): Result<PlaylistImportResult> = withContext(ioDispatcher) {
        runSuspendCatching {
            val document = readDocument(uri)
            validateDocument(document)
            restoreFromPlaylists(document.playlists, mode)
        }
    }

    override suspend fun exportPlaylistToM3uUri(
        playlistId: String,
        uri: String,
    ): Result<Unit> = withContext(ioDispatcher) {
        runSuspendCatching {
            val playlists = playlistRepository.observePlaylists().first()
            val playlist = playlists.firstOrNull { it.id == playlistId }
                ?: error("Playlist no encontrada.")
            val tracksById = localDataSource.observeTracks().first().associateBy { it.id }

            val content = buildString {
                appendLine("#EXTM3U")
                appendLine("#PLAYLIST:${playlist.name}")
                playlist.trackIds.forEach { trackId ->
                    val track = tracksById[trackId] ?: return@forEach
                    val durationSeconds = (track.durationMillis / 1000L).coerceAtLeast(0L)
                    appendLine("#EXTINF:$durationSeconds,${track.artistName} - ${track.title}")
                    val pathOrUri = track.folderPath?.let { folder ->
                        "$folder/${track.title}"
                    } ?: track.mediaUri
                    appendLine(pathOrUri)
                }
            }

            context.contentResolver.openOutputStream(uri.toUri())?.use { output ->
                output.bufferedWriter(Charsets.UTF_8).use { writer ->
                    writer.write(content)
                }
                output.flush()
            } ?: error("No se pudo abrir el archivo de destino para exportar M3U8.")
        }
    }

    private fun readDocument(uri: String): PlaylistBackupDocument {
        return context.contentResolver.openInputStream(uri.toUri())?.use { input ->
            decodeDocument(SizeLimitedInputStream(input, MAX_BACKUP_BYTES))
        } ?: error("No se pudo abrir el archivo de respaldo.")
    }

    private fun validateDocument(document: PlaylistBackupDocument) {
        if (document.format != BACKUP_FORMAT &&
            document.format != LEGACY_STATISTICS_FORMAT &&
            document.format != UNIFIED_BACKUP_FORMAT
        ) {
            error("Formato de archivo no reconocido como respaldo de Catlytics.")
        }
        if (document.schemaVersion !in MIN_SUPPORTED_SCHEMA_VERSION..SUPPORTED_SCHEMA_VERSION) {
            error("Versión de respaldo no soportada (v${document.schemaVersion}).")
        }
        require(document.exportedAtMillis > 0L) { "Fecha de exportación inválida." }
        document.playlists.forEachIndexed { index, playlist ->
            require(playlist.id.isNotBlank()) { "Playlist $index: ID vacío." }
            require(playlist.name.isNotBlank()) { "Playlist $index: nombre vacío." }
        }
    }

    @OptIn(ExperimentalSerializationApi::class, InternalSerializationApi::class)
    private fun writeDocument(
        document: PlaylistBackupDocument,
        output: OutputStream,
    ) {
        json.encodeToStream(document, output)
    }

    @OptIn(ExperimentalSerializationApi::class, InternalSerializationApi::class)
    private fun decodeDocument(input: InputStream): PlaylistBackupDocument = try {
        json.decodeFromStream<PlaylistBackupDocument>(input)
    } catch (error: BackupTooLargeException) {
        throw error
    } catch (error: Exception) {
        throw IllegalArgumentException("El archivo no es un respaldo de playlists válido.", error)
    }

    private fun tryReadCoverBase64(coverUri: String): String? = runCatching {
        val file = when {
            coverUri.startsWith("/") -> File(coverUri)
            coverUri.startsWith("file://") -> coverUri.toUri().path?.let(::File)
            else -> null
        }
        if (file != null && file.exists() && file.length() in 1..MAX_COVER_BYTES) {
            Base64.getEncoder().encodeToString(file.readBytes())
        } else null
    }.getOrNull()

    private fun saveCoverFromBase64(playlistId: String, base64: String): String? = runCatching {
        val bytes = Base64.getDecoder().decode(base64)
        if (bytes.size > MAX_COVER_BYTES) return null
        val coversDir = context.filesDir.resolve("playlist_covers").apply { mkdirs() }
        val target = File(coversDir, "$playlistId-${UUID.randomUUID()}.cover")
        target.writeBytes(bytes)
        target.absolutePath
    }.getOrNull()

    companion object {
        const val BACKUP_FORMAT = "catlytics.playlists.backup"
        const val UNIFIED_BACKUP_FORMAT = "catlytics.backup"
        const val LEGACY_STATISTICS_FORMAT = "catlytics.statistics.backup"
        const val SUPPORTED_SCHEMA_VERSION = 3
        const val MIN_SUPPORTED_SCHEMA_VERSION = 1
        internal const val MAX_BACKUP_BYTES = 64L * 1024L * 1024L
        internal const val MAX_COVER_BYTES = 2L * 1024L * 1024L
    }
}

internal class TrackReconciler(localTracks: List<TrackEntity>) {
    private val byId = localTracks.associateBy { it.id }
    private val byArtistAndTitle = localTracks.groupBy {
        artistIdentityKey(it.artistName) to normalizeTitle(it.title)
    }
    private val byTitle = localTracks.groupBy { normalizeTitle(it.title) }

    fun reconcile(backupTrack: PlaylistTrackBackupDto): TrackEntity? {
        // Nivel 1: ID exacto y título coincidente
        val byExactId = byId[backupTrack.trackId]
        if (byExactId != null && normalizeTitle(byExactId.title) == normalizeTitle(backupTrack.title)) {
            return byExactId
        }

        // Nivel 2: Artista + Título normalizado
        val artistKey = artistIdentityKey(backupTrack.artistName)
        val titleKey = normalizeTitle(backupTrack.title)
        val candidateMatches = byArtistAndTitle[artistKey to titleKey]
        if (!candidateMatches.isNullOrEmpty()) {
            if (candidateMatches.size == 1) return candidateMatches.first()
            return candidateMatches.minByOrNull {
                kotlin.math.abs(it.durationMillis - backupTrack.durationMillis)
            }
        }

        // Nivel 3: Título coincidente con duración cercana (±3000ms)
        val titleMatches = byTitle[titleKey]
        if (!titleMatches.isNullOrEmpty()) {
            val closeDuration = titleMatches.filter {
                kotlin.math.abs(it.durationMillis - backupTrack.durationMillis) <= 3000L
            }
            if (closeDuration.isNotEmpty()) {
                return closeDuration.first()
            }
        }

        return null
    }

    companion object {
        fun normalizeTitle(title: String): String = Normalizer
            .normalize(title.trim(), Normalizer.Form.NFKC)
            .lowercase(Locale.ROOT)
            .replace(Regex("\\s+"), " ")
    }
}

@OptIn(ExperimentalSerializationApi::class, InternalSerializationApi::class)
@Serializable
internal data class PlaylistBackupDocument(
    val format: String,
    val schemaVersion: Int,
    val exportedAtMillis: Long,
    val appVersion: String = "",
    val playlists: List<PlaylistBackupDto> = emptyList(),
)

@OptIn(ExperimentalSerializationApi::class, InternalSerializationApi::class)
@Serializable
internal data class PlaylistBackupDto(
    val id: String,
    val name: String,
    val description: String = "",
    val isLikedPlaylist: Boolean = false,
    val coverBase64: String? = null,
    val tracks: List<PlaylistTrackBackupDto> = emptyList(),
)

@OptIn(ExperimentalSerializationApi::class, InternalSerializationApi::class)
@Serializable
internal data class PlaylistTrackBackupDto(
    val trackId: String,
    val title: String,
    val artistName: String,
    val albumTitle: String? = null,
    val durationMillis: Long = 0L,
    val relativePath: String? = null,
    val trackNumber: Int? = null,
)


