package com.catlytics.core.data.mediator

import com.catlytics.core.data.local.CatlyticsLocalDataSource
import com.catlytics.core.data.remote.CatlyticsRemoteDataSource
import javax.inject.Inject

class OfflineFirstCatlyticsDataMediator @Inject constructor(
    private val localDataSource: CatlyticsLocalDataSource,
    private val remoteDataSource: CatlyticsRemoteDataSource,
) : CatlyticsDataMediator {
    override suspend fun syncLibrary() {
        val remoteTracks = remoteDataSource.fetchLibrary()
        if (remoteTracks.isNotEmpty()) {
            localDataSource.upsertTracks(remoteTracks)
        }
    }
}
