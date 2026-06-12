package com.catlytics.core.domain.usecase.statistics

import com.catlytics.core.domain.repository.StatisticsRepository

class ObserveListeningStatsUseCase(
    private val statisticsRepository: StatisticsRepository,
) {
    operator fun invoke() = statisticsRepository.observeListeningStats()
}
