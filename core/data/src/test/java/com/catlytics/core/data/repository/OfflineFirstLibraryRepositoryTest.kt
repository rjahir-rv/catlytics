package com.catlytics.core.data.repository

import com.catlytics.core.data.local.InMemoryLocalDataSource
import com.catlytics.core.data.mediator.DataMediator
import com.catlytics.core.data.model.TrackEntity
import com.catlytics.core.domain.repository.LibraryPreferencesRepository
import com.catlytics.core.model.ArtistViewMode
import com.catlytics.core.model.PlaylistViewMode
import com.catlytics.core.model.SortDirection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class OfflineFirstLibraryRepositoryTest {
    private val localDataSource = InMemoryLocalDataSource()
    private val preferencesRepository = FakeLibraryPreferencesRepository()
    private val repository = OfflineFirstLibraryRepository(
        localDataSource = localDataSource,
        mediator = NoOpDataMediator,
        preferencesRepository = preferencesRepository,
    )

    @Test
    fun `hidden folder is filtered while remaining available in folder list`() = runTest {
        localDataSource.replaceTracks(
            listOf(
                track("music", FAVORITES_FOLDER_ID, "Favorites", "Music/Favorites"),
                track("game", GAME_FOLDER_ID, "audio", "Android/data/game/audio"),
                track("remote"),
            ),
        )

        repository.setFolderVisible(ANDROID_BASE_FOLDER_ID, visible = false)

        assertEquals(listOf("music", "remote"), repository.observeTracks().first().map { it.id })
        val folders = repository.observeFolders().first()
        assertEquals(2, folders.size)
        assertEquals(false, folders.first { it.id == ANDROID_BASE_FOLDER_ID }.isVisible)
    }

    @Test
    fun `albums group visible tracks and are sorted by title`() = runTest {
        localDataSource.replaceTracks(
            listOf(
                track("z-first", albumId = "album-z", albumTitle = "Zulu"),
                track("a-first", albumId = "album-a", albumTitle = "Alpha"),
                track("a-second", albumId = "album-a", albumTitle = "Alpha"),
            ),
        )

        val albums = repository.observeAlbums().first()

        assertEquals(listOf("Alpha", "Zulu"), albums.map { it.title })
        assertEquals(2, albums.first().trackCount)
    }

    @Test
    fun `albums exclude tracks from hidden folders`() = runTest {
        localDataSource.replaceTracks(
            listOf(
                track(
                    id = "visible",
                    folderId = FAVORITES_FOLDER_ID,
                    folderName = "Favorites",
                    folderPath = "Music/Favorites",
                    albumId = "album-visible",
                    albumTitle = "Visible",
                ),
                track(
                    id = "hidden",
                    folderId = GAME_FOLDER_ID,
                    folderName = "audio",
                    folderPath = "Android/data/game/audio",
                    albumId = "album-hidden",
                    albumTitle = "Hidden",
                ),
            ),
        )
        repository.setFolderVisible(ANDROID_BASE_FOLDER_ID, visible = false)

        assertEquals(listOf("Visible"), repository.observeAlbums().first().map { it.title })
    }

    @Test
    fun `album content orders numbered tracks before unnumbered tracks`() = runTest {
        localDataSource.replaceTracks(
            listOf(
                track("unknown", albumId = "album-a", albumTitle = "Alpha"),
                track("second", albumId = "album-a", albumTitle = "Alpha", trackNumber = 2),
                track("first", albumId = "album-a", albumTitle = "Alpha", trackNumber = 1),
            ),
        )

        val content = requireNotNull(repository.observeAlbumContent("album-a").first())

        assertEquals(listOf("first", "second", "unknown"), content.tracks.map { it.id })
    }

    @Test
    fun `album content is unavailable when every track is hidden`() = runTest {
        localDataSource.replaceTracks(
            listOf(
                track(
                    id = "hidden",
                    folderId = GAME_FOLDER_ID,
                    folderName = "audio",
                    folderPath = "Android/data/game/audio",
                    albumId = "album-hidden",
                    albumTitle = "Hidden",
                ),
            ),
        )
        repository.setFolderVisible(ANDROID_BASE_FOLDER_ID, visible = false)

        assertEquals(null, repository.observeAlbumContent("album-hidden").first())
    }

    @Test
    fun `subfolders from the same base folder are grouped`() = runTest {
        localDataSource.replaceTracks(
            listOf(
                track("favorite", FAVORITES_FOLDER_ID, "Favorites", "Music/Favorites"),
                track("album", ALBUMS_FOLDER_ID, "Albums", "Music/Albums"),
            ),
        )

        val folders = repository.observeFolders().first()

        assertEquals(1, folders.size)
        assertEquals(MUSIC_BASE_FOLDER_ID, folders.single().id)
        assertEquals("Music", folders.single().name)
        assertEquals(2, folders.single().trackCount)
    }

    @Test
    fun `folder content returns only direct subfolders and tracks`() = runTest {
        localDataSource.replaceTracks(
            listOf(
                track("root", MUSIC_BASE_FOLDER_ID, "Music", "Music"),
                track("favorite", FAVORITES_FOLDER_ID, "Favorites", "Music/Favorites"),
                track(
                    "live",
                    "$FAVORITES_FOLDER_ID/Live",
                    "Live",
                    "Music/Favorites/Live",
                ),
                track("album", ALBUMS_FOLDER_ID, "Albums", "Music/Albums"),
            ),
        )

        val content = requireNotNull(repository.observeFolderContent(MUSIC_BASE_FOLDER_ID).first())

        assertEquals(listOf(ALBUMS_FOLDER_ID, FAVORITES_FOLDER_ID), content.subfolders.map { it.id })
        assertEquals(listOf("root"), content.tracks.map { it.id })
        assertEquals(4, content.folder.trackCount)
    }

    @Test
    fun `hidden folder content remains available with all tracks`() = runTest {
        localDataSource.replaceTracks(
            listOf(track("favorite", FAVORITES_FOLDER_ID, "Favorites", "Music/Favorites")),
        )
        repository.setFolderVisible(MUSIC_BASE_FOLDER_ID, visible = false)

        val content = requireNotNull(repository.observeFolderContent(FAVORITES_FOLDER_ID).first())

        assertEquals(false, content.folder.isVisible)
        assertEquals(listOf("favorite"), content.tracks.map { it.id })
        assertEquals(emptyList<String>(), repository.observeTracks().first().map { it.id })
        assertEquals(listOf("favorite"), repository.observeAllTracks().first().map { it.id })
    }

    @Test
    fun `folder content does not mix matching paths from different volumes`() = runTest {
        localDataSource.replaceTracks(
            listOf(
                track("primary", FAVORITES_FOLDER_ID, "Favorites", "Music/Favorites"),
                track("sd-card", "external_sd:Music/Albums", "Albums", "Music/Albums"),
            ),
        )

        val content = requireNotNull(repository.observeFolderContent(MUSIC_BASE_FOLDER_ID).first())

        assertEquals(listOf(FAVORITES_FOLDER_ID), content.subfolders.map { it.id })
    }

    @Test
    fun `hiding base folder filters every child folder`() = runTest {
        localDataSource.replaceTracks(
            listOf(
                track("favorite", FAVORITES_FOLDER_ID, "Favorites", "Music/Favorites"),
                track("album", ALBUMS_FOLDER_ID, "Albums", "Music/Albums"),
            ),
        )

        repository.setFolderVisible(MUSIC_BASE_FOLDER_ID, visible = false)

        assertEquals(emptyList<String>(), repository.observeTracks().first().map { it.id })
    }

    @Test
    fun `showing base folder restores its tracks`() = runTest {
        localDataSource.replaceTracks(
            listOf(track("game", GAME_FOLDER_ID, "audio", "Android/data/game/audio")),
        )
        repository.setFolderVisible(ANDROID_BASE_FOLDER_ID, visible = false)
        repository.setFolderVisible(ANDROID_BASE_FOLDER_ID, visible = true)

        assertEquals(listOf("game"), repository.observeTracks().first().map { it.id })
    }

    @Test
    fun `artists group visible tracks with album and track counts`() = runTest {
        localDataSource.replaceTracks(
            listOf(
                track(
                    id = "one",
                    artistId = "artist-a",
                    artistName = "Alpha",
                    albumId = "album-a",
                    albumTitle = "First",
                    artworkUri = "content://artwork/alpha",
                ),
                track(
                    id = "two",
                    artistId = "artist-a",
                    artistName = "Alpha",
                    albumId = "album-b",
                    albumTitle = "Second",
                ),
                track(
                    id = "three",
                    artistId = "artist-z",
                    artistName = "Zulu",
                    albumId = "album-z",
                    albumTitle = "Last",
                ),
            ),
        )

        val artists = repository.observeArtists().first()

        assertEquals(listOf("Alpha", "Zulu"), artists.map { it.artist.name })
        assertEquals(2, artists.first().albumCount)
        assertEquals(2, artists.first().trackCount)
        assertEquals("content://artwork/alpha", artists.first().artworkUri)
    }

    @Test
    fun `artist content includes ordered albums and tracks`() = runTest {
        localDataSource.replaceTracks(
            listOf(
                track(
                    id = "second",
                    artistId = "artist-a",
                    artistName = "Alpha",
                    albumId = "album-b",
                    albumTitle = "Beta",
                    trackNumber = 2,
                ),
                track(
                    id = "first",
                    artistId = "artist-a",
                    artistName = "Alpha",
                    albumId = "album-a",
                    albumTitle = "Alpha",
                    trackNumber = 1,
                ),
            ),
        )

        val content = requireNotNull(repository.observeArtistContent("artist-a").first())

        assertEquals(listOf("Alpha", "Beta"), content.albums.map { it.title })
        assertEquals(listOf("first", "second"), content.tracks.map { it.id })
    }

    @Test
    fun `api 28 physical paths group by shared storage base folder`() = runTest {
        localDataSource.replaceTracks(
            listOf(
                track(
                    "favorite",
                    "external:storage/emulated/0/Music/Favorites",
                    "Favorites",
                    "storage/emulated/0/Music/Favorites",
                ),
                track(
                    "album",
                    "external:storage/emulated/0/Music/Albums",
                    "Albums",
                    "storage/emulated/0/Music/Albums",
                ),
            ),
        )

        val folder = repository.observeFolders().first().single()

        assertEquals("external:storage/emulated/0/Music", folder.id)
        assertEquals("Music", folder.name)
        assertEquals(2, folder.trackCount)
    }

    private fun track(
        id: String,
        folderId: String? = null,
        folderName: String? = null,
        folderPath: String? = null,
        albumId: String? = null,
        albumTitle: String? = null,
        trackNumber: Int? = null,
        artistId: String = "artist-$id",
        artistName: String = "Artist $id",
        artworkUri: String? = null,
    ) = TrackEntity(
        id = id,
        title = "Track $id",
        artistId = artistId,
        artistName = artistName,
        durationMillis = 180_000L,
        mediaUri = "content://media/$id",
        artworkUri = artworkUri,
        albumId = albumId,
        albumTitle = albumTitle,
        trackNumber = trackNumber,
        folderId = folderId,
        folderName = folderName,
        folderPath = folderPath,
    )

    private companion object {
        const val MUSIC_BASE_FOLDER_ID = "external_primary:Music"
        const val FAVORITES_FOLDER_ID = "external_primary:Music/Favorites"
        const val ALBUMS_FOLDER_ID = "external_primary:Music/Albums"
        const val ANDROID_BASE_FOLDER_ID = "external_primary:Android"
        const val GAME_FOLDER_ID = "external_primary:Android/data/game/audio"
    }
}

