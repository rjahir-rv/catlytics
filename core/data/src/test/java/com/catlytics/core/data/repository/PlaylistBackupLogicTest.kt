package com.catlytics.core.data.repository

import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.catlytics.core.data.local.InMemoryLocalDataSource
import com.catlytics.core.data.model.TrackEntity
import com.catlytics.core.model.LIKED_PLAYLIST_ID
import com.catlytics.core.model.StatisticsImportMode
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class PlaylistBackupLogicTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var context: Context
    private lateinit var localDataSource: InMemoryLocalDataSource
    private lateinit var playlistRepository: DataStorePlaylistRepository
    private lateinit var backupRepository: DefaultPlaylistBackupRepository
    private val temporaryFiles = mutableListOf<File>()

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        localDataSource = InMemoryLocalDataSource()
        val dataStoreFile = temporaryFolder.newFile("playlists_test.preferences_pb")
        playlistRepository = DataStorePlaylistRepository(
            dataStore = PreferenceDataStoreFactory.create(
                produceFile = { dataStoreFile },
            ),
            context = context,
        )
        backupRepository = DefaultPlaylistBackupRepository(
            context = context,
            playlistRepository = playlistRepository,
            localDataSource = localDataSource,
        )
    }

    @After
    fun tearDown() {
        temporaryFiles.forEach(File::delete)
    }

    @Test
    fun `export and replace import round-trip restores playlists and liked tracks`() = runTest {
        val tracks = listOf(
            track(id = "ms-1", title = "Bohemian Rhapsody", artist = "Queen"),
            track(id = "ms-2", title = "Don't Stop Me Now", artist = "Queen"),
        )
        localDataSource.replaceTracks(tracks)

        val rockPlaylist = playlistRepository.createPlaylist("Rock Classics")
        playlistRepository.addTracks(rockPlaylist.id, listOf("ms-1", "ms-2"))
        playlistRepository.addTracks(LIKED_PLAYLIST_ID, listOf("ms-1"))

        val backupUri = newBackupUri()
        val exportResult = backupRepository.exportToUri(backupUri, "0.0.5").getOrThrow()
        assertEquals(1, exportResult.playlistCount)
        assertEquals(2, exportResult.trackCount)

        val preview = backupRepository.previewFromUri(backupUri).getOrThrow()
        assertEquals(1, preview.playlistCount)
        assertEquals(3, preview.totalTracksInBackup) // 2 in rock + 1 in liked
        assertEquals(3, preview.matchedTracksCount)
        assertEquals(0, preview.missingTracksCount)
        assertEquals(1, preview.likedTracksCount)

        // Clear existing playlists
        playlistRepository.deletePlaylist(rockPlaylist.id)
        playlistRepository.removeTrack(LIKED_PLAYLIST_ID, "ms-1")

        val importResult = backupRepository.importFromUri(backupUri, StatisticsImportMode.Replace).getOrThrow()
        assertEquals(1, importResult.importedPlaylistsCount)
        assertEquals(3, importResult.matchedTracksCount)
        assertEquals(0, importResult.missingTracksCount)

        val restored = playlistRepository.observePlaylists().first()
        val restoredRock = restored.first { it.name == "Rock Classics" }
        assertEquals(listOf("ms-1", "ms-2"), restoredRock.trackIds)

        val restoredLiked = restored.first { it.id == LIKED_PLAYLIST_ID }
        assertEquals(listOf("ms-1"), restoredLiked.trackIds)
    }

    @Test
    fun `reconciliation matches tracks even if MediaStore IDs changed`() = runTest {
        // Original tracks on device A
        localDataSource.replaceTracks(
            listOf(
                track(id = "old-1", title = "Billie Jean", artist = "Michael Jackson", duration = 294000L),
                track(id = "old-2", title = "Beat It", artist = "Michael Jackson", duration = 258000L),
            ),
        )

        val popPlaylist = playlistRepository.createPlaylist("Pop")
        playlistRepository.addTracks(popPlaylist.id, listOf("old-1", "old-2"))

        val backupUri = newBackupUri()
        backupRepository.exportToUri(backupUri, "0.0.5").getOrThrow()

        // After reinstalling on device B, Android assigns brand new IDs:
        localDataSource.replaceTracks(
            listOf(
                track(id = "new-991", title = "Billie Jean", artist = "Michael Jackson", duration = 294000L),
                track(id = "new-992", title = "Beat It", artist = "Michael Jackson", duration = 258000L),
            ),
        )

        val preview = backupRepository.previewFromUri(backupUri).getOrThrow()
        assertEquals(2, preview.matchedTracksCount)
        assertEquals(0, preview.missingTracksCount)

        val importResult = backupRepository.importFromUri(backupUri, StatisticsImportMode.Replace).getOrThrow()
        assertEquals(2, importResult.matchedTracksCount)
        assertEquals(0, importResult.missingTracksCount)

        val restored = playlistRepository.observePlaylists().first().first { it.name == "Pop" }
        assertEquals(listOf("new-991", "new-992"), restored.trackIds)
    }

    @Test
    fun `reconciliation with missing tracks omits them and reports missing names (Option B)`() = runTest {
        localDataSource.replaceTracks(
            listOf(
                track(id = "ms-1", title = "Existing Song", artist = "Artist A"),
                track(id = "ms-2", title = "Missing Song", artist = "Artist B"),
            ),
        )

        val playlist = playlistRepository.createPlaylist("Mixed")
        playlistRepository.addTracks(playlist.id, listOf("ms-1", "ms-2"))

        val backupUri = newBackupUri()
        backupRepository.exportToUri(backupUri, "0.0.5").getOrThrow()

        // User reinstalls but "Missing Song" has not been downloaded to the device yet:
        localDataSource.replaceTracks(
            listOf(
                track(id = "ms-1", title = "Existing Song", artist = "Artist A"),
            ),
        )

        val preview = backupRepository.previewFromUri(backupUri).getOrThrow()
        assertEquals(1, preview.matchedTracksCount)
        assertEquals(1, preview.missingTracksCount)
        assertTrue(preview.missingTrackNames.any { it.contains("Missing Song") })

        val importResult = backupRepository.importFromUri(backupUri, StatisticsImportMode.Replace).getOrThrow()
        assertEquals(1, importResult.matchedTracksCount)
        assertEquals(1, importResult.missingTracksCount)
        assertTrue(importResult.missingTrackNames.any { it.contains("Missing Song") })

        val restored = playlistRepository.observePlaylists().first().first { it.name == "Mixed" }
        // Option B: Only existing track is included in the playlist
        assertEquals(listOf("ms-1"), restored.trackIds)
    }

    @Test
    fun `merge mode merges playlists without duplicate tracks`() = runTest {
        localDataSource.replaceTracks(
            listOf(
                track(id = "t-1", title = "Track 1", artist = "Artist"),
                track(id = "t-2", title = "Track 2", artist = "Artist"),
                track(id = "t-3", title = "Track 3", artist = "Artist"),
            ),
        )

        // Local state before backup: playlist with Track 1 and Track 2
        val pl = playlistRepository.createPlaylist("Party")
        playlistRepository.addTracks(pl.id, listOf("t-1", "t-2"))

        val backupUri = newBackupUri()
        backupRepository.exportToUri(backupUri, "0.0.5").getOrThrow()

        // On current device, user has "Party" with Track 2 and Track 3
        playlistRepository.removeTrack(pl.id, "t-1")
        playlistRepository.addTracks(pl.id, listOf("t-3"))

        // Import in Merge mode:
        backupRepository.importFromUri(backupUri, StatisticsImportMode.Merge).getOrThrow()

        val restored = playlistRepository.observePlaylists().first().first { it.name == "Party" }
        // Should contain Track 2, Track 3, and Track 1 without duplicates
        assertEquals(setOf("t-1", "t-2", "t-3"), restored.trackIds.toSet())
    }

    @Test
    fun `export playlist to M3U8 generates valid format`() = runTest {
        localDataSource.replaceTracks(
            listOf(
                track(id = "m-1", title = "Song A", artist = "Band", duration = 185000L),
            ),
        )
        val pl = playlistRepository.createPlaylist("M3UTest")
        playlistRepository.addTracks(pl.id, listOf("m-1"))

        val m3uFile = File.createTempFile("playlist-", ".m3u8", context.cacheDir).also(temporaryFiles::add)
        val m3uUri = Uri.fromFile(m3uFile).toString()

        backupRepository.exportPlaylistToM3uUri(pl.id, m3uUri).getOrThrow()

        val text = m3uFile.readText()
        assertTrue(text.startsWith("#EXTM3U"))
        assertTrue(text.contains("#PLAYLIST:M3UTest"))
        assertTrue(text.contains("#EXTINF:185,Band - Song A"))
    }

    private fun newBackupUri(): String = Uri.fromFile(newBackupFile()).toString()

    private fun newBackupFile(): File = File.createTempFile(
        "catlytics-playlists-",
        ".json",
        context.cacheDir,
    ).also(temporaryFiles::add)

    private fun track(
        id: String,
        title: String,
        artist: String,
        duration: Long = 180_000L,
    ) = TrackEntity(
        id = id,
        title = title,
        artistId = "artist-$artist",
        artistName = artist,
        durationMillis = duration,
        mediaUri = "content://media/external/audio/media/${id.filter(Char::isDigit).ifEmpty { "1" }}",
        folderPath = "/storage/emulated/0/Music",
    )
}
