package com.catlytics.core.data.repository

import com.catlytics.core.data.local.CatlyticsLocalDataSource
import com.catlytics.core.domain.repository.StatisticsRepository
import com.catlytics.core.model.ListeningStats
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class DefaultStatisticsRepository @Inject constructor(
    private val localDataSource: CatlyticsLocalDataSource,
) : StatisticsRepository {
    override fun observeListeningStats() = combine(
        localDataSource.observeTracks(),
        localDataSource.observePlaylists(),
    ) { tracks, playlists ->
        ListeningStats(
            totalTracks = tracks.size,
            totalPlaylists = playlists.size,
            totalDurationMillis = tracks.sumOf { it.durationMillis },
        )
    }
}
