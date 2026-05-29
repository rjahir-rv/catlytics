package com.catlytics.core.data.remote

import com.catlytics.core.data.model.TrackEntity
import javax.inject.Inject

class NoOpCatlyticsRemoteDataSource @Inject constructor() : CatlyticsRemoteDataSource {
    override suspend fun fetchLibrary(): List<TrackEntity> = emptyList()
}
