package com.catlytics.core.domain.usecase.statistics

import com.catlytics.core.domain.repository.PlaybackEventRepository
import com.catlytics.core.model.StatsGranularity
import com.catlytics.core.model.WeeklyStats
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock

class ObserveWeeklyStatsUseCase(
    private val playbackEventRepository: PlaybackEventRepository,
    private val clock: Clock = Clock.systemDefaultZone(),
) {
    private val periodStats = ObservePeriodStatsUseCase(playbackEventRepository, clock)

    operator fun invoke(weekOffset: Int = 0): Flow<WeeklyStats> {
        return periodStats(
            granularity = StatsGranularity.WEEK,
            offset = weekOffset,
            topLimit = 5,
        ).map { period ->
            WeeklyStats(
                weekStart = period.range.startMillis,
                weekEnd = period.range.endMillis,
                topTracks = period.topTracks,
                topArtists = period.topArtists,
                totalListenedMillis = period.totalListenedMillis,
                dailyListening = period.dailyListening,
                playCount = period.playCount,
            )
        }
    }
}
