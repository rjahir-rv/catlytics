package com.catlytics.feature.statistics.impl

import com.catlytics.core.domain.usecase.statistics.BuildListeningNarrativeUseCase
import com.catlytics.core.domain.usecase.statistics.ObserveListeningStreakUseCase
import com.catlytics.core.domain.usecase.statistics.ObserveListeningTotalsUseCase
import com.catlytics.core.domain.usecase.statistics.ObservePeriodStatsUseCase
import com.catlytics.core.domain.usecase.statistics.StatsPeriodCalculator
import com.catlytics.core.model.ListeningNarrativeKind
import com.catlytics.core.model.StatsGranularity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StatisticsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakePlaybackEventRepository()
    private val thisWeek = StatsPeriodCalculator.calculateRange(StatsGranularity.WEEK, offset = 0)

    @Test
    fun `starts loading and shows an empty dashboard without history`() = runTest {
        val viewModel = viewModel()
        assertEquals(StatisticsUiState.Loading, viewModel.uiState.value)

        val data = successOf(viewModel)

        assertFalse(data.hasAnyHistory)
        assertEquals(0, data.thisWeek.playCount)
        assertEquals(0, data.streak.currentDays)
        assertFalse(data.narrative.eligible)
    }

    @Test
    fun `this week's plays fill the dashboard and the narrative`() = runTest {
        repository.events.value = listOf(
            playbackEvent(thisWeek.startMillis + HOUR, trackId = "aria", listenedMillis = 15 * MINUTE),
            playbackEvent(thisWeek.startMillis + 2 * HOUR, trackId = "aria", listenedMillis = 15 * MINUTE),
            playbackEvent(
                thisWeek.startMillis + 3 * HOUR,
                trackId = "lacrimosa",
                artistId = "artist-2",
                artistName = "Wolfgang Amadeus Mozart",
                listenedMillis = 5 * MINUTE,
            ),
        )

        val data = successOf(viewModel())

        assertTrue(data.hasAnyHistory)
        assertEquals(3, data.thisWeek.playCount)
        assertEquals(35 * MINUTE, data.thisWeek.totalListenedMillis)
        assertEquals(2, data.thisWeek.uniqueArtists)
        assertEquals(2, data.totals.trackCount)
        assertTrue(data.narrative.eligible)
        assertEquals(ListeningNarrativeKind.TimeWithArtist, data.narrative.kind)
        assertEquals("Johann Sebastian Bach", data.narrative.topArtist?.name)
    }

    @Test
    fun `listening today and yesterday counts as a two day streak`() = runTest {
        val now = System.currentTimeMillis()
        repository.events.value = listOf(
            playbackEvent(now - DAY),
            playbackEvent(now),
        )

        val data = successOf(viewModel())

        assertEquals(2, data.streak.currentDays)
    }

    @Test
    fun `older history counts as history even with an empty week`() = runTest {
        repository.events.value = listOf(playbackEvent(System.currentTimeMillis() - 30 * DAY))

        val data = successOf(viewModel())

        assertTrue(data.hasAnyHistory)
        assertEquals(0, data.thisWeek.playCount)
        assertEquals(0, data.streak.currentDays)
        assertNotNull(data.streak.lastActiveDayEpochDay)
    }

    @Test
    fun `a failing source shows the error state`() = runTest {
        repository.failure = IllegalStateException("database closed")
        val viewModel = viewModel()
        backgroundScope.launch { viewModel.uiState.collect() }
        advanceUntilIdle()

        assertEquals(StatisticsUiState.Error, viewModel.uiState.value)
    }

    private fun viewModel() = StatisticsViewModel(
        observePeriodStats = ObservePeriodStatsUseCase(repository),
        observeListeningStreak = ObserveListeningStreakUseCase(repository),
        observeListeningTotals = ObserveListeningTotalsUseCase(repository),
        buildListeningNarrative = BuildListeningNarrativeUseCase(),
    )

    private fun TestScope.successOf(viewModel: StatisticsViewModel): StatisticsDashboardData {
        backgroundScope.launch { viewModel.uiState.collect() }
        advanceUntilIdle()
        return (viewModel.uiState.value as StatisticsUiState.Success).data
    }
}
