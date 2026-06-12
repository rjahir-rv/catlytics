package com.catlytics.core.data.local

import com.catlytics.core.data.model.TrackEntity
import kotlinx.coroutines.flow.Flow

interface LocalDataSource {
    fun observeTracks(): Flow<List<TrackEntity>>

    suspend fun upsertTracks(tracks: List<TrackEntity>)

    suspend fun replaceTracks(tracks: List<TrackEntity>)

}
