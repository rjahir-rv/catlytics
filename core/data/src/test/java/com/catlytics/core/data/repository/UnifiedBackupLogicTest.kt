package com.catlytics.core.data.repository

import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import com.catlytics.core.data.local.InMemoryLocalDataSource
import com.catlytics.core.data.local.room.CatlyticsDatabase
import com.catlytics.core.data.model.TrackEntity
import com.catlytics.core.model.Artist
import com.catlytics.core.model.BackupOptions
import com.catlytics.core.model.LIKED_PLAYLIST_ID
import com.catlytics.core.model.PlaybackEvent
import com.catlytics.core.model.StatisticsImportMode
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class UnifiedBackupLogicTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var context: Context
    private lateinit var database: CatlyticsDatabase
    private lateinit var eventRepository: RoomPlaybackEventRepository
    private lateinit var artistIdentityRepository: RoomArtistIdentityRepository
    private lateinit var localDataSource: InMemoryLocalDataSource
    private lateinit var playlistRepository: DataStorePlaylistRepository
    private lateinit var statisticsBackupRepository: DefaultStatisticsBackupRepository
    private lateinit var playlistBackupRepository: DefaultPlaylistBackupRepository
    private lateinit var unifiedBackupRepository: DefaultUnifiedBackupRepository
    private val temporaryFiles = mutableListOf<File>()
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        database = Room.inMemoryDatabaseBuilder(context, CatlyticsDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        eventRepository = RoomPlaybackEventRepository(database.playbackEventDao())
        artistIdentityRepository = RoomArtistIdentityRepository(database, database.artistAliasDao())
        localDataSource = InMemoryLocalDataSource()
        val dataStoreFile = temporaryFolder.newFile("playlists_test.preferences_pb")
        playlistRepository = DataStorePlaylistRepository(
            dataStore = PreferenceDataStoreFactory.create(produceFile = { dataStoreFile }),
            context = context,
        )
        statisticsBackupRepository = DefaultStatisticsBackupRepository(
            context = context,
            playbackEventRepository = eventRepository,
            artistIdentityRepository = artistIdentityRepository,
            database = database,
        )
        playlistBackupRepository = DefaultPlaylistBackupRepository(
            context = context,
            playlistRepository = playlistRepository,
            localDataSource = localDataSource,
        )
        unifiedBackupRepository = DefaultUnifiedBackupRepository(
            context = context,
            statisticsBackupRepository = statisticsBackupRepository,
            playlistBackupRepository = playlistBackupRepository,
            ioDispatcher = testDispatcher,
        )
    }

    @After
    fun tearDown() {
        database.close()
        temporaryFiles.forEach(File::delete)
    }

    @Test
    fun `export both and restore both round-trip restores stats and playlists`() = runTest(testDispatcher) {
        val tracks = listOf(
            track(id = "ms-1", title = "Song A", artist = "Artist A"),
            track(id = "ms-2", title = "Song B", artist = "Artist B"),
        )
        localDataSource.replaceTracks(tracks)

        val rockPlaylist = playlistRepository.createPlaylist("Rock Classics")
        playlistRepository.addTracks(rockPlaylist.id, listOf("ms-1", "ms-2"))
        playlistRepository.addTracks(LIKED_PLAYLIST_ID, listOf("ms-1"))

        eventRepository.insertEventsIfAbsent(listOf(sampleEvent("ms-1", "Song A", 1000L)))
        artistIdentityRepository.merge(Artist("a1", "The Queen"), Artist("a2", "Queen"))

        val backupUri = newBackupUri()
        val exportResult = unifiedBackupRepository.exportToUri(
            backupUri,
            BackupOptions(includeStatistics = true, includePlaylists = true),
            "1.0.0",
        ).getOrThrow()

        assertNotNull(exportResult.statistics)
        assertEquals(1, exportResult.statistics?.eventCount)
        assertEquals(1, exportResult.statistics?.artistAliasCount)
        assertNotNull(exportResult.playlists)
        assertEquals(1, exportResult.playlists?.playlistCount)
        assertEquals(2, exportResult.playlists?.trackCount)

        val preview = unifiedBackupRepository.previewFromUri(backupUri).getOrThrow()
        assertNotNull(preview.statistics)
        assertEquals(1, preview.statistics?.eventCount)
        assertNotNull(preview.playlists)
        assertEquals(1, preview.playlists?.playlistCount)
        assertEquals(3, preview.playlists?.matchedTracksCount)

        playlistRepository.deletePlaylist(rockPlaylist.id)
        playlistRepository.removeTrack(LIKED_PLAYLIST_ID, "ms-1")
        eventRepository.replaceEvents(emptyList())

        val importResult = unifiedBackupRepository.importFromUri(
            backupUri,
            BackupOptions(includeStatistics = true, includePlaylists = true),
            StatisticsImportMode.Replace,
        ).getOrThrow()

        assertNotNull(importResult.statistics)
        assertEquals(1, importResult.statistics?.importedCount)
        assertNotNull(importResult.playlists)
        assertEquals(1, importResult.playlists?.importedPlaylistsCount)
        assertEquals(3, importResult.playlists?.matchedTracksCount)

        val restoredPlaylists = playlistRepository.observePlaylists().first()
        val restoredRock = restoredPlaylists.first { it.name == "Rock Classics" }
        assertEquals(listOf("ms-1", "ms-2"), restoredRock.trackIds)
        assertEquals(1, eventRepository.getAllEvents().size)
    }

    @Test
    fun `export selective allows choosing only playlists`() = runTest(testDispatcher) {
        val tracks = listOf(track(id = "ms-1", title = "Song A", artist = "Artist A"))
        localDataSource.replaceTracks(tracks)
        val playlist = playlistRepository.createPlaylist("Favorites")
        playlistRepository.addTracks(playlist.id, listOf("ms-1"))
        eventRepository.insertEventsIfAbsent(listOf(sampleEvent("ms-1", "Song A", 1000L)))

        val backupUri = newBackupUri()
        val exportResult = unifiedBackupRepository.exportToUri(
            backupUri,
            BackupOptions(includeStatistics = false, includePlaylists = true),
            "1.0.0",
        ).getOrThrow()

        assertNull(exportResult.statistics)
        assertNotNull(exportResult.playlists)
        assertEquals(1, exportResult.playlists?.playlistCount)

        val preview = unifiedBackupRepository.previewFromUri(backupUri).getOrThrow()
        assertNull(preview.statistics)
        assertNotNull(preview.playlists)
        assertEquals(1, preview.playlists?.playlistCount)
    }

    @Test
    fun `import selective allows restoring only stats from full backup`() = runTest(testDispatcher) {
        val tracks = listOf(track(id = "ms-1", title = "Song A", artist = "Artist A"))
        localDataSource.replaceTracks(tracks)
        val playlist = playlistRepository.createPlaylist("Chill")
        playlistRepository.addTracks(playlist.id, listOf("ms-1"))
        eventRepository.insertEventsIfAbsent(listOf(sampleEvent("ms-1", "Song A", 1000L)))

        val backupUri = newBackupUri()
        unifiedBackupRepository.exportToUri(
            backupUri,
            BackupOptions(includeStatistics = true, includePlaylists = true),
            "1.0.0",
        ).getOrThrow()

        playlistRepository.deletePlaylist(playlist.id)
        eventRepository.replaceEvents(emptyList())

        val importResult = unifiedBackupRepository.importFromUri(
            backupUri,
            BackupOptions(includeStatistics = true, includePlaylists = false),
            StatisticsImportMode.Merge,
        ).getOrThrow()

        assertNotNull(importResult.statistics)
        assertEquals(1, importResult.statistics?.importedCount)
        assertNull(importResult.playlists)

        val playlists = playlistRepository.observePlaylists().first()
        assertTrue(playlists.none { it.name == "Chill" })
        assertEquals(1, eventRepository.getAllEvents().size)
    }

    @Test
    fun `Option B missing tracks in unified import are omitted and returned in warning list`() = runTest(testDispatcher) {
        val tracks = listOf(track(id = "ms-1", title = "Song A", artist = "Artist A"))
        localDataSource.replaceTracks(tracks)
        val playlist = playlistRepository.createPlaylist("Mix")
        playlistRepository.addTracks(playlist.id, listOf("ms-1"))

        val backupUri = newBackupUri()
        unifiedBackupRepository.exportToUri(
            backupUri,
            BackupOptions(includeStatistics = false, includePlaylists = true),
            "1.0.0",
        ).getOrThrow()

        localDataSource.replaceTracks(emptyList())
        playlistRepository.deletePlaylist(playlist.id)

        val preview = unifiedBackupRepository.previewFromUri(backupUri).getOrThrow()
        assertEquals(1, preview.playlists?.missingTracksCount)

        val importResult = unifiedBackupRepository.importFromUri(
            backupUri,
            BackupOptions(includeStatistics = false, includePlaylists = true),
            StatisticsImportMode.Merge,
        ).getOrThrow()

        assertEquals(1, importResult.playlists?.missingTracksCount)
        assertTrue(importResult.playlists?.missingTrackNames?.any { it.contains("Song A") } == true)

        val restored = playlistRepository.observePlaylists().first()
        val mix = restored.first { it.name == "Mix" }
        assertTrue(mix.trackIds.isEmpty())
    }

    private fun newBackupUri(): String {
        val file = temporaryFolder.newFile("unified_backup_${System.nanoTime()}.json")
        temporaryFiles += file
        return Uri.fromFile(file).toString()
    }

    private fun track(id: String, title: String, artist: String): TrackEntity =
        TrackEntity(
            id = id,
            title = title,
            artistId = "artist-$artist",
            artistName = artist,
            albumId = null,
            albumTitle = null,
            artworkUri = null,
            durationMillis = 200_000L,
            mediaUri = "content://media/external/audio/media/${id.filter(Char::isDigit).ifEmpty { "1" }}",
            folderPath = "/storage/emulated/0/Music",
            trackNumber = null,
        )

    private fun sampleEvent(trackId: String, title: String, timestamp: Long) = PlaybackEvent(
        trackId = trackId,
        trackTitle = title,
        artistId = "artist-1",
        artistName = "Artist 1",
        albumId = null,
        albumTitle = null,
        artworkUri = null,
        durationListenedMillis = 60_000L,
        trackDurationMillis = 180_000L,
        timestamp = timestamp,
    )
}
