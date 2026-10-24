package com.catlytics.core.data.repository

import android.content.Context
import com.catlytics.core.data.local.LocalDataSource
import com.catlytics.core.data.local.ManagedImageStore
import com.catlytics.core.data.model.TrackEntity
import com.catlytics.core.domain.repository.PlaylistRepository
import com.catlytics.core.model.LIKED_PLAYLIST_ID
import com.catlytics.core.model.LIKED_PLAYLIST_NAME
import com.catlytics.core.model.Playlist
import com.catlytics.core.model.PlaylistBackupPreview
import com.catlytics.core.model.PlaylistBackupSummary
import com.catlytics.core.model.PlaylistExportResult
import com.catlytics.core.model.PlaylistImportResult
import com.catlytics.core.model.StatisticsImportMode
import com.catlytics.core.model.UNKNOWN_ARTIST_NAME
import com.catlytics.core.model.UNTITLED_TRACK_TITLE
import com.catlytics.core.model.artistIdentityKey
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

@Singleton
class PlaylistBackupSection @Inject constructor(
    @ApplicationContext context: Context,
    private val playlistRepository: PlaylistRepository,
    private val localDataSource: LocalDataSource,
) {
    private val coverStore = ManagedImageStore(context, DataStorePlaylistRepository.COVER_DIRECTORY)

    fun observeSummary(): Flow<PlaylistBackupSummary> =
        playlistRepository.observePlaylists().map { playlists ->
            val liked = playlists.firstOrNull { it.id == LIKED_PLAYLIST_ID }
            val nonLiked = playlists.filterNot { it.id == LIKED_PLAYLIST_ID }
            PlaylistBackupSummary(
                playlistCount = nonLiked.size,
                totalTracks = nonLiked.sumOf { it.trackIds.size },
                likedTracksCount = liked?.trackIds?.size ?: 0,
            )
        }

    internal suspend fun export(): Pair<List<PlaylistBackupDto>, PlaylistExportResult> {
        val playlists = playlistRepository.observePlaylists().first()
        val tracksById = localDataSource.observeTracks().first().associateBy { it.id }

        val playlistDtos = playlists.map { playlist ->
            val trackDtos = playlist.trackIds.map { trackId ->
                val track = tracksById[trackId]
                PlaylistTrackBackupDto(
                    trackId = trackId,
                    title = track?.title ?: UNTITLED_TRACK_TITLE,
                    artistName = track?.artistName ?: UNKNOWN_ARTIST_NAME,
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

    internal suspend fun preview(
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
            if (playlistDto.isLiked) {
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

        val nonLikedCount = playlists.count { !it.isLiked }

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

    internal suspend fun restore(
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

            val playlistId = if (playlistDto.isLiked) {
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

    private fun tryReadCoverBase64(coverUri: String): String? =
        coverStore.read(coverUri, ManagedImageStore.MAX_IMAGE_BYTES)?.let(Base64.getEncoder()::encodeToString)

    private fun saveCoverFromBase64(playlistId: String, base64: String): String? {
        val bytes = runCatching { Base64.getDecoder().decode(base64) }.getOrNull() ?: return null
        if (bytes.isEmpty() || bytes.size > ManagedImageStore.MAX_IMAGE_BYTES) return null
        return coverStore.write(bytes, playlistId)
    }
}

internal class TrackReconciler(localTracks: List<TrackEntity>) {
    private val byId = localTracks.associateBy { it.id }
    private val byArtistAndTitle = localTracks.groupBy {
        artistIdentityKey(it.artistName) to artistIdentityKey(it.title)
    }
    private val byTitle = localTracks.groupBy { artistIdentityKey(it.title) }

    fun reconcile(backupTrack: PlaylistTrackBackupDto): TrackEntity? {
        val byExactId = byId[backupTrack.trackId]
        if (byExactId != null && artistIdentityKey(byExactId.title) == artistIdentityKey(backupTrack.title)) {
            return byExactId
        }

        val artistKey = artistIdentityKey(backupTrack.artistName)
        val titleKey = artistIdentityKey(backupTrack.title)
        val candidateMatches = byArtistAndTitle[artistKey to titleKey]
        if (!candidateMatches.isNullOrEmpty()) {
            if (candidateMatches.size == 1) return candidateMatches.first()
            return candidateMatches.minByOrNull {
                kotlin.math.abs(it.durationMillis - backupTrack.durationMillis)
            }
        }
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
}
