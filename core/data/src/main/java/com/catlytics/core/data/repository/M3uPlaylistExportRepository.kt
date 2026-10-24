package com.catlytics.core.data.repository

import android.content.Context
import androidx.core.net.toUri
import com.catlytics.core.data.local.LocalDataSource
import com.catlytics.core.data.model.TrackEntity
import com.catlytics.core.domain.repository.PlaylistExportRepository
import com.catlytics.core.domain.repository.PlaylistRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class M3uPlaylistExportRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val playlistRepository: PlaylistRepository,
    private val localDataSource: LocalDataSource,
) : PlaylistExportRepository {
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO

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
                    appendLine(track.m3uLocation())
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
}


internal fun TrackEntity.m3uLocation(): String {
    val key = fileKey ?: return mediaUri
    val folder = folderPath ?: return mediaUri
    val displayName = key.substringAfterLast('/')
    val volume = key.substringBefore(':')
    val root = when (volume) {
        // Before Android 10 the folder path is already absolute (from MediaStore DATA).
        "external" -> ""
        "external_primary" -> "/storage/emulated/0"
        else -> "/storage/${volume.uppercase(Locale.ROOT)}"
    }
    return "$root/$folder/$displayName"
}
