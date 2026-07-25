package com.catlytics.feature.statistics.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.catlytics.core.domain.repository.PlaybackEventRepository
import com.catlytics.core.domain.usecase.statistics.BuildListeningNarrativeUseCase
import com.catlytics.core.domain.usecase.statistics.ObservePeriodStatsUseCase
import com.catlytics.core.domain.usecase.statistics.StatsPeriodCalculator
import com.catlytics.core.model.ListeningNarrative
import com.catlytics.core.model.PeriodStats
import com.catlytics.core.model.StatsGranularity
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

data class ExplorePeriodSelection(
    val granularity: StatsGranularity = StatsGranularity.WEEK,
    val offset: Int = 0,
)

data class StatisticsExploreData(
    val selection: ExplorePeriodSelection,
    val stats: PeriodStats,
    val narrative: ListeningNarrative,
    val canGoBack: Boolean,
    val canGoForward: Boolean,
)

sealed interface StatisticsExploreUiState {
    data object Loading : StatisticsExploreUiState
    data class Success(val data: StatisticsExploreData) : StatisticsExploreUiState
    data object Error : StatisticsExploreUiState
}

@HiltViewModel
class StatisticsExploreViewModel @Inject constructor(
    private val observePeriodStats: ObservePeriodStatsUseCase,
    private val buildListeningNarrative: BuildListeningNarrativeUseCase,
    playbackEventRepository: PlaybackEventRepository,
) : ViewModel() {

    private val selection = MutableStateFlow(ExplorePeriodSelection())

    private val firstEventMillis = playbackEventRepository.observeBackupSummary()
        .map { it.firstEventMillis }

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<StatisticsExploreUiState> = selection
        .flatMapLatest { sel ->
            combine(
                observePeriodStats(sel.granularity, sel.offset, topLimit = 10),
                firstEventMillis,
            ) { stats, firstEvent ->
                val canGoBack = StatsPeriodCalculator.canNavigateBack(stats.range, firstEvent)
                val canGoForward = sel.offset < 0
                val narrative = buildListeningNarrative(stats)
                StatisticsExploreData(
                    selection = sel,
                    stats = stats,
                    narrative = narrative,
                    canGoBack = canGoBack,
                    canGoForward = canGoForward,
                )
            }
        }
        .map { StatisticsExploreUiState.Success(it) as StatisticsExploreUiState }
        .catch { emit(StatisticsExploreUiState.Error) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = StatisticsExploreUiState.Loading,
        )

    fun setGranularity(granularity: StatsGranularity) {
        selection.update { current ->
            if (current.granularity == granularity) current
            else ExplorePeriodSelection(granularity = granularity, offset = 0)
        }
    }

    fun shiftPeriod(delta: Int) {
        selection.update { current ->
            val nextOffset = (current.offset + delta).coerceAtMost(0)
            if (nextOffset == current.offset) current
            else current.copy(offset = nextOffset)
        }
    }
}
