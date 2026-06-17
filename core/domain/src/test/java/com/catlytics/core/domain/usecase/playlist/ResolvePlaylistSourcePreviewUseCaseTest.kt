package com.catlytics.core.domain.usecase.playlist

import com.catlytics.core.domain.repository.LibraryRepository
import com.catlytics.core.model.Album
import com.catlytics.core.model.Artist
import com.catlytics.core.model.ArtistSummary
import com.catlytics.core.model.LibraryFolder
import com.catlytics.core.model.PlaylistSource
import com.catlytics.core.model.Track
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ResolvePlaylistSourcePreviewUseCaseTest {
    @Test
    fun `album source uses album artwork and metadata`() = runTest {
        val repository = FakeLibraryRepository(
            albums = listOf(
                Album(
                    id = "album-1",
                    title = "Midnight",
                    artist = Artist(id = "artist-1", name = "Nova"),
                    artworkUri = "content://album-art/1",
                    trackCount = 2,
                ),
            ),
            tracks = listOf(
                track(id = "t1", albumId = "album-1", albumTitle = "Midnight"),
                track(id = "t2", albumId = "album-1", albumTitle = "Midnight"),
            ),
        )
        val useCase = ResolvePlaylistSourcePreviewUseCase(repository)

        val preview = useCase(PlaylistSource.AlbumSource("album-1"))

        assertEquals("Midnight", preview.title)
        assertEquals("Nova", preview.subtitle)
        assertEquals("content://album-art/1", preview.artworkUri)
        assertEquals(2, preview.itemCount)
        assertEquals(listOf("t1", "t2"), preview.trackIds)
    }

    @Test
    fun `album source falls back to track artwork when album is missing`() = runTest {
        val repository = FakeLibraryRepository(
            tracks = listOf(
                track(
                    id = "t1",
                    albumId = "album-missing",
                    albumTitle = "Hidden",
                    artworkUri = "content://track-art/1",
                ),
            ),
        )
        val useCase = ResolvePlaylistSourcePreviewUseCase(repository)

        val preview = useCase(PlaylistSource.AlbumSource("album-missing"))

        assertEquals("Hidden", preview.title)
        assertEquals("content://track-art/1", preview.artworkUri)
        assertEquals(1, preview.itemCount)
    }

    private fun track(
        id: String,
        albumId: String? = null,
        albumTitle: String? = null,
        artworkUri: String? = null,
    ) = Track(
        id = id,
        title = "Song $id",
        artist = Artist(id = "artist-1", name = "Nova"),
        durationMillis = 180_000L,
        mediaUri = "content://media/$id",
        artworkUri = artworkUri,
        albumId = albumId,
        albumTitle = albumTitle,
    )

    private class FakeLibraryRepository(
        private val albums: List<Album> = emptyList(),
        private val artists: List<ArtistSummary> = emptyList(),
        private val folders: List<LibraryFolder> = emptyList(),
        private val tracks: List<Track> = emptyList(),
    ) : LibraryRepository {
        override fun observeAlbums(): Flow<List<Album>> = MutableStateFlow(albums)
        override fun observeAlbumContent(albumId: String) = MutableStateFlow(null)
        override fun observeArtists(): Flow<List<ArtistSummary>> = MutableStateFlow(artists)
        override fun observeArtistContent(artistId: String) = MutableStateFlow(null)
        override fun observeTracks(): Flow<List<Track>> = MutableStateFlow(tracks)
        override fun observeAllTracks(): Flow<List<Track>> = MutableStateFlow(tracks)
        override fun observeFolders(): Flow<List<LibraryFolder>> = MutableStateFlow(folders)
        override fun observeFolderContent(folderId: String) = MutableStateFlow(null)
        override suspend fun resolvePlaylistSource(source: PlaylistSource): List<Track> =
            when (source) {
                is PlaylistSource.TrackSource -> tracks.filter { it.id == source.trackId }
                is PlaylistSource.AlbumSource -> tracks.filter { it.albumId == source.albumId }
                is PlaylistSource.ArtistSource -> tracks.filter { it.artist.id == source.artistId }
                is PlaylistSource.FolderSource -> emptyList()
            }
        override suspend fun refreshTracks() = Unit
        override suspend fun setFolderVisible(folderId: String, visible: Boolean) = Unit
    }
}