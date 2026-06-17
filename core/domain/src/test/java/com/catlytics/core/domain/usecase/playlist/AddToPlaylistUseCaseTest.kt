package com.catlytics.core.domain.usecase.playlist

import com.catlytics.core.domain.repository.LibraryRepository
import com.catlytics.core.domain.repository.PlaylistRepository
import com.catlytics.core.model.Album
import com.catlytics.core.model.AlbumContent
import com.catlytics.core.model.Artist
import com.catlytics.core.model.ArtistContent
import com.catlytics.core.model.ArtistSummary
import com.catlytics.core.model.LibraryFolder
import com.catlytics.core.model.LibraryFolderContent
import com.catlytics.core.model.Playlist
import com.catlytics.core.model.PlaylistSource
import com.catlytics.core.model.Track
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class AddToPlaylistUseCaseTest {
    @Test
    fun `addToPlaylists adds source track to every selected playlist`() = runTest {
        val playlistRepository = FakePlaylistRepository(
            playlists = listOf(
                Playlist(id = "playlist-1", name = "Focus", trackIds = emptyList()),
                Playlist(id = "playlist-2", name = "Chill", trackIds = emptyList()),
            ),
        )
        val libraryRepository = FakeLibraryRepository(
            tracks = listOf(
                Track(
                    id = "track-1",
                    title = "Song",
                    artist = Artist(id = "artist-1", name = "Artist"),
                    durationMillis = 180_000L,
                    mediaUri = "content://track-1",
                ),
            ),
        )
        val useCase = AddToPlaylistUseCase(playlistRepository, libraryRepository)

        val added = useCase.addToPlaylists(
            playlistIds = listOf("playlist-1", "playlist-2"),
            source = PlaylistSource.TrackSource("track-1"),
        )

        assertEquals(1, added["playlist-1"])
        assertEquals(1, added["playlist-2"])
        val playlists = playlistRepository.playlists.value
        assertEquals(listOf("track-1"), playlists.first { it.id == "playlist-1" }.trackIds)
        assertEquals(listOf("track-1"), playlists.first { it.id == "playlist-2" }.trackIds)
    }

    private class FakePlaylistRepository(
        playlists: List<Playlist>,
    ) : PlaylistRepository {
        val playlists = MutableStateFlow(playlists)

        override fun observePlaylists(): Flow<List<Playlist>> = playlists

        override suspend fun createPlaylist(name: String, trackIds: List<String>): Playlist {
            val playlist = Playlist("playlist-${playlists.value.size}", name, trackIds)
            playlists.value = playlists.value + playlist
            return playlist
        }

        override suspend fun renamePlaylist(playlistId: String, name: String) = Unit
        override suspend fun deletePlaylist(playlistId: String) = Unit

        override suspend fun addTracks(playlistId: String, trackIds: List<String>): Int =
            addTracksToPlaylists(listOf(playlistId), trackIds)[playlistId] ?: 0

        override suspend fun addTracksToPlaylists(
            playlistIds: Collection<String>,
            trackIds: List<String>,
        ): Map<String, Int> {
            val distinctTrackIds = trackIds.distinct()
            val addedByPlaylist = mutableMapOf<String, Int>()
            playlists.value = playlists.value.map { playlist ->
                if (playlist.id !in playlistIds) return@map playlist
                val newIds = distinctTrackIds.filterNot(playlist.trackIds::contains)
                addedByPlaylist[playlist.id] = newIds.size
                playlist.copy(trackIds = playlist.trackIds + newIds)
            }
            return addedByPlaylist
        }

        override suspend fun removeTrack(playlistId: String, trackId: String) = Unit
        override suspend fun setPlaylistArtwork(playlistId: String, artworkUri: String?) = Unit
    }

    private class FakeLibraryRepository(
        private val tracks: List<Track>,
    ) : LibraryRepository {
        override fun observeAlbums(): Flow<List<Album>> = MutableStateFlow(emptyList())
        override fun observeAlbumContent(albumId: String): Flow<AlbumContent?> = MutableStateFlow(null)
        override fun observeArtists(): Flow<List<ArtistSummary>> = MutableStateFlow(emptyList())
        override fun observeArtistContent(artistId: String): Flow<ArtistContent?> = MutableStateFlow(null)
        override fun observeTracks(): Flow<List<Track>> = MutableStateFlow(tracks)
        override fun observeAllTracks(): Flow<List<Track>> = MutableStateFlow(tracks)
        override fun observeFolders(): Flow<List<LibraryFolder>> = MutableStateFlow(emptyList())
        override fun observeFolderContent(folderId: String): Flow<LibraryFolderContent?> = MutableStateFlow(null)
        override suspend fun resolvePlaylistSource(source: PlaylistSource): List<Track> =
            when (source) {
                is PlaylistSource.TrackSource -> tracks.filter { it.id == source.trackId }
                else -> emptyList()
            }
        override suspend fun refreshTracks() = Unit
        override suspend fun setFolderVisible(folderId: String, visible: Boolean) = Unit
    }
}