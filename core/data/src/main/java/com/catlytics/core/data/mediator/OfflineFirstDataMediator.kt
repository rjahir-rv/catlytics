package com.catlytics.core.data.mediator

import com.catlytics.core.data.local.LocalDataSource
import com.catlytics.core.data.local.MediaStoreLibraryDataSource
import com.catlytics.core.data.remote.RemoteDataSource
import com.catlytics.core.domain.repository.LibraryPreferencesRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.first

class OfflineFirstDataMediator @Inject constructor(
    private val localDataSource: LocalDataSource,
    private val mediaStoreLibraryDataSource: MediaStoreLibraryDataSource,
    private val remoteDataSource: RemoteDataSource,
    private val libraryPreferencesRepository: LibraryPreferencesRepository,
) : DataMediator {
    override suspend fun syncLibrary() {
        val scanSettings = libraryPreferencesRepository.observeMusicScanSettings().first()
        val localTracks = mediaStoreLibraryDataSource.loadTracks(scanSettings)
        localDataSource.replaceTracks(localTracks)

        val remoteTracks = remoteDataSource.fetchLibrary()
        if (remoteTracks.isNotEmpty()) {
            localDataSource.upsertTracks(remoteTracks)
        }
    }
}
