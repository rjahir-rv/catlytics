package com.catlytics.core.data.repository

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
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

        val restored = repository.observePlaylists().first().single()
        assertEquals("Viaje", restored.name)
        assertEquals(listOf("two"), restored.trackIds)

        repository.deletePlaylist(playlist.id)
        assertTrue(repository.observePlaylists().first().isEmpty())
    }

    @Test
    fun `setPlaylistArtwork persists custom uri (falls back in test without context)`() = runTest {
        val file = temporaryFolder.newFile("playlists-art.preferences_pb")
        val repository = repository(backgroundScope, file)

        val playlist = repository.createPlaylist("Favoritas")
        val custom = "content://media/external/images/media/999"
        repository.setPlaylistArtwork(playlist.id, custom)

        val restored = repository.observePlaylists().first().single()
        assertEquals(custom, restored.artworkUri)

        repository.setPlaylistArtwork(playlist.id, null)
        val cleared = repository.observePlaylists().first().single()
        assertEquals(null, cleared.artworkUri)
    }

    private fun repository(scope: CoroutineScope, file: File) = DataStorePlaylistRepository(
        PreferenceDataStoreFactory.create(scope = scope, produceFile = { file }),
    )
}
