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
        eventRepository = RoomPlaybackEventRepository(database.playbackEventDao())
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
