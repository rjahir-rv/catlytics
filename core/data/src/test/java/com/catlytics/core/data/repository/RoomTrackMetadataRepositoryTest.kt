package com.catlytics.core.data.repository

import android.content.Context
import androidx.room.Room
import com.catlytics.core.data.local.AudioTagWriter
import com.catlytics.core.data.local.AudioTags
import com.catlytics.core.data.local.InMemoryLocalDataSource
import com.catlytics.core.data.local.ManagedImageStore
import com.catlytics.core.data.local.room.CatlyticsDatabase
import com.catlytics.core.data.model.TrackEntity
import com.catlytics.core.model.ArtworkEdit
import com.catlytics.core.model.PlaybackEvent
import com.catlytics.core.model.TrackMetadataEdit
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class RoomTrackMetadataRepositoryTest {
    private lateinit var context: Context
    private lateinit var database: CatlyticsDatabase
    private lateinit var localDataSource: InMemoryLocalDataSource
    private lateinit var tagWriter: RecordingTagWriter
    private lateinit var repository: RoomTrackMetadataRepository
    private lateinit var eventRepository: RoomPlaybackEventRepository

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        database = Room.inMemoryDatabaseBuilder(context, CatlyticsDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        localDataSource = InMemoryLocalDataSource()
        tagWriter = RecordingTagWriter()
        repository = RoomTrackMetadataRepository(
            dao = database.trackMetadataOverrideDao(),
            localDataSource = localDataSource,
            tagWriter = tagWriter,
            artworkStore = ManagedImageStore(context, "track_artwork"),
            clock = { 42L },
        )
        eventRepository = RoomPlaybackEventRepository(database.playbackEventDao())
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun `only values that differ from the scanned ones are overridden`() = runTest {
        localDataSource.replaceTracks(listOf(track("one")))

        repository.saveEdit("one", edit("one", title = " Nuevo título "))

        val override = repository.observeOverrides().first().single()
        assertEquals("Nuevo título", override.title)
        assertNull(override.artistName)
        assertNull(override.albumTitle)
        assertEquals("Título one", override.originalTitle)
        assertEquals("external:Music/one.mp3", override.fileKey)
        assertEquals(42L, override.updatedAtMillis)
    }

    @Test
    fun `saving the scanned values removes the override`() = runTest {
        localDataSource.replaceTracks(listOf(track("one")))
        repository.saveEdit("one", edit("one", title = "Otro"))

        repository.saveEdit("one", edit("one"))

        assertTrue(repository.observeOverrides().first().isEmpty())
    }

    @Test
    fun `statistics regroup past plays under the edited artist and album`() = runTest {
        localDataSource.replaceTracks(
            listOf(
                track("tagged", artist = "Nova", album = "Debut", albumId = "album-debut"),
                track("untagged", artist = "Artista desconocido", album = "Álbum desconocido"),
            ),
        )
        eventRepository.recordEvent(event(localDataSource.track("tagged"), 1_000L))
        eventRepository.recordEvent(event(localDataSource.track("untagged"), 2_000L))

        repository.saveEdit(
            "untagged",
            edit("untagged", title = "Intro", artist = "Nova", album = "Debut"),
        )

        val topArtists = eventRepository.observeTopArtists(0L, 10_000L, 10).first()
        assertEquals(listOf("Nova"), topArtists.map { it.name })
        assertEquals("artist-tagged", topArtists.single().artistId)
        val topAlbums = eventRepository.observeTopAlbums(0L, 10_000L, 10).first()
        assertEquals(listOf("album-debut"), topAlbums.map { it.albumId })
        assertEquals(2, topAlbums.single().playCount)
        val titles = eventRepository.observeTopTracks(0L, 10_000L, 10).first().map { it.title }
        assertTrue("Intro" in titles)
        val counts = eventRepository.observePeriodUniqueCounts(0L, 10_000L).first()
        assertEquals(1, counts.artistCount)
        assertEquals(1, counts.albumCount)
        // Stored events are never rewritten.
        assertEquals(
            setOf("Nova", "Artista desconocido"),
            eventRepository.getAllEvents().map { it.artistName }.toSet(),
        )
    }

    @Test
    fun `replaced artwork is copied into app storage and removed on reset`() = runTest {
        localDataSource.replaceTracks(listOf(track("one")))
        val source = File(context.cacheDir, "picked.jpg").apply { writeBytes(byteArrayOf(1, 2, 3)) }

        repository.saveEdit(
            "one",
            edit("one", artwork = ArtworkEdit.Replace(source.toURI().toString())),
        )

        val artworkPath = repository.observeOverrides().first().single().artworkUri!!
        assertTrue(artworkPath.contains("track_artwork"))
        assertTrue(File(artworkPath).exists())

        repository.reset("one")

        assertFalse(File(artworkPath).exists())
        assertTrue(repository.observeOverrides().first().isEmpty())
    }

    @Test
    fun `reconcile moves overrides to the new MediaStore id of the same file`() = runTest {
        localDataSource.replaceTracks(listOf(track("old")))
        repository.saveEdit("old", edit("old", title = "Editada"))

        val rescanned = listOf(track("new").copy(fileKey = track("old").fileKey))
        repository.reconcile(rescanned)

        assertEquals("new", repository.observeOverrides().first().single().trackId)
    }

    @Test
    fun `writing tags uses the edited values`() = runTest {
        localDataSource.replaceTracks(listOf(track("one")))
        repository.saveEdit("one", edit("one", artist = "Nova"))

        repository.writeTagsToFile("one")

        val (uri, tags) = tagWriter.writes.single()
        assertEquals("content://media/one", uri)
        assertEquals("Nova", tags.artist)
        assertEquals("Título one", tags.title)
        assertNull(tags.frontCover)
    }

    private suspend fun InMemoryLocalDataSource.track(id: String) =
        observeTracks().first().single { it.id == id }

    private fun edit(
        id: String,
        title: String = "Título $id",
        artist: String = "Artista $id",
        album: String = "Álbum $id",
        artwork: ArtworkEdit = ArtworkEdit.Keep,
    ) = TrackMetadataEdit(title, artist, album, artwork)

    private fun track(
        id: String,
        artist: String = "Artista $id",
        album: String = "Álbum $id",
        albumId: String = "album-$id",
    ) = TrackEntity(
        id = id,
        title = "Título $id",
        artistId = "artist-$id",
        artistName = artist,
        durationMillis = 180_000L,
        mediaUri = "content://media/$id",
        albumId = albumId,
        albumTitle = album,
        fileKey = "external:Music/$id.mp3",
    )

    private fun event(track: TrackEntity, timestamp: Long) = PlaybackEvent(
        trackId = track.id,
        trackTitle = track.title,
        artistId = track.artistId,
        artistName = track.artistName,
        albumId = track.albumId,
        albumTitle = track.albumTitle,
        artworkUri = null,
        durationListenedMillis = 60_000L,
        trackDurationMillis = track.durationMillis,
        timestamp = timestamp,
    )
}

private class RecordingTagWriter : AudioTagWriter {
    val writes = mutableListOf<Pair<String, AudioTags>>()

    override suspend fun write(mediaUri: String, tags: AudioTags) {
        writes += mediaUri to tags
    }
}
