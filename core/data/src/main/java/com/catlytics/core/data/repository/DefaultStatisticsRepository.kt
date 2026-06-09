package com.catlytics.core.data.repository

import com.catlytics.core.data.local.LocalDataSource
import com.catlytics.core.domain.repository.LibraryRepository
import com.catlytics.core.domain.repository.StatisticsRepository
import com.catlytics.core.model.ListeningStats
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class DefaultStatisticsRepository @Inject constructor(
    private val localDataSource: LocalDataSource,
    private val libraryRepository: LibraryRepository,
) : StatisticsRepository {
    override fun observeListeningStats() = combine(
        libraryRepository.observeTracks(),
        localDataSource.observePlaylists(),
    ) { tracks, playlists ->
        ListeningStats(
            totalTracks = tracks.size,
            totalPlaylists = playlists.size,
            totalDurationMillis = tracks.sumOf { it.durationMillis },
        )
    }
}