private object NoOpDataMediator : DataMediator {
    override suspend fun syncLibrary() = Unit
}

private class FakeLibraryPreferencesRepository : LibraryPreferencesRepository {
    private val hiddenFolderIds = MutableStateFlow(emptySet<String>())
    private val artistViewMode = MutableStateFlow(ArtistViewMode.List)
    private val playlistViewMode = MutableStateFlow(PlaylistViewMode.List)
    private val librarySortDirection = MutableStateFlow(SortDirection.Ascending)
    private val playlistSortDirection = MutableStateFlow(SortDirection.Ascending)

    override fun observeHiddenFolderIds() = hiddenFolderIds
    override fun observeArtistViewMode() = artistViewMode
    override fun observePlaylistViewMode() = playlistViewMode
    override fun observeLibrarySortDirection() = librarySortDirection
    override fun observePlaylistSortDirection() = playlistSortDirection

    override suspend fun setFolderVisible(folderId: String, visible: Boolean) {
        hiddenFolderIds.update { current ->
            if (visible) current - folderId else current + folderId
        }
    }

    override suspend fun setArtistViewMode(viewMode: ArtistViewMode) {
        artistViewMode.value = viewMode
    }

    override suspend fun setPlaylistViewMode(viewMode: PlaylistViewMode) {
        playlistViewMode.value = viewMode
    }

    override suspend fun setLibrarySortDirection(direction: SortDirection) {
        librarySortDirection.value = direction
    }

    override suspend fun setPlaylistSortDirection(direction: SortDirection) {
        playlistSortDirection.value = direction
    }
}
