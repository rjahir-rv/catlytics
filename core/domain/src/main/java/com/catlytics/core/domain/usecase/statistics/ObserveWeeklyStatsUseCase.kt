package com.catlytics.core.domain.usecase.statistics

import com.catlytics.core.domain.repository.PlaybackEventRepository
import com.catlytics.core.model.WeeklyStats
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class ObserveWeeklyStatsUseCase(
    private val playbackEventRepository: PlaybackEventRepository,
    private val clock: Clock = Clock.systemDefaultZone()
) {
    operator fun invoke(weekOffset: Int = 0): Flow<WeeklyStats> {
        val (start, end) = calculateWeekRange(weekOffset, clock)
        return combine(
            playbackEventRepository.observeTopTracks(start, end, limit = 5),
            playbackEventRepository.observeTopArtists(start, end, limit = 5),
            playbackEventRepository.observeTotalListeningTime(start, end)
        ) { topTracks, topArtists, totalMillis ->
            WeeklyStats(
                weekStart = start,
                weekEnd = end,
                topTracks = topTracks,
                topArtists = topArtists,
                totalListenedMillis = totalMillis
            )
        }
    }

    private fun calculateWeekRange(weekOffset: Int, clock: Clock): Pair<Long, Long> {
        val zone = clock.zone
        val today = LocalDate.now(clock)
        val startOfWeek = today
            .with(DayOfWeek.MONDAY)
            .plusWeeks(weekOffset.toLong())
        val endOfWeek = startOfWeek.plusWeeks(1)
        return Pair(
            startOfWeek.atStartOfDay(zone).toInstant().toEpochMilli(),
            endOfWeek.atStartOfDay(zone).toInstant().toEpochMilli()
        )
    }
}
