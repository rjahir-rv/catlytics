package com.catlytics.core.domain.repository

import com.catlytics.core.model.Playlist
import kotlinx.coroutines.flow.Flow

interface PlaylistRepository {
    fun observePlaylists(): Flow<List<Playlist>>

    suspend fun upsertPlaylist(playlist: Playlist)
}
