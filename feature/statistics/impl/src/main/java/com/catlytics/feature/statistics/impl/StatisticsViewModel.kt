package com.catlytics.feature.statistics.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.catlytics.core.domain.usecase.statistics.ObserveWeeklyStatsUseCase
import com.catlytics.core.model.WeeklyStats
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

sealed interface StatisticsUiState {
    data object Loading : StatisticsUiState
    data class Success(val weekOffset: Int, val stats: WeeklyStats) : StatisticsUiState
    data object Error : StatisticsUiState
}

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val observeWeeklyStats: ObserveWeeklyStatsUseCase
) : ViewModel() {

    val weekOffset = MutableStateFlow(0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<StatisticsUiState> = weekOffset
        .flatMapLatest { offset ->
            observeWeeklyStats(offset)
                .map { stats ->
                    StatisticsUiState.Success(
                        weekOffset = offset,
                        stats = stats
                    ) as StatisticsUiState
                }
                .catch {
                    emit(StatisticsUiState.Error)
                }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = StatisticsUiState.Loading
        )

    fun selectWeek(offset: Int) {
        if (offset == 0 || offset == -1) {
            weekOffset.value = offset
        }
    }
}
