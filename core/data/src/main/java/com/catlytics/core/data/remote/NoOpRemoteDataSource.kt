package com.catlytics.core.data.remote

import com.catlytics.core.data.model.TrackEntity
import javax.inject.Inject

class NoOpRemoteDataSource @Inject constructor() : RemoteDataSource {
    override suspend fun fetchLibrary(): List<TrackEntity> = emptyList()
}
