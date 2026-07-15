package com.catlytics.core.domain.usecase.statistics

import com.catlytics.core.domain.repository.PlaybackEventRepository
import com.catlytics.core.model.WeeklyStats
import com.catlytics.core.model.DailyListeningStat
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
            playbackEventRepository.observeTotalListeningTime(start, end),
            playbackEventRepository.observeDailyListening(start, end),
            playbackEventRepository.observePlayCount(start, end),
        ) { totalMillis, dailyListening, playCount ->
            WeeklyListeningData(totalMillis, dailyListening, playCount)
        }.combine(
            combine(
                playbackEventRepository.observeTopTracks(start, end, limit = 5),
                playbackEventRepository.observeTopArtists(start, end, limit = 5),
            ) { topTracks, topArtists -> WeeklyRankings(topTracks, topArtists) }
        ) { listening, rankings ->
            WeeklyStats(
                weekStart = start,
                weekEnd = end,
                topTracks = rankings.topTracks,
                topArtists = rankings.topArtists,
                totalListenedMillis = listening.totalListenedMillis,
                dailyListening = listening.dailyListening,
                playCount = listening.playCount,
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

    private data class WeeklyListeningData(
        val totalListenedMillis: Long,
        val dailyListening: List<DailyListeningStat>,
        val playCount: Int,
    )

    private data class WeeklyRankings(
        val topTracks: List<com.catlytics.core.model.TopTrack>,
        val topArtists: List<com.catlytics.core.model.TopArtist>,
    )
}
