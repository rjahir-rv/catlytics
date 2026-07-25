package com.catlytics.core.domain.usecase.statistics

import com.catlytics.core.domain.repository.PlaybackEventRepository
import com.catlytics.core.model.DailyListeningStat
import com.catlytics.core.model.PeriodStats
import com.catlytics.core.model.PeriodUniqueCounts
import com.catlytics.core.model.StatsGranularity
import com.catlytics.core.model.TopAlbum
import com.catlytics.core.model.TopArtist
import com.catlytics.core.model.TopTrack
import java.time.Clock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class ObservePeriodStatsUseCase(
    private val playbackEventRepository: PlaybackEventRepository,
    private val clock: Clock = Clock.systemDefaultZone(),
) {
    operator fun invoke(
        granularity: StatsGranularity,
        offset: Int = 0,
        topLimit: Int = 10,
    ): Flow<PeriodStats> {
        val range = StatsPeriodCalculator.calculateRange(granularity, offset, clock)
        val start = range.startMillis
        val end = range.endMillis

        val listeningFlow = combine(
            playbackEventRepository.observeTotalListeningTime(start, end),
            playbackEventRepository.observePlayCount(start, end),
            playbackEventRepository.observePeriodUniqueCounts(start, end),
            playbackEventRepository.observeDailyListening(start, end),
        ) { totalMillis, playCount, unique, daily ->
            ListeningSlice(totalMillis, playCount, unique, daily)
        }

        val rankingsFlow = combine(
            playbackEventRepository.observeTopTracks(start, end, topLimit),
            playbackEventRepository.observeTopArtists(start, end, topLimit),
            playbackEventRepository.observeTopAlbums(start, end, topLimit),
        ) { tracks, artists, albums ->
            RankingsSlice(tracks, artists, albums)
        }

        return combine(listeningFlow, rankingsFlow) { listening, rankings ->
            PeriodStats(
                range = range,
                totalListenedMillis = listening.totalMillis,
                playCount = listening.playCount,
                uniqueTracks = listening.unique.trackCount,
                uniqueArtists = listening.unique.artistCount,
                uniqueAlbums = listening.unique.albumCount,
                dailyListening = listening.daily,
                topTracks = rankings.tracks,
                topArtists = rankings.artists,
                topAlbums = rankings.albums,
            )
        }
    }

    private data class ListeningSlice(
        val totalMillis: Long,
        val playCount: Int,
        val unique: PeriodUniqueCounts,
        val daily: List<DailyListeningStat>,
    )

    private data class RankingsSlice(
        val tracks: List<TopTrack>,
        val artists: List<TopArtist>,
        val albums: List<TopAlbum>,
    )
}
