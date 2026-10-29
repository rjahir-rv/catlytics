package com.catlytics.core.data.repository

import android.content.Context
import androidx.room.Room
import com.catlytics.core.data.local.room.CatlyticsDatabase
import com.catlytics.core.model.Artist
import com.catlytics.core.model.PlaybackEvent
import com.catlytics.core.model.renamedArtistId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class RoomArtistIdentityRepositoryTest {
    private lateinit var database: CatlyticsDatabase
    private lateinit var identityRepository: RoomArtistIdentityRepository
    private lateinit var eventRepository: RoomPlaybackEventRepository

    @Before
    fun setUp() {
        val context: Context = RuntimeEnvironment.getApplication()
        database = Room.inMemoryDatabaseBuilder(context, CatlyticsDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        identityRepository = RoomArtistIdentityRepository(database, database.artistAliasDao())
        eventRepository = RoomPlaybackEventRepository(
            database.playbackEventDao(),
            database.artistArtworkOverrideDao(),
            database.artistAliasDao(),
        )
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun `merging groups keeps aliases flat and supports individual unmerge`() = runTest {
        val main = Artist("main", "Artista")
        val collaboration = Artist("collab", "Artista feat. Invitado")
        val liveCollaboration = Artist("live", "Artista & Invitado")

        identityRepository.merge(collaboration, main)
        identityRepository.merge(liveCollaboration, collaboration)

        val merged = identityRepository.getAliases()
        assertEquals(setOf(collaboration, liveCollaboration), merged.map { it.source }.toSet())
        assertEquals(setOf(main), merged.map { it.target }.toSet())

        identityRepository.unmerge(collaboration)

        assertEquals(listOf(liveCollaboration), identityRepository.getAliases().map { it.source })
    }

    @Test
    fun `renaming moves the artist and its merged group to the new name`() = runTest {
        val main = Artist("main", "Artista")
        val collaboration = Artist("collab", "Artista feat. Invitado")
        identityRepository.merge(collaboration, main)

        identityRepository.rename(main, "Nuevo Nombre")

        val targets = identityRepository.getAliases().associate { it.source.name to it.target }
        assertEquals(Artist(renamedArtistId("Nuevo Nombre"), "Nuevo Nombre"), targets.getValue("Artista"))
        assertEquals(targets.getValue("Artista"), targets.getValue("Artista feat. Invitado"))

        // Renaming the renamed artist again keeps a single hop and no alias for the old new name.
        identityRepository.rename(targets.getValue("Artista"), "Otro Nombre")
        val renamedAgain = identityRepository.getAliases()
        assertEquals(setOf("Artista", "Artista feat. Invitado"), renamedAgain.map { it.source.name }.toSet())
        assertEquals(setOf("Otro Nombre"), renamedAgain.map { it.target.name }.toSet())
    }

    @Test
    fun `a new spelling of the same name is a display rename that can be undone`() = runTest {
        val main = Artist("main", "the beatles")
        val collaboration = Artist("collab", "The Beatles & Friends")
        identityRepository.merge(collaboration, main)

        identityRepository.rename(main, "The Beatles")

        val aliases = identityRepository.getAliases().associate { it.source.name to it.target.name }
        assertEquals(mapOf("the beatles" to "The Beatles", "The Beatles & Friends" to "The Beatles"), aliases)

        identityRepository.rename(Artist("main", "The Beatles"), "the beatles")
        assertEquals(listOf("The Beatles & Friends"), identityRepository.getAliases().map { it.source.name })
    }

    @Test
    fun `statistics use the renamed artist`() = runTest {
        val main = Artist("main", "Artista")
        eventRepository.recordEvent(event(main, "track-main", 1_000L))

        identityRepository.rename(main, "ARTISTA")

        assertEquals("ARTISTA", eventRepository.observeTopArtists(0L, 10_000L, 10).first().single().name)

        identityRepository.rename(Artist("main", "ARTISTA"), "Nuevo")
        val top = eventRepository.observeTopArtists(0L, 10_000L, 10).first().single()
        assertEquals("Nuevo", top.name)
        assertEquals(1, top.playCount)
    }

    @Test
    fun `display renames survive a backup restore`() = runTest {
        val main = Artist("main", "artista")
        identityRepository.rename(main, "Artista")
        val exported = identityRepository.getAliases()

        identityRepository.replaceAliases(exported)

        assertEquals(exported, identityRepository.getAliases())
    }

    @Test
    fun `statistics regroup historical artists without rewriting events`() = runTest {
        val main = Artist("main", "Artista")
        val collaboration = Artist("collab", "Artista feat. Invitado")
        eventRepository.recordEvent(event(main, "track-main", 1_000L))
        eventRepository.recordEvent(event(collaboration, "track-collab", 2_000L))

        identityRepository.merge(collaboration, main)

        val topArtists = eventRepository.observeTopArtists(0L, 10_000L, 10).first()
        val uniqueCounts = eventRepository.observePeriodUniqueCounts(0L, 10_000L).first()
        assertEquals(1, topArtists.size)
        assertEquals(main.id, topArtists.single().artistId)
        assertEquals(main.name, topArtists.single().name)
        assertEquals(2, topArtists.single().playCount)
        assertEquals(1, uniqueCounts.artistCount)
        assertEquals(setOf(main.id, collaboration.id), eventRepository.getAllEvents().map { it.artistId }.toSet())
    }

    private fun event(artist: Artist, trackId: String, timestamp: Long) = PlaybackEvent(
        trackId = trackId,
        trackTitle = trackId,
        artistId = artist.id,
        artistName = artist.name,
        albumId = null,
        albumTitle = null,
        artworkUri = null,
        durationListenedMillis = 1_000L,
        trackDurationMillis = 10_000L,
        timestamp = timestamp,
    )
}
