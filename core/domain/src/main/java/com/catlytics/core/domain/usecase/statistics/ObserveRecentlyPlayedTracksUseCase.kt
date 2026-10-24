package com.catlytics.core.domain.usecase.statistics

import com.catlytics.core.domain.repository.PlaybackEventRepository
import com.catlytics.core.model.RecentlyPlayedTrack
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveRecentlyPlayedTracksUseCase @Inject constructor(
    private val playbackEventRepository: PlaybackEventRepository,
) {
    operator fun invoke(limit: Int): Flow<List<RecentlyPlayedTrack>> =
        playbackEventRepository.observeRecentlyPlayedTracks(limit)
}
