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
import com.catlytics.core.model.BulkTrackMetadataEdit
import com.catlytics.core.model.FieldEdit
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
        eventRepository = RoomPlaybackEventRepository(
            database.playbackEventDao(),
            database.artistArtworkOverrideDao(),
            database.artistAliasDao(),
        )
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

    @Test
    fun `bulk edit only touches the fields it sets`() = runTest {
        localDataSource.replaceTracks(listOf(track("one"), track("two")))
        repository.saveEdit("one", edit("one", title = "Propio"))

        val result = repository.saveBulkEdit(
            listOf("one", "two", "missing"),
            BulkTrackMetadataEdit(albumTitle = FieldEdit.Set("Recopilatorio")),
        )

        assertEquals(2, result.updated)
        assertEquals(1, result.skipped)
        val overrides = repository.observeOverrides().first().associateBy { it.trackId }
        assertEquals("Propio", overrides.getValue("one").title)
        assertEquals("Recopilatorio", overrides.getValue("one").albumTitle)
        assertNull(overrides.getValue("two").title)
        assertEquals("Recopilatorio", overrides.getValue("two").albumTitle)
    }

    @Test
    fun `bulk restore drops the field and empty overrides`() = runTest {
        localDataSource.replaceTracks(listOf(track("one"), track("two")))
        repository.saveEdit("one", edit("one", title = "Propio", artist = "Nova"))
        repository.saveEdit("two", edit("two", artist = "Nova"))

        repository.saveBulkEdit(
            listOf("one", "two"),
            BulkTrackMetadataEdit(artistName = FieldEdit.Restore),
        )

        val override = repository.observeOverrides().first().single()
        assertEquals("one", override.trackId)
        assertEquals("Propio", override.title)
        assertNull(override.artistName)
    }

    @Test
    fun `bulk artwork is stored once and kept while any track uses it`() = runTest {
        localDataSource.replaceTracks(listOf(track("one"), track("two")))
        val source = File(context.cacheDir, "shared.jpg").apply { writeBytes(byteArrayOf(4, 5)) }

        repository.saveBulkEdit(
            listOf("one", "two"),
            BulkTrackMetadataEdit(artwork = ArtworkEdit.Replace(source.toURI().toString())),
        )

        val paths = repository.observeOverrides().first().map { it.artworkUri!! }.toSet()
        val shared = paths.single()
        assertTrue(File(shared).exists())

        repository.reset("one")
        assertTrue(File(shared).exists())

        repository.resetMany(listOf("two"))
        assertFalse(File(shared).exists())
        assertTrue(repository.observeOverrides().first().isEmpty())
    }

    @Test
    fun `writing several files reports the failures`() = runTest {
        localDataSource.replaceTracks(listOf(track("one")))

        val failures = repository.writeTagsToFiles(listOf("one", "missing"))

        assertEquals(1, failures)
        assertEquals("content://media/one", tagWriter.writes.single().first)
    }

    @Test
    fun `editing only the artist of a whole album shows the new artist in the library`() = runTest {
        localDataSource.replaceTracks(
            listOf(
                track("one", artist = "Nova", album = "Debut", albumId = "album-debut"),
                track("two", artist = "Nova", album = "Debut", albumId = "album-debut"),
            ),
        )
        val library = OfflineFirstLibraryRepository(
            localDataSource = localDataSource,
            mediator = NoOpDataMediator,
            preferencesRepository = FakeLibraryPreferencesRepository(),
            artistIdentityRepository = RoomArtistIdentityRepository(database, database.artistAliasDao()),
            trackMetadataRepository = repository,
            artistArtworkRepository = FakeArtistArtworkRepository(),
        )

        repository.saveBulkEdit(
            listOf("one", "two"),
            BulkTrackMetadataEdit(artistName = FieldEdit.Set("Luz")),
        )

        val content = library.observeAlbumContent("album-debut").first()!!
        assertEquals("Luz", content.album.artist.name)
        assertEquals(listOf("Luz", "Luz"), content.tracks.map { it.artist.name })
    }

    @Test
    fun `album with a shared cover can then change only its artist`() = runTest {
        localDataSource.replaceTracks(listOf(track("one"), track("two")))
        val source = File(context.cacheDir, "cover.jpg").apply { writeBytes(byteArrayOf(7)) }
        repository.saveBulkEdit(
            listOf("one", "two"),
            BulkTrackMetadataEdit(artwork = ArtworkEdit.Replace(source.toURI().toString())),
        )

        val result = repository.saveBulkEdit(
            listOf("one", "two"),
            BulkTrackMetadataEdit(artistName = FieldEdit.Set("Luz")),
        )

        assertEquals(2, result.updated)
        val overrides = repository.observeOverrides().first()
        assertEquals(listOf("Luz", "Luz"), overrides.map { it.artistName })
        assertEquals(1, overrides.map { it.artworkUri }.distinct().size)
    }

    @Test
    fun `bulk edit only counts and touches tracks that change`() = runTest {
        localDataSource.replaceTracks(listOf(track("one"), track("two")))
        repository.saveEdit("one", edit("one", artist = "Luz"))
        val stored = repository.observeOverrides().first().single()

        val unchanged = repository.saveBulkEdit(
            listOf("one"),
            BulkTrackMetadataEdit(artistName = FieldEdit.Set("Luz")),
        )
        val changed = repository.saveBulkEdit(
            listOf("one", "two"),
            BulkTrackMetadataEdit(artistName = FieldEdit.Set("Luz")),
        )

        assertEquals(0, unchanged.updated)
        assertEquals(1, changed.updated)
        assertEquals(stored, repository.observeOverrides().first().single { it.trackId == "one" })
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
