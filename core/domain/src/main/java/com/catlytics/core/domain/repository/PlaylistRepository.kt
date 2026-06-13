package com.catlytics.core.domain.repository

import com.catlytics.core.model.Playlist
import kotlinx.coroutines.flow.Flow

interface PlaylistRepository {
    fun observePlaylists(): Flow<List<Playlist>>

    suspend fun createPlaylist(name: String, trackIds: List<String> = emptyList()): Playlist

    suspend fun renamePlaylist(playlistId: String, name: String)

    suspend fun deletePlaylist(playlistId: String)

    suspend fun addTracks(playlistId: String, trackIds: List<String>): Int

    suspend fun removeTrack(playlistId: String, trackId: String)

    suspend fun setPlaylistArtwork(playlistId: String, artworkUri: String?)
}
