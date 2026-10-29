package com.catlytics.core.data.mediator

import com.catlytics.core.data.local.LocalDataSource
import com.catlytics.core.data.local.MediaStoreLibraryDataSource
import com.catlytics.core.data.repository.RoomTrackMetadataRepository
import com.catlytics.core.domain.repository.LibraryPreferencesRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class OfflineFirstDataMediator @Inject constructor(
    private val localDataSource: LocalDataSource,
    private val mediaStoreLibraryDataSource: MediaStoreLibraryDataSource,
    private val libraryPreferencesRepository: LibraryPreferencesRepository,
    private val trackMetadataRepository: RoomTrackMetadataRepository,
) : DataMediator {
    override suspend fun syncLibrary() {
        val scanSettings = libraryPreferencesRepository.observeMusicScanSettings().first()
        val localTracks = mediaStoreLibraryDataSource.loadTracks(scanSettings)
        localDataSource.replaceTracks(localTracks)
        trackMetadataRepository.reconcile(localTracks)
    }
}
