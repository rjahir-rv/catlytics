package com.catlytics.core.data.repository

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.catlytics.core.model.LIKED_PLAYLIST_ID
import com.catlytics.core.model.LIKED_PLAYLIST_NAME
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DataStorePlaylistRepositoryTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `playlist operations persist and ignore duplicate tracks`() = runTest {
        val file = temporaryFolder.newFile("playlists.preferences_pb")
        val repository = repository(backgroundScope, file)

        val playlist = repository.createPlaylist("Favoritas")
        assertEquals(2, repository.addTracks(playlist.id, listOf("one", "two", "one")))
        assertEquals(0, repository.addTracks(playlist.id, listOf("one", "two")))
        repository.renamePlaylist(playlist.id, "Viaje")
        repository.removeTrack(playlist.id, "one")

        val restored = repository.observePlaylists().first().single { it.id == playlist.id }
        assertEquals("Viaje", restored.name)
        assertEquals(listOf("two"), restored.trackIds)

        repository.deletePlaylist(playlist.id)
        val playlists = repository.observePlaylists().first()
        assertEquals(listOf(LIKED_PLAYLIST_ID), playlists.map { it.id })
    }

    @Test
    fun `addTracksToPlaylists updates every target playlist in one write`() = runTest {
        val file = temporaryFolder.newFile("playlists-batch.preferences_pb")
        val repository = repository(backgroundScope, file)

        val first = repository.createPlaylist("Focus")
        val second = repository.createPlaylist("Chill")

        val added = repository.addTracksToPlaylists(
            playlistIds = listOf(first.id, second.id),
            trackIds = listOf("track-1"),
        )

        assertEquals(1, added[first.id])
        assertEquals(1, added[second.id])

        val restored = repository.observePlaylists().first()
        val focus = restored.single { it.id == first.id }
        val chill = restored.single { it.id == second.id }
        assertEquals(listOf("track-1"), focus.trackIds)
        assertEquals(listOf("track-1"), chill.trackIds)
    }

    @Test
    fun `setPlaylistArtwork persists custom uri (falls back in test without context)`() = runTest {
        val file = temporaryFolder.newFile("playlists-art.preferences_pb")
        val repository = repository(backgroundScope, file)

        val playlist = repository.createPlaylist("Favoritas")
        val custom = "content://media/external/images/media/999"
        repository.setPlaylistArtwork(playlist.id, custom)

        val restored = repository.observePlaylists().first().single { it.id == playlist.id }
        assertEquals(custom, restored.artworkUri)

        repository.setPlaylistArtwork(playlist.id, null)
        val cleared = repository.observePlaylists().first().single { it.id == playlist.id }
        assertEquals(null, cleared.artworkUri)
    }

    @Test
    fun `liked playlist is available by default and first`() = runTest {
        val file = temporaryFolder.newFile("liked-default.preferences_pb")
        val repository = repository(backgroundScope, file)

        val playlists = repository.observePlaylists().first()

        assertTrue(playlists.isNotEmpty())
        assertEquals(LIKED_PLAYLIST_ID, playlists.first().id)
        assertEquals(LIKED_PLAYLIST_NAME, playlists.first().name)
    }

    @Test
    fun `liked playlist cannot be deleted or renamed`() = runTest {
        val file = temporaryFolder.newFile("liked-protected.preferences_pb")
        val repository = repository(backgroundScope, file)

        repository.renamePlaylist(LIKED_PLAYLIST_ID, "Otro nombre")
        repository.deletePlaylist(LIKED_PLAYLIST_ID)

        val liked = repository.observePlaylists().first().first()
        assertEquals(LIKED_PLAYLIST_ID, liked.id)
        assertEquals(LIKED_PLAYLIST_NAME, liked.name)
    }

    @Test
    fun `liked playlist accepts tracks without duplicates`() = runTest {
        val file = temporaryFolder.newFile("liked-tracks.preferences_pb")
        val repository = repository(backgroundScope, file)

        assertEquals(2, repository.addTracks(LIKED_PLAYLIST_ID, listOf("one", "two", "one")))
        assertEquals(0, repository.addTracks(LIKED_PLAYLIST_ID, listOf("one", "two")))

        val liked = repository.observePlaylists().first().first()
        assertEquals(listOf("one", "two"), liked.trackIds)
    }

    private fun repository(scope: CoroutineScope, file: File) = DataStorePlaylistRepository(
        PreferenceDataStoreFactory.create(scope = scope, produceFile = { file }),
    )
}
