package com.catlytics.core.domain.repository

import com.catlytics.core.model.Track
import kotlinx.coroutines.flow.Flow

interface LibraryRepository {
    fun observeTracks(): Flow<List<Track>>

    suspend fun refreshTracks()
}
