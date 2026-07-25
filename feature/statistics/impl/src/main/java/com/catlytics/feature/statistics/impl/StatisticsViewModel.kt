package com.catlytics.feature.statistics.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.catlytics.core.domain.usecase.statistics.BuildListeningNarrativeUseCase
import com.catlytics.core.domain.usecase.statistics.ObserveListeningStreakUseCase
import com.catlytics.core.domain.usecase.statistics.ObserveListeningTotalsUseCase
import com.catlytics.core.domain.usecase.statistics.ObservePeriodStatsUseCase
import com.catlytics.core.model.ListeningNarrative
import com.catlytics.core.model.ListeningStreak
import com.catlytics.core.model.ListeningTotals
import com.catlytics.core.model.PeriodStats
import com.catlytics.core.model.StatsGranularity
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class StatisticsDashboardData(
    val streak: ListeningStreak,
    val thisWeek: PeriodStats,
    val narrative: ListeningNarrative,
    val totals: ListeningTotals,
    val hasAnyHistory: Boolean,
)

sealed interface StatisticsUiState {
    data object Loading : StatisticsUiState
    data class Success(val data: StatisticsDashboardData) : StatisticsUiState
    data object Error : StatisticsUiState
}

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    observePeriodStats: ObservePeriodStatsUseCase,
    observeListeningStreak: ObserveListeningStreakUseCase,
    observeListeningTotals: ObserveListeningTotalsUseCase,
    buildListeningNarrative: BuildListeningNarrativeUseCase,
) : ViewModel() {

    val uiState: StateFlow<StatisticsUiState> = combine(
        observePeriodStats(StatsGranularity.WEEK, offset = 0, topLimit = 5),
        observeListeningStreak(),
        observeListeningTotals(),
    ) { week, streak, totals ->
        val narrative = buildListeningNarrative(week)
        val hasAnyHistory = totals.trackCount > 0 ||
            week.playCount > 0 ||
            streak.lastActiveDayEpochDay != null
        StatisticsDashboardData(
            streak = streak,
            thisWeek = week,
            narrative = narrative,
            totals = totals,
            hasAnyHistory = hasAnyHistory,
        )
    }
        .map { StatisticsUiState.Success(it) as StatisticsUiState }
        .catch { emit(StatisticsUiState.Error) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = StatisticsUiState.Loading,
        )
}
