package com.catlytics.core.domain.usecase

import com.catlytics.core.domain.repository.StatisticsRepository

class ObserveListeningStatsUseCase(
    private val statisticsRepository: StatisticsRepository,
) {
    operator fun invoke() = statisticsRepository.observeListeningStats()
}
