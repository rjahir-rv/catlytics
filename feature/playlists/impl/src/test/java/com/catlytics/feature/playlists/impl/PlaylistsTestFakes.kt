package com.catlytics.feature.playlists.impl

import com.catlytics.core.domain.repository.LibraryPreferencesRepository
import com.catlytics.core.domain.repository.LibraryRepository
import com.catlytics.core.domain.repository.PlaybackController
import com.catlytics.core.domain.repository.PlaylistExportRepository
import com.catlytics.core.domain.repository.PlaylistRepository
import com.catlytics.core.model.Album
import com.catlytics.core.model.AlbumContent
import com.catlytics.core.model.AlbumTrackSort
import com.catlytics.core.model.Artist
import com.catlytics.core.model.ArtistAlbumSort
import com.catlytics.core.model.ArtistContent
import com.catlytics.core.model.ArtistSummary
import com.catlytics.core.model.ArtistTrackSort
import com.catlytics.core.model.ArtistViewMode
import com.catlytics.core.model.DetailSortSettings
import com.catlytics.core.model.LibraryFolder
import com.catlytics.core.model.LibraryFolderContent
import com.catlytics.core.model.MusicScanDurationFilter
import com.catlytics.core.model.MusicScanSettings
import com.catlytics.core.model.MusicScanSizeFilter
import com.catlytics.core.model.PlaybackQueueSource
import com.catlytics.core.model.PlaybackRepeatMode
import com.catlytics.core.model.PlaybackState
import com.catlytics.core.model.Playlist
import com.catlytics.core.model.PlaylistSource
import com.catlytics.core.model.PlaylistViewMode
import com.catlytics.core.model.SortDirection
import com.catlytics.core.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    private val testDispatcher: TestDispatcher = StandardTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}

fun track(id: String, durationMillis: Long = 200_000L) = Track(
    id = id,
    title = "Title $id",
    artist = Artist(id = "artist-$id", name = "Artist $id"),
    durationMillis = durationMillis,
    mediaUri = "content://media/external/audio/media/$id",
)

class FakePlaylistRepository(vararg initial: Playlist) : PlaylistRepository {
    val playlists = MutableStateFlow(initial.toList())
    var failure: Exception? = null
    private var nextId = 1

    override fun observePlaylists(): Flow<List<Playlist>> = playlists

    override suspend fun createPlaylist(name: String, trackIds: List<String>): Playlist {
        val playlist = Playlist(id = "created-${nextId++}", name = name, trackIds = trackIds)
        playlists.update { it + playlist }
        return playlist
    }

    override suspend fun renamePlaylist(playlistId: String, name: String) =
        edit(playlistId) { it.copy(name = name) }

    override suspend fun updatePlaylistDetails(playlistId: String, name: String, description: String) =
        edit(playlistId) { it.copy(name = name, description = description) }

    override suspend fun deletePlaylist(playlistId: String) {
        failure?.let { throw it }
        playlists.update { list -> list.filterNot { it.id == playlistId } }
    }

    override suspend fun addTracks(playlistId: String, trackIds: List<String>): Int =
        addTracksToPlaylists(listOf(playlistId), trackIds)[playlistId] ?: 0

    override suspend fun addTracksToPlaylists(
        playlistIds: Collection<String>,
        trackIds: List<String>,
    ): Map<String, Int> {
        val added = mutableMapOf<String, Int>()
        playlistIds.forEach { id ->
            edit(id) { playlist ->
                val newIds = trackIds.distinct().filterNot { it in playlist.trackIds }
                added[id] = newIds.size
                playlist.copy(trackIds = playlist.trackIds + newIds)
            }
        }
        return added
    }

    override suspend fun removeTrack(playlistId: String, trackId: String) {
        removeTracks(playlistId, listOf(trackId))
    }

    override suspend fun removeTracks(playlistId: String, trackIds: Collection<String>): Int {
        var removed = 0
        edit(playlistId) { playlist ->
            removed = playlist.trackIds.count { it in trackIds }
            playlist.copy(trackIds = playlist.trackIds - trackIds.toSet())
        }
        return removed
    }

    override suspend fun reorderTracks(playlistId: String, orderedTrackIds: List<String>) =
        edit(playlistId) { it.copy(trackIds = orderedTrackIds) }

    override suspend fun setPlaylistArtwork(playlistId: String, artworkUri: String?) =
        edit(playlistId) { it.copy(artworkUri = artworkUri) }

    fun playlist(id: String): Playlist? = playlists.value.firstOrNull { it.id == id }

    private suspend fun edit(playlistId: String, transform: (Playlist) -> Playlist) {
        failure?.let { throw it }
        playlists.update { list -> list.map { if (it.id == playlistId) transform(it) else it } }
    }
}

class FakeLibraryRepository(vararg initial: Track) : LibraryRepository {
    val tracks = MutableStateFlow(initial.toList())
    var resolveFailure: Exception? = null

