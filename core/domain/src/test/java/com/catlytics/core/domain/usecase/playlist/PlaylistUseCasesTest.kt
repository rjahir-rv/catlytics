package com.catlytics.core.domain.usecase.playlist

import com.catlytics.core.domain.repository.PlaylistRepository
import com.catlytics.core.model.LIKED_PLAYLIST_ID
import com.catlytics.core.model.Playlist
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaylistUseCasesTest {
    @Test
    fun `toggle liked track adds and removes the track`() = runTest {
        val repository = FakePlaylistRepository()
        val useCase = ToggleLikedTrackUseCase(repository)

        assertEquals(ToggleLikedTrackResult.Added, useCase("track-1"))
        assertEquals(ToggleLikedTrackResult.Removed, useCase("track-1"))
        assertTrue(
            repository.observePlaylists().first()
                .first { it.id == LIKED_PLAYLIST_ID }
                .trackIds
                .isEmpty(),
        )
    }

    @Test
    fun `observe is track liked follows liked playlist contents`() = runTest {
        val repository = FakePlaylistRepository()
        val useCase = ObserveIsTrackLikedUseCase(repository)

        assertFalse(useCase("track-1").first())

        repository.addTracks(LIKED_PLAYLIST_ID, listOf("track-1"))

        assertTrue(useCase("track-1").first())
        assertFalse(useCase("track-2").first())
        assertFalse(useCase(null).first())
    }

    private class FakePlaylistRepository : PlaylistRepository {
        private val playlists = MutableStateFlow(
            listOf(Playlist(LIKED_PLAYLIST_ID, "Tus me gusta", emptyList())),
        )

        override fun observePlaylists(): Flow<List<Playlist>> = playlists

        override suspend fun createPlaylist(name: String, trackIds: List<String>): Playlist {
            val playlist = Playlist("playlist-${playlists.value.size}", name, trackIds.distinct())
            playlists.value = playlists.value + playlist
            return playlist
        }

        override suspend fun renamePlaylist(playlistId: String, name: String) {
            playlists.value = playlists.value.map {
                if (it.id == playlistId) it.copy(name = name) else it
            }
        }

        override suspend fun deletePlaylist(playlistId: String) {
            playlists.value = playlists.value.filterNot { it.id == playlistId }
        }

        override suspend fun addTracks(playlistId: String, trackIds: List<String>): Int {
            var added = 0
            playlists.value = playlists.value.map { playlist ->
                if (playlist.id != playlistId) return@map playlist
                val newIds = trackIds.distinct().filterNot(playlist.trackIds::contains)
                added = newIds.size
                playlist.copy(trackIds = playlist.trackIds + newIds)
            }
            return added
        }

        override suspend fun removeTrack(playlistId: String, trackId: String) {
            playlists.value = playlists.value.map { playlist ->
                if (playlist.id == playlistId) playlist.copy(trackIds = playlist.trackIds - trackId)
                else playlist
            }
        }

        override suspend fun setPlaylistArtwork(playlistId: String, artworkUri: String?) {
            playlists.value = playlists.value.map { playlist ->
                if (playlist.id == playlistId) playlist.copy(artworkUri = artworkUri) else playlist
            }
        }
    }
}
