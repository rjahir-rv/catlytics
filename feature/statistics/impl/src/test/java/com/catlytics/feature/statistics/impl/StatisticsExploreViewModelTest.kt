package com.catlytics.feature.statistics.impl

import com.catlytics.core.domain.usecase.statistics.BuildListeningNarrativeUseCase
import com.catlytics.core.domain.usecase.statistics.ObservePeriodStatsUseCase
import com.catlytics.core.domain.usecase.statistics.StatsPeriodCalculator
import com.catlytics.core.model.StatsGranularity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StatisticsExploreViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakePlaybackEventRepository()
    private val thisWeek = StatsPeriodCalculator.calculateRange(StatsGranularity.WEEK, offset = 0)
    private val lastWeek = StatsPeriodCalculator.calculateRange(StatsGranularity.WEEK, offset = -1)

    @Test
    fun `starts at the current week without navigation when there is no history`() = runTest {
        val viewModel = viewModel()
        assertEquals(StatisticsExploreUiState.Loading, viewModel.uiState.value)

        val data = startCollecting(viewModel)

        assertEquals(ExplorePeriodSelection(), data.selection)
        assertTrue(data.stats.isEmpty)
        assertFalse(data.canGoBack)
        assertFalse(data.canGoForward)
    }

    @Test
    fun `history before this week lets the user step back to it`() = runTest {
        repository.events.value = listOf(
            playbackEvent(lastWeek.startMillis + HOUR),
            playbackEvent(lastWeek.startMillis + 2 * HOUR),
            playbackEvent(thisWeek.startMillis + HOUR),
        )
        val viewModel = viewModel()

        val current = startCollecting(viewModel)
        assertEquals(1, current.stats.playCount)
        assertTrue(current.canGoBack)

        viewModel.shiftPeriod(-1)
        advanceUntilIdle()

        val previous = viewModel.successData()
        assertEquals(ExplorePeriodSelection(offset = -1), previous.selection)
        assertEquals(lastWeek.startMillis, previous.stats.range.startMillis)
        assertEquals(2, previous.stats.playCount)
        assertTrue(previous.canGoForward)
        assertFalse(previous.canGoBack)
    }

    @Test
    fun `shifting forward never goes past the current period`() = runTest {
        val viewModel = viewModel()
        startCollecting(viewModel)

        viewModel.shiftPeriod(1)
        advanceUntilIdle()
        assertEquals(0, viewModel.successData().selection.offset)

        viewModel.shiftPeriod(-2)
        viewModel.shiftPeriod(5)
        advanceUntilIdle()
        assertEquals(0, viewModel.successData().selection.offset)
    }

    @Test
    fun `changing the granularity resets the offset and keeping it does not`() = runTest {
        val viewModel = viewModel()
        startCollecting(viewModel)
        viewModel.shiftPeriod(-1)

        viewModel.setGranularity(StatsGranularity.WEEK)
        advanceUntilIdle()
        assertEquals(-1, viewModel.successData().selection.offset)

        viewModel.setGranularity(StatsGranularity.MONTH)
        advanceUntilIdle()
        val month = viewModel.successData()
        assertEquals(ExplorePeriodSelection(StatsGranularity.MONTH, offset = 0), month.selection)
        assertEquals(StatsGranularity.MONTH, month.stats.range.granularity)
    }

    @Test
    fun `a failing source shows the error state`() = runTest {
        repository.failure = IllegalStateException("database closed")
        val viewModel = viewModel()
        backgroundScope.launch { viewModel.uiState.collect() }
        advanceUntilIdle()

        assertEquals(StatisticsExploreUiState.Error, viewModel.uiState.value)
    }

    private fun viewModel() = StatisticsExploreViewModel(
        observePeriodStats = ObservePeriodStatsUseCase(repository),
        buildListeningNarrative = BuildListeningNarrativeUseCase(),
        playbackEventRepository = repository,
    )

    private fun TestScope.startCollecting(viewModel: StatisticsExploreViewModel): StatisticsExploreData {
        backgroundScope.launch { viewModel.uiState.collect() }
        advanceUntilIdle()
        return viewModel.successData()
    }

    private fun StatisticsExploreViewModel.successData(): StatisticsExploreData =
        (uiState.value as StatisticsExploreUiState.Success).data
}
