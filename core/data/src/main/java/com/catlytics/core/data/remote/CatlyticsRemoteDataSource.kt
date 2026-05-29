package com.catlytics.core.data.remote

import com.catlytics.core.data.model.TrackEntity

interface CatlyticsRemoteDataSource {
    suspend fun fetchLibrary(): List<TrackEntity>
}
