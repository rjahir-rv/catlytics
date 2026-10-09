package com.catlytics.core.data.repository

import com.catlytics.core.data.local.InMemoryLocalDataSource
import com.catlytics.core.data.mediator.DataMediator
import com.catlytics.core.data.model.TrackEntity
import com.catlytics.core.domain.repository.LibraryPreferencesRepository
import com.catlytics.core.domain.repository.ArtistIdentityRepository
import com.catlytics.core.domain.repository.TrackMetadataRepository
import com.catlytics.core.model.Track
import com.catlytics.core.model.TrackMetadataEdit
import com.catlytics.core.model.TrackMetadataOverride
import com.catlytics.core.model.Artist
import com.catlytics.core.model.ArtistAlias
import com.catlytics.core.model.ArtistViewMode
import com.catlytics.core.model.PlaylistViewMode
import com.catlytics.core.model.SortDirection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class OfflineFirstLibraryRepositoryTest {
    private val localDataSource = InMemoryLocalDataSource()
    private val preferencesRepository = FakeLibraryPreferencesRepository()
    private val artistIdentityRepository = FakeArtistIdentityRepository()
    private val trackMetadataRepository = FakeTrackMetadataRepository()
    private val repository = OfflineFirstLibraryRepository(
        localDataSource = localDataSource,
        mediator = NoOpDataMediator,
        preferencesRepository = preferencesRepository,
        artistIdentityRepository = artistIdentityRepository,
        trackMetadataRepository = trackMetadataRepository,
    )

    @Test
    fun `refresh returns only tracks that were not present before the scan`() = runTest {
        val existingTrack = track("existing")
        val newTrack = track("new")
        localDataSource.replaceTracks(listOf(existingTrack))
        val refreshingRepository = OfflineFirstLibraryRepository(
            localDataSource = localDataSource,
            mediator = object : DataMediator {
                override suspend fun syncLibrary() {
                    localDataSource.replaceTracks(listOf(existingTrack, newTrack))
                }
            },
            preferencesRepository = preferencesRepository,
            artistIdentityRepository = artistIdentityRepository,
            trackMetadataRepository = trackMetadataRepository,
        )

        assertEquals(1, refreshingRepository.refreshTracks())
        assertEquals(0, refreshingRepository.refreshTracks())
    }

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
    fun `artist merge is reflected across summaries content and tracks`() = runTest {
        val main = Artist("artist-main", "Artista")
        val collaboration = Artist("artist-collab", "Artista feat. Invitado")
        localDataSource.replaceTracks(
            listOf(
                track("main", artistId = main.id, artistName = main.name),
                track("collab", artistId = collaboration.id, artistName = collaboration.name),
            ),
        )

        artistIdentityRepository.merge(collaboration, main)

        val summary = repository.observeArtists().first().single()
        val content = requireNotNull(repository.observeArtistContent(collaboration.id).first())
        assertEquals(main, summary.artist)
        assertEquals(2, summary.trackCount)
        assertEquals(setOf("main", "collab"), content.tracks.map { it.id }.toSet())
        assertEquals(setOf(main), repository.observeTracks().first().map { it.artist }.toSet())
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

    @Test
    fun `edited album joins an existing album of the same artist`() = runTest {
        localDataSource.replaceTracks(
            listOf(
                track("tagged", albumId = "album-1", albumTitle = "Debut", artistName = "Nova"),
                track("untagged", albumId = "album-unknown", albumTitle = "Álbum desconocido"),
            ),
        )
        trackMetadataRepository.overrides.value = listOf(
            override("untagged", artistName = "nova", albumTitle = "debut"),
        )

        val albums = repository.observeAlbums().first()
        val debut = albums.single { it.id == "album-1" }
        assertEquals(2, debut.trackCount)
        assertEquals(listOf("album-1"), albums.map { it.id })
        val artists = repository.observeArtists().first()
        assertEquals(listOf("artist-tagged"), artists.map { it.artist.id })
    }

    @Test
    fun `edited album without a match gets its own album`() = runTest {
        localDataSource.replaceTracks(
            listOf(track("solo", albumId = "album-unknown", albumTitle = "Álbum desconocido")),
        )
        trackMetadataRepository.overrides.value = listOf(
            override("solo", title = "Nueva", artistName = "Nadie", albumTitle = "Demos"),
        )

        val track = repository.observeTracks().first().single()
        assertEquals("Nueva", track.title)
        assertEquals("Nadie", track.artist.name)
        assertEquals("Demos", track.albumTitle)
        assertEquals("Demos", repository.observeAlbums().first().single().title)
    }

    @Test
    fun `override matches by file key when the MediaStore id changed`() = runTest {
        localDataSource.replaceTracks(listOf(track("new-id").copy(fileKey = "external:Music/a.mp3")))
        trackMetadataRepository.overrides.value = listOf(
            override("old-id", title = "Editada").copy(fileKey = "external:Music/a.mp3"),
        )

        assertEquals("Editada", repository.observeAllTracks().first().single().title)
    }

    @Test
    fun `artist aliases apply on top of edited artists`() = runTest {
        localDataSource.replaceTracks(
            listOf(track("a", artistName = "Bad Bunny"), track("b", artistName = "Desconocido")),
        )
        trackMetadataRepository.overrides.value = listOf(override("b", artistName = "Benito"))
        artistIdentityRepository.merge(Artist("x", "Benito"), Artist("artist-a", "Bad Bunny"))

        assertEquals(
            listOf("Bad Bunny"),
            repository.observeArtists().first().map { it.artist.name },
        )
    }

    @Test
    fun `edited artwork wins over MediaStore album artwork`() = runTest {
        localDataSource.replaceTracks(
            listOf(
                track("one", albumId = "album", albumTitle = "A", artworkUri = "content://art/1"),
                track("two", albumId = "album", albumTitle = "A", artworkUri = "content://art/1"),
            ),
        )
        trackMetadataRepository.overrides.value = listOf(
            override("two", artworkUri = "/data/track_artwork/two.cover"),
        )

        assertEquals(
            "/data/track_artwork/two.cover",
            repository.observeAlbums().first().single().artworkUri,
        )
    }

    private fun override(
        trackId: String,
        title: String? = null,
        artistName: String? = null,
        albumTitle: String? = null,
        artworkUri: String? = null,
    ) = TrackMetadataOverride(
        trackId = trackId,
        fileKey = null,
        title = title,
        artistName = artistName,
        albumTitle = albumTitle,
        artworkUri = artworkUri,
        originalTitle = "Track $trackId",
        originalArtistName = "Artist $trackId",
        originalAlbumTitle = null,
        durationMillis = 180_000L,
        updatedAtMillis = 1L,
    )

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

private class FakeArtistIdentityRepository : ArtistIdentityRepository {
    private val aliases = MutableStateFlow(emptyList<ArtistAlias>())

    override fun observeAliases() = aliases
    override suspend fun getAliases() = aliases.value
    override suspend fun merge(source: Artist, target: Artist) {
        aliases.value += ArtistAlias(source, target)
    }
    override suspend fun unmerge(source: Artist) {
        aliases.value = aliases.value.filterNot { it.source == source }
    }
    override suspend fun replaceAliases(aliases: List<ArtistAlias>) {
        this.aliases.value = aliases
    }
    override suspend fun mergeAliases(aliases: List<ArtistAlias>): Int {
        val existing = this.aliases.value.map { it.source.name }.toSet()
        val newAliases = aliases.filterNot { it.source.name in existing }
        this.aliases.value += newAliases
        return newAliases.size
    }
}

private class FakeTrackMetadataRepository : TrackMetadataRepository {
    val overrides = MutableStateFlow(emptyList<TrackMetadataOverride>())

    override fun observeOverrides() = overrides
    override fun observeOriginalTrack(trackId: String) = flowOf<Track?>(null)
    override suspend fun saveEdit(trackId: String, edit: TrackMetadataEdit) = Unit
    override suspend fun reset(trackId: String) = Unit
    override suspend fun writeTagsToFile(trackId: String) = Unit
}

private class FakeLibraryPreferencesRepository : LibraryPreferencesRepository {
    private val hiddenFolderIds = MutableStateFlow(emptySet<String>())
    private val artistViewMode = MutableStateFlow(ArtistViewMode.List)
    private val playlistViewMode = MutableStateFlow(PlaylistViewMode.List)
    private val librarySortDirection = MutableStateFlow(SortDirection.Ascending)
    private val playlistSortDirection = MutableStateFlow(SortDirection.Ascending)
    private val musicScanSettings = MutableStateFlow(com.catlytics.core.model.MusicScanSettings())

    override fun observeHiddenFolderIds() = hiddenFolderIds
    override fun observeMusicScanSettings() = musicScanSettings
    override fun observeArtistViewMode() = artistViewMode
    override fun observePlaylistViewMode() = playlistViewMode
    override fun observeLibrarySortDirection() = librarySortDirection
    override fun observePlaylistSortDirection() = playlistSortDirection

    override suspend fun setFolderVisible(folderId: String, visible: Boolean) {
        hiddenFolderIds.update { current ->
            if (visible) current - folderId else current + folderId
        }
    }

    override suspend fun setMusicScanDurationFilter(
        filter: com.catlytics.core.model.MusicScanDurationFilter,
    ) {
        musicScanSettings.value = musicScanSettings.value.copy(durationFilter = filter)
    }

    override suspend fun setMusicScanSizeFilter(
        filter: com.catlytics.core.model.MusicScanSizeFilter,
    ) {
        musicScanSettings.value = musicScanSettings.value.copy(sizeFilter = filter)
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
