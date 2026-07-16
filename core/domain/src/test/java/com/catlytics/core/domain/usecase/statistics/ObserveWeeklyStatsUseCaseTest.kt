package com.catlytics.core.domain.usecase.statistics

import com.catlytics.core.domain.repository.PlaybackEventRepository
import com.catlytics.core.model.PlaybackEvent
import com.catlytics.core.model.DailyListeningStat
import com.catlytics.core.model.ListeningTotals
import com.catlytics.core.model.RecentlyPlayedTrack
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
import java.time.ZoneId

class FakePlaybackEventRepository2 : PlaybackEventRepository {
    var lastStartObserved: Long = 0L
    var lastEndObserved: Long = 0L
    var lastLimitObserved: Int = 0
    val recentlyPlayedTracks = MutableStateFlow(emptyList<RecentlyPlayedTrack>())

    override suspend fun recordEvent(event: PlaybackEvent) {}

    override fun observeRecentlyPlayedTracks(limit: Int): Flow<List<RecentlyPlayedTrack>> = recentlyPlayedTracks

    override fun observeTopTracks(startMillis: Long, endMillis: Long, limit: Int): Flow<List<TopTrack>> {
        lastStartObserved = startMillis
        lastEndObserved = endMillis
        lastLimitObserved = limit
        return flowOf(
            listOf(
                TopTrack("track-1", "Track One", "Artist One", null, 10, 300_000L)
            )
        )
    }

    override fun observeTopArtists(startMillis: Long, endMillis: Long, limit: Int): Flow<List<TopArtist>> {
        return flowOf(
            listOf(
                TopArtist("artist-1", "Artist One", null, 10, 300_000L)
            )
        )
    }

    override fun observeTotalListeningTime(startMillis: Long, endMillis: Long): Flow<Long> {
        return flowOf(500_000L)
    }

    override fun observeDailyListening(
        startMillis: Long,
        endMillis: Long,
    ): Flow<List<DailyListeningStat>> = flowOf(
        listOf(DailyListeningStat(dayOfWeek = 1, totalListenedMillis = 120_000L))
    )

    override fun observePlayCount(startMillis: Long, endMillis: Long): Flow<Int> = flowOf(3)

    override fun observeListeningTotals(): Flow<ListeningTotals> = flowOf(ListeningTotals(1, 1, 1))

    override suspend fun cleanOldEvents(beforeMillis: Long): Int = 0
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
        assertEquals(1, stats.dailyListening.single().dayOfWeek)
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
