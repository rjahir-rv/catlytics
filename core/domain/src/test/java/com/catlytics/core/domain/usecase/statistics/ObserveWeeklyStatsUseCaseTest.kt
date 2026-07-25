package com.catlytics.core.domain.usecase.statistics

import com.catlytics.core.domain.repository.PlaybackEventRepository
import com.catlytics.core.model.PlaybackEvent
import com.catlytics.core.model.DailyListeningStat
import com.catlytics.core.model.ListeningTotals
import com.catlytics.core.model.PeriodUniqueCounts
import com.catlytics.core.model.RecentlyPlayedTrack
import com.catlytics.core.model.TopAlbum
import com.catlytics.core.model.TopArtist
import com.catlytics.core.model.TopTrack
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class FakePlaybackEventRepository2 : PlaybackEventRepository {
    var lastStartObserved: Long = 0L
    var lastEndObserved: Long = 0L
    var lastLimitObserved: Int = 0
    val recentlyPlayedTracks = MutableStateFlow(emptyList<RecentlyPlayedTrack>())
    var activeDays: List<LocalDate> = emptyList()
    var totalListeningMillis: Long = 500_000L
    var topTracks: List<TopTrack> = listOf(
        TopTrack("track-1", "Track One", "Artist One", null, 10, 300_000L),
    )
    var topArtists: List<TopArtist> = listOf(
        TopArtist("artist-1", "Artist One", null, 10, 300_000L),
    )
    var topAlbums: List<TopAlbum> = emptyList()

    override suspend fun recordEvent(event: PlaybackEvent) {}

    override fun observeRecentlyPlayedTracks(limit: Int): Flow<List<RecentlyPlayedTrack>> = recentlyPlayedTracks

    override fun observeTopTracks(startMillis: Long, endMillis: Long, limit: Int): Flow<List<TopTrack>> {
        lastStartObserved = startMillis
        lastEndObserved = endMillis
        lastLimitObserved = limit
        return flowOf(topTracks)
    }

    override fun observeTopArtists(startMillis: Long, endMillis: Long, limit: Int): Flow<List<TopArtist>> {
        return flowOf(topArtists)
    }

    override fun observeTopAlbums(startMillis: Long, endMillis: Long, limit: Int): Flow<List<TopAlbum>> {
        return flowOf(topAlbums)
    }

    override fun observeTotalListeningTime(startMillis: Long, endMillis: Long): Flow<Long> {
        return flowOf(totalListeningMillis)
    }

    override fun observeDailyListening(
        startMillis: Long,
        endMillis: Long,
    ): Flow<List<DailyListeningStat>> = flowOf(
        listOf(DailyListeningStat(dayIndex = 1, totalListenedMillis = 120_000L))
    )

    override fun observePlayCount(startMillis: Long, endMillis: Long): Flow<Int> = flowOf(3)

    override fun observePeriodUniqueCounts(
        startMillis: Long,
        endMillis: Long,
    ): Flow<PeriodUniqueCounts> = flowOf(PeriodUniqueCounts(2, 1, 1))

    override fun observeListeningTotals(): Flow<ListeningTotals> = flowOf(ListeningTotals(1, 1, 1))

    override fun observeActiveListeningDays(): Flow<List<LocalDate>> = flowOf(activeDays)

    override suspend fun cleanOldEvents(beforeMillis: Long): Int = 0

    override suspend fun getAllEvents(): List<PlaybackEvent> = emptyList()

    override suspend fun replaceEvents(events: List<PlaybackEvent>) = Unit

    override suspend fun insertEventsIfAbsent(events: List<PlaybackEvent>): Int = 0

    override fun observeBackupSummary(): Flow<com.catlytics.core.model.StatisticsBackupSummary> =
        flowOf(com.catlytics.core.model.StatisticsBackupSummary(0, null, null))

}

class ObserveRecentlyPlayedTracksUseCaseTest {
    private val repository = FakePlaybackEventRepository2()
    private val useCase = ObserveRecentlyPlayedTracksUseCase(repository)

    @Test
    fun `observes recently played tracks from repository`() = runTest {
        val recentTrack = RecentlyPlayedTrack(
            trackId = "track-1",
            title = "Track One",
            artistName = "Artist One",
            artworkUri = null,
            lastListenedAtMillis = 1L,
        )
        repository.recentlyPlayedTracks.value = listOf(recentTrack)

        assertEquals(listOf(recentTrack), useCase(limit = 10).first())
    }
}

class ObserveWeeklyStatsUseCaseTest {

    private val repository = FakePlaybackEventRepository2()

    // Fixed clock: Wednesday, June 24, 2026 (local time or UTC)
    private val clock = Clock.fixed(
        Instant.parse("2026-06-24T12:00:00Z"),
        ZoneId.of("UTC")
    )

    private val useCase = ObserveWeeklyStatsUseCase(repository, clock)

