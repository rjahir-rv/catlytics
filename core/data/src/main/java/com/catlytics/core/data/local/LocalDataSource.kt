package com.catlytics.core.data.local

import com.catlytics.core.data.model.PlaylistEntity
import com.catlytics.core.data.model.TrackEntity
import kotlinx.coroutines.flow.Flow

interface LocalDataSource {
    fun observeTracks(): Flow<List<TrackEntity>>

    fun observePlaylists(): Flow<List<PlaylistEntity>>

    suspend fun upsertTracks(tracks: List<TrackEntity>)

    suspend fun replaceTracks(tracks: List<TrackEntity>)

    suspend fun upsertPlaylist(playlist: PlaylistEntity)
}
