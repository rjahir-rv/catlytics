package com.catlytics.core.data.mediator

import com.catlytics.core.data.local.LocalDataSource
import com.catlytics.core.data.local.MediaStoreLibraryDataSource
import com.catlytics.core.data.remote.RemoteDataSource
import javax.inject.Inject

class OfflineFirstDataMediator @Inject constructor(
    private val localDataSource: LocalDataSource,
    private val mediaStoreLibraryDataSource: MediaStoreLibraryDataSource,
    private val remoteDataSource: RemoteDataSource,
) : DataMediator {
    override suspend fun syncLibrary() {
        val localTracks = mediaStoreLibraryDataSource.loadTracks()
        localDataSource.replaceTracks(localTracks)

        val remoteTracks = remoteDataSource.fetchLibrary()
        if (remoteTracks.isNotEmpty()) {
            localDataSource.upsertTracks(remoteTracks)
        }
    }
}