    @Test
    fun `observes weekly stats for current week`() = runTest {
        val stats = useCase(weekOffset = 0).first()

        // Monday of that week: June 22, 2026 00:00:00 UTC -> 1782086400000L
        // Sunday of that week ends, so next Monday is June 29, 2026 00:00:00 UTC -> 1782691200000L.
        val expectedStart = 1782086400000L
        val expectedEnd = 1782691200000L

        assertEquals(expectedStart, stats.weekStart)
        assertEquals(expectedEnd, stats.weekEnd)
        assertEquals(expectedStart, repository.lastStartObserved)
        assertEquals(expectedEnd, repository.lastEndObserved)
        assertEquals(5, repository.lastLimitObserved)

        assertEquals(1, stats.topTracks.size)
        assertEquals("track-1", stats.topTracks[0].trackId)
        assertEquals(1, stats.topArtists.size)
        assertEquals("artist-1", stats.topArtists[0].artistId)
        assertEquals(500_000L, stats.totalListenedMillis)
        assertEquals(3, stats.playCount)
        assertEquals(1, stats.dailyListening.single().dayIndex)
    }

    @Test
    fun `observes weekly stats for previous week`() = runTest {
        val stats = useCase(weekOffset = -1).first()

        // Previous week start (Monday, June 15, 2026 00:00:00 UTC) -> 1781481600000L
        // Previous week end (Monday, June 22, 2026 00:00:00 UTC) -> 1782086400000L
        val expectedStart = 1781481600000L
        val expectedEnd = 1782086400000L

        assertEquals(expectedStart, stats.weekStart)
        assertEquals(expectedEnd, stats.weekEnd)
        assertEquals(expectedStart, repository.lastStartObserved)
        assertEquals(expectedEnd, repository.lastEndObserved)
    }
}

class ObserveListeningStreakUseCaseTest {

    @Test
    fun `empty history yields zero streak`() {
        val streak = ObserveListeningStreakUseCase.computeStreak(
            activeDaysNewestFirst = emptyList(),
            today = LocalDate.of(2026, 6, 24),
        )
        assertEquals(0, streak.currentDays)
        assertEquals(null, streak.lastActiveDayEpochDay)
    }

    @Test
    fun `counts consecutive days including today`() {
        val today = LocalDate.of(2026, 6, 24)
        val streak = ObserveListeningStreakUseCase.computeStreak(
            activeDaysNewestFirst = listOf(today, today.minusDays(1), today.minusDays(2)),
            today = today,
        )
        assertEquals(3, streak.currentDays)
    }

    @Test
    fun `grace allows streak when only yesterday is active`() {
        val today = LocalDate.of(2026, 6, 24)
        val yesterday = today.minusDays(1)
        val streak = ObserveListeningStreakUseCase.computeStreak(
            activeDaysNewestFirst = listOf(yesterday, yesterday.minusDays(1)),
            today = today,
        )
        assertEquals(2, streak.currentDays)
    }

    @Test
    fun `gap breaks streak`() {
        val today = LocalDate.of(2026, 6, 24)
        val streak = ObserveListeningStreakUseCase.computeStreak(
            activeDaysNewestFirst = listOf(today, today.minusDays(2)),
            today = today,
        )
        assertEquals(1, streak.currentDays)
    }

    @Test
    fun `old activity without today or yesterday yields zero`() {
        val today = LocalDate.of(2026, 6, 24)
        val streak = ObserveListeningStreakUseCase.computeStreak(
            activeDaysNewestFirst = listOf(today.minusDays(3)),
            today = today,
        )
        assertEquals(0, streak.currentDays)
    }
}

class BuildListeningNarrativeUseCaseTest {

    private val useCase = BuildListeningNarrativeUseCase()

    @Test
    fun `not eligible under one hour`() = runTest {
        val repository = FakePlaybackEventRepository2().apply {
            totalListeningMillis = 30 * 60_000L
        }
        val stats = ObservePeriodStatsUseCase(
            repository,
            Clock.fixed(Instant.parse("2026-06-24T12:00:00Z"), ZoneId.of("UTC")),
        ).invoke(com.catlytics.core.model.StatsGranularity.WEEK).first()

        val narrative = useCase(stats)
        assertEquals(false, narrative.eligible)
        assertEquals("", narrative.headline)
    }

    @Test
    fun `eligible builds headline with top artist`() = runTest {
        val repository = FakePlaybackEventRepository2().apply {
            totalListeningMillis = 2 * 3_600_000L
            topArtists = listOf(
                TopArtist("a1", "Eminem", null, 20, 4_000_000L),
            )
            topTracks = listOf(
                TopTrack("t1", "Rap God", "Eminem", null, 18, 1_000_000L),
            )
        }
        val stats = ObservePeriodStatsUseCase(
            repository,
            Clock.fixed(Instant.parse("2026-06-24T12:00:00Z"), ZoneId.of("UTC")),
        ).invoke(com.catlytics.core.model.StatsGranularity.WEEK).first()

        val narrative = useCase(stats)
        assertEquals(true, narrative.eligible)
        assertEquals("Pasaste más tiempo con Eminem", narrative.headline)
        assertEquals(
            true,
            narrative.supportingLines.any {
                it.contains("Canción más escuchada") && it.contains("Rap God")
            },
        )
        assertEquals(
            true,
            narrative.supportingLines.any {
                it.contains("Artista más escuchado") && it.contains("Eminem")
            },
        )
    }
}