    override fun observeAlbums(): Flow<List<Album>> = flowOf(emptyList())
    override fun observeAlbumContent(albumId: String): Flow<AlbumContent?> = flowOf(null)
    override fun observeArtists(): Flow<List<ArtistSummary>> = flowOf(emptyList())
    override fun observeArtistContent(artistId: String): Flow<ArtistContent?> = flowOf(null)
    override fun observeTracks(): Flow<List<Track>> = tracks
    override fun observeAllTracks(): Flow<List<Track>> = tracks
    override fun observeFolders(): Flow<List<LibraryFolder>> = flowOf(emptyList())
    override fun observeFolderContent(folderId: String): Flow<LibraryFolderContent?> = flowOf(null)

    override suspend fun resolvePlaylistSource(source: PlaylistSource): List<Track> {
        resolveFailure?.let { throw it }
        val ids = (source as PlaylistSource.TrackCollectionSource).trackIds
        return tracks.value.filter { it.id in ids }
    }

    override suspend fun refreshTracks(): Int = tracks.value.size
    override suspend fun setFolderVisible(folderId: String, visible: Boolean) = Unit
}

data class PlayRequest(
    val track: Track,
    val queue: List<Track>,
    val startIndex: Int,
    val queueSource: PlaybackQueueSource,
)

class FakePlaybackController : PlaybackController {
    override val playbackState = MutableStateFlow(PlaybackState())
    val playRequests = mutableListOf<PlayRequest>()
    var shuffleEnabled = false

    override suspend fun play(
        track: Track,
        queue: List<Track>,
        startIndex: Int,
        queueSource: PlaybackQueueSource,
    ) {
        playRequests += PlayRequest(track, queue, startIndex, queueSource)
    }

    override suspend fun setShuffleEnabled(enabled: Boolean) {
        shuffleEnabled = enabled
    }

    override suspend fun playQueueItem(index: Int) = Unit
    override suspend fun addQueueItem(track: Track) = Unit
    override suspend fun playNext(track: Track) = Unit
    override suspend fun moveQueueItem(fromIndex: Int, toIndex: Int) = Unit
    override suspend fun removeQueueItem(index: Int) = Unit
    override suspend fun togglePlayPause() = Unit
    override suspend fun pause() = Unit
    override suspend fun skipNext() = Unit
    override suspend fun skipPrevious() = Unit
    override suspend fun seekTo(positionMillis: Long) = Unit
    override suspend fun setRepeatMode(mode: PlaybackRepeatMode) = Unit
    override suspend fun restoreLastSession() = Unit
    override suspend fun stop() = Unit
}

class FakePlaylistExportRepository : PlaylistExportRepository {
    var result: Result<Unit> = Result.success(Unit)
    val exports = mutableListOf<Pair<String, String>>()

    override suspend fun exportPlaylistToM3uUri(playlistId: String, uri: String): Result<Unit> {
        exports += playlistId to uri
        return result
    }
}

class FakeLibraryPreferencesRepository : LibraryPreferencesRepository {
    val playlistViewMode = MutableStateFlow(PlaylistViewMode.List)
    val playlistSortDirection = MutableStateFlow(SortDirection.Ascending)

    override fun observeHiddenFolderIds(): Flow<Set<String>> = flowOf(emptySet())
    override fun observeMusicScanSettings(): Flow<MusicScanSettings> = flowOf(MusicScanSettings())
    override fun observeArtistViewMode(): Flow<ArtistViewMode> = flowOf(ArtistViewMode.List)
    override fun observePlaylistViewMode(): Flow<PlaylistViewMode> = playlistViewMode
    override fun observeLibrarySortDirection(): Flow<SortDirection> = flowOf(SortDirection.Ascending)
    override fun observePlaylistSortDirection(): Flow<SortDirection> = playlistSortDirection
    override fun observeDetailSortSettings(): Flow<DetailSortSettings> = flowOf(DetailSortSettings())
    override suspend fun setAlbumTrackSort(sort: AlbumTrackSort) = Unit
    override suspend fun setArtistTrackSort(sort: ArtistTrackSort) = Unit
    override suspend fun setArtistAlbumSort(sort: ArtistAlbumSort) = Unit
    override suspend fun setFolderVisible(folderId: String, visible: Boolean) = Unit
    override suspend fun setMusicScanDurationFilter(filter: MusicScanDurationFilter) = Unit
    override suspend fun setMusicScanSizeFilter(filter: MusicScanSizeFilter) = Unit
    override suspend fun setArtistViewMode(viewMode: ArtistViewMode) = Unit

    override suspend fun setPlaylistViewMode(viewMode: PlaylistViewMode) {
        playlistViewMode.value = viewMode
    }

    override suspend fun setLibrarySortDirection(direction: SortDirection) = Unit

    override suspend fun setPlaylistSortDirection(direction: SortDirection) {
        playlistSortDirection.value = direction
    }
}
