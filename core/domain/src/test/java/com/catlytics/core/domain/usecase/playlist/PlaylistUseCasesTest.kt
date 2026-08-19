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
    fun `add tracks to liked skips ids already present`() = runTest {
        val repository = FakePlaylistRepository()
        repository.addTracks(LIKED_PLAYLIST_ID, listOf("track-1"))

        val added = AddTracksToLikedUseCase(repository)(listOf("track-1", "track-2", "track-2"))

        assertEquals(1, added)
        assertEquals(
            listOf("track-1", "track-2"),
            repository.observePlaylists().first().first { it.id == LIKED_PLAYLIST_ID }.trackIds,
        )
    }

    @Test
    fun `remove tracks from playlist drops only requested ids`() = runTest {
        val repository = FakePlaylistRepository()
        val playlist = repository.createPlaylist("Focus", listOf("one", "two", "three"))

        val removed = RemoveTrackFromPlaylistUseCase(repository)(
            playlist.id,
            listOf("two", "missing", "three"),
        )

        assertEquals(2, removed)
        assertEquals(
            listOf("one"),
            repository.observePlaylists().first().first { it.id == playlist.id }.trackIds,
        )
    }

    @Test
    fun `remove tracks from liked uses the liked playlist`() = runTest {
        val repository = FakePlaylistRepository()
        repository.addTracks(LIKED_PLAYLIST_ID, listOf("track-1", "track-2"))

        val removed = RemoveTracksFromLikedUseCase(repository)(listOf("track-1"))

        assertEquals(1, removed)
        assertEquals(
            listOf("track-2"),
            repository.observePlaylists().first().first { it.id == LIKED_PLAYLIST_ID }.trackIds,
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

        override suspend fun updatePlaylistDetails(
            playlistId: String,
            name: String,
            description: String,
        ) {
            playlists.value = playlists.value.map {
                if (it.id == playlistId) it.copy(name = name, description = description) else it
            }
        }

        override suspend fun deletePlaylist(playlistId: String) {
            playlists.value = playlists.value.filterNot { it.id == playlistId }
        }

        override suspend fun addTracks(playlistId: String, trackIds: List<String>): Int =
            addTracksToPlaylists(listOf(playlistId), trackIds)[playlistId] ?: 0

        override suspend fun addTracksToPlaylists(
            playlistIds: Collection<String>,
            trackIds: List<String>,
        ): Map<String, Int> {
            val distinctTrackIds = trackIds.distinct()
            val addedByPlaylist = mutableMapOf<String, Int>()
            playlists.value = playlists.value.map { playlist ->
                if (playlist.id !in playlistIds) return@map playlist
                val newIds = distinctTrackIds.filterNot(playlist.trackIds::contains)
                addedByPlaylist[playlist.id] = newIds.size
                playlist.copy(trackIds = playlist.trackIds + newIds)
            }
            return addedByPlaylist
        }

        override suspend fun removeTrack(playlistId: String, trackId: String) {
            removeTracks(playlistId, listOf(trackId))
        }

        override suspend fun removeTracks(playlistId: String, trackIds: Collection<String>): Int {
            val toRemove = trackIds.toSet()
            var removed = 0
            playlists.value = playlists.value.map { playlist ->
                if (playlist.id != playlistId) return@map playlist
                val remaining = playlist.trackIds.filterNot(toRemove::contains)
                removed = playlist.trackIds.size - remaining.size
                playlist.copy(trackIds = remaining)
            }
            return removed
        }

        override suspend fun reorderTracks(playlistId: String, orderedTrackIds: List<String>) {
            playlists.value = playlists.value.map {
                if (it.id == playlistId) it.copy(trackIds = orderedTrackIds) else it
            }
        }

        override suspend fun setPlaylistArtwork(playlistId: String, artworkUri: String?) {
            playlists.value = playlists.value.map { playlist ->
                if (playlist.id == playlistId) playlist.copy(artworkUri = artworkUri) else playlist
            }
        }
    }
}
