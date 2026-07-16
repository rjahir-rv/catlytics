package com.catlytics.core.domain.usecase.statistics

import com.catlytics.core.domain.repository.PlaybackEventRepository
import com.catlytics.core.model.RecentlyPlayedTrack
import kotlinx.coroutines.flow.Flow

class ObserveRecentlyPlayedTracksUseCase(
    private val playbackEventRepository: PlaybackEventRepository,
) {
    operator fun invoke(limit: Int): Flow<List<RecentlyPlayedTrack>> =
        playbackEventRepository.observeRecentlyPlayedTracks(limit)
}
