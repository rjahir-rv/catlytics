package com.catlytics.core.domain.usecase.playlist

import com.catlytics.core.domain.repository.LibraryRepository
import com.catlytics.core.domain.repository.PlaylistRepository
import com.catlytics.core.model.LIKED_PLAYLIST_ID
import com.catlytics.core.model.Playlist
import com.catlytics.core.model.PlaylistContent
import com.catlytics.core.model.PlaylistSource
import com.catlytics.core.model.PlaylistSourcePreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class ObservePlaylistContentUseCase(
    private val playlistRepository: PlaylistRepository,
    private val libraryRepository: LibraryRepository,
) {
    operator fun invoke(playlistId: String): Flow<PlaylistContent?> = combine(
        playlistRepository.observePlaylists(),
        libraryRepository.observeAllTracks(),
    ) { playlists, tracks ->
        val playlist = playlists.firstOrNull { it.id == playlistId } ?: return@combine null
        val tracksById = tracks.associateBy { it.id }
        PlaylistContent(
            playlist = playlist,
            tracks = playlist.trackIds.mapNotNull(tracksById::get),
        )
    }
}

class CreatePlaylistUseCase(private val repository: PlaylistRepository) {
    suspend operator fun invoke(name: String): Playlist {
        val normalized = name.trim()
        require(normalized.isNotEmpty()) { "El nombre no puede estar vacío." }
        return repository.createPlaylist(normalized)
    }
}

class RenamePlaylistUseCase(private val repository: PlaylistRepository) {
    suspend operator fun invoke(id: String, name: String) {
        val normalized = name.trim()
        require(normalized.isNotEmpty()) { "El nombre no puede estar vacío." }
        repository.renamePlaylist(id, normalized)
    }
}

class DeletePlaylistUseCase(private val repository: PlaylistRepository) {
    suspend operator fun invoke(id: String) = repository.deletePlaylist(id)
}

class AddToPlaylistUseCase(
    private val playlistRepository: PlaylistRepository,
    private val libraryRepository: LibraryRepository,
) {
    suspend operator fun invoke(playlistId: String, source: PlaylistSource): Int =
        addToPlaylists(listOf(playlistId), source)[playlistId] ?: 0

    suspend fun addToPlaylists(
        playlistIds: Collection<String>,
        source: PlaylistSource,
    ): Map<String, Int> {
        if (playlistIds.isEmpty()) return emptyMap()
        val trackIds = libraryRepository.resolvePlaylistSource(source).map { it.id }
        if (trackIds.isEmpty()) return emptyMap()
        return playlistRepository.addTracksToPlaylists(playlistIds, trackIds)
    }
}

class ResolvePlaylistSourcePreviewUseCase(
    private val libraryRepository: LibraryRepository,
) {
    suspend operator fun invoke(source: PlaylistSource): PlaylistSourcePreview {
        val tracks = libraryRepository.resolvePlaylistSource(source)
        val trackIds = tracks.map { it.id }
        return when (source) {
            is PlaylistSource.TrackSource -> {
                val track = tracks.firstOrNull()
                PlaylistSourcePreview(
                    title = track?.title ?: "Canción",
                    subtitle = track?.artist?.name,
                    artworkUri = track?.artworkUri,
                    itemCount = tracks.size,
                    trackIds = trackIds,
                )
            }
            is PlaylistSource.AlbumSource -> {
                val album = libraryRepository.observeAlbums().first()
                    .firstOrNull { it.id == source.albumId }
                PlaylistSourcePreview(
                    title = album?.title ?: tracks.firstOrNull()?.albumTitle ?: "Álbum",
                    subtitle = album?.artist?.name ?: tracks.firstOrNull()?.artist?.name,
                    artworkUri = album?.artworkUri ?: tracks.firstNotNullOfOrNull { it.artworkUri },
                    itemCount = tracks.size,
                    trackIds = trackIds,
                )
            }
            is PlaylistSource.ArtistSource -> {
                val artist = libraryRepository.observeArtists().first()
                    .firstOrNull { it.artist.id == source.artistId }
                PlaylistSourcePreview(
                    title = artist?.artist?.name ?: tracks.firstOrNull()?.artist?.name ?: "Artista",
                    subtitle = artist?.let { artistSummary ->
                        if (artistSummary.albumCount == 1) {
                            "1 álbum"
                        } else {
                            "${artistSummary.albumCount} álbumes"
                        }
                    },
                    artworkUri = artist?.artworkUri ?: tracks.firstNotNullOfOrNull { it.artworkUri },
                    itemCount = tracks.size,
                    trackIds = trackIds,
                )
            }
            is PlaylistSource.FolderSource -> {
                val folder = libraryRepository.observeFolders().first()
                    .firstOrNull { it.id == source.folderId }
                PlaylistSourcePreview(
                    title = folder?.name ?: "Carpeta",
                    subtitle = folder?.path,
                    artworkUri = tracks.firstNotNullOfOrNull { it.artworkUri },
                    itemCount = tracks.size,
                    trackIds = trackIds,
                )
            }
        }
    }
}

enum class ToggleLikedTrackResult {
    Added,
    Removed,
}

class ToggleLikedTrackUseCase(private val repository: PlaylistRepository) {
    suspend operator fun invoke(trackId: String): ToggleLikedTrackResult {
        val likedTrackIds = repository.observePlaylists()
            .first()
            .firstOrNull { it.id == LIKED_PLAYLIST_ID }
            ?.trackIds
            .orEmpty()

        return if (trackId in likedTrackIds) {
            repository.removeTrack(LIKED_PLAYLIST_ID, trackId)
            ToggleLikedTrackResult.Removed
        } else {
            repository.addTracks(LIKED_PLAYLIST_ID, listOf(trackId))
            ToggleLikedTrackResult.Added
        }
    }
}

class ObserveIsTrackLikedUseCase(private val repository: PlaylistRepository) {
    operator fun invoke(trackId: String?): Flow<Boolean> = repository.observePlaylists()
        .map { playlists ->
            val likedTrackIds = playlists
                .firstOrNull { it.id == LIKED_PLAYLIST_ID }
                ?.trackIds
                .orEmpty()
            trackId != null && trackId in likedTrackIds
        }
}

class RemoveTrackFromPlaylistUseCase(private val repository: PlaylistRepository) {
    suspend operator fun invoke(playlistId: String, trackId: String) =
        repository.removeTrack(playlistId, trackId)
}
