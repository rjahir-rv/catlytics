package com.catlytics.core.data.repository

import android.content.Context
import androidx.room.Room
import com.catlytics.core.data.local.room.CatlyticsDatabase
import com.catlytics.core.model.Artist
import com.catlytics.core.model.PlaybackEvent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File

@RunWith(RobolectricTestRunner::class)
class RoomArtistArtworkRepositoryTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var context: Context
    private lateinit var database: CatlyticsDatabase
    private lateinit var repository: RoomArtistArtworkRepository
    private lateinit var artistIdentityRepository: RoomArtistIdentityRepository
    private var now = 1L

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        database = Room.inMemoryDatabaseBuilder(context, CatlyticsDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = RoomArtistArtworkRepository(
            dao = database.artistArtworkOverrideDao(),
            aliasDao = database.artistAliasDao(),
            artworkStore = com.catlytics.core.data.local.ManagedImageStore(
                context,
                RoomArtistArtworkRepository.ARTWORK_DIRECTORY,
            ),
            clock = { now },
        )
        artistIdentityRepository = RoomArtistIdentityRepository(database, database.artistAliasDao())
    }

    @After
    fun tearDown() {
        database.close()
        context.filesDir.resolve(RoomArtistArtworkRepository.ARTWORK_DIRECTORY).deleteRecursively()
    }

    @Test
    fun `changing the image keys it by artist name and deletes the previous copy`() = runTest {
        repository.setArtwork(Artist("a1", "Nova "), image("first").toURI().toString())
        val first = repository.observeOverrides().first().single()
        now = 2L
        repository.setArtwork(Artist("other-id", "NOVA"), image("second").toURI().toString())

        val second = repository.observeOverrides().first().single()
        assertEquals("nova", second.artistKey)
        assertEquals(2L, second.updatedAtMillis)
        assertEquals("second", File(second.artworkUri).readText())
        assertFalse(File(first.artworkUri).exists())
    }

    @Test
    fun `restoring removes the images of the artist and of the artists merged into it`() = runTest {
        repository.setArtwork(Artist("main", "Main"), image("main").toURI().toString())
        repository.setArtwork(Artist("feat", "Main feat. Guest"), image("feat").toURI().toString())
        repository.setArtwork(Artist("luz", "Luz"), image("luz").toURI().toString())
        artistIdentityRepository.merge(Artist("feat", "Main feat. Guest"), Artist("main", "Main"))
        val files = repository.observeOverrides().first().associate { it.artistKey to File(it.artworkUri) }

        repository.restoreArtwork(Artist("main", "Main"))

        assertEquals(listOf("luz"), repository.observeOverrides().first().map { it.artistKey })
        assertFalse(files.getValue("main").exists())
        assertFalse(files.getValue("main feat. guest").exists())
        assertTrue(files.getValue("luz").exists())
    }

    @Test
    fun `top artists show the chosen image`() = runTest {
        val events = RoomPlaybackEventRepository(
            database.playbackEventDao(),
            database.artistArtworkOverrideDao(),
            database.artistAliasDao(),
        )
        events.recordEvent(event(artistId = "nova", artistName = "Nova"))
        events.recordEvent(event(artistId = "luz", artistName = "Luz"))
        repository.setArtwork(Artist("nova", "Nova"), image("nova").toURI().toString())

        val artwork = events.observeTopArtists(0L, Long.MAX_VALUE, 10).first().associate { it.name to it.artworkUri }

        assertEquals(repository.observeOverrides().first().single().artworkUri, artwork["Nova"])
        assertEquals("content://cover/luz", artwork["Luz"])
    }

    private fun image(content: String): File =
        temporaryFolder.newFile("$content.jpg").apply { writeText(content) }

    private fun event(artistId: String, artistName: String) = PlaybackEvent(
        trackId = "track-$artistId",
        trackTitle = "Song",
        artistId = artistId,
        artistName = artistName,
        albumId = null,
        albumTitle = null,
        artworkUri = "content://cover/$artistId",
        durationListenedMillis = 60_000L,
        trackDurationMillis = 180_000L,
        timestamp = 1_000L,
    )
}
