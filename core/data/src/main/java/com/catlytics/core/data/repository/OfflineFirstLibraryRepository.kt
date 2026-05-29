package com.catlytics.core.data.repository

import com.catlytics.core.data.local.CatlyticsLocalDataSource
import com.catlytics.core.data.mediator.CatlyticsDataMediator
import com.catlytics.core.data.model.toDomain
import com.catlytics.core.domain.repository.LibraryRepository
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class OfflineFirstLibraryRepository @Inject constructor(
    private val localDataSource: CatlyticsLocalDataSource,
    private val mediator: CatlyticsDataMediator,
) : LibraryRepository {
    override fun observeTracks() = localDataSource.observeTracks()
        .map { tracks -> tracks.map { it.toDomain() } }

    override suspend fun refreshTracks() {
        mediator.syncLibrary()
    }
}
