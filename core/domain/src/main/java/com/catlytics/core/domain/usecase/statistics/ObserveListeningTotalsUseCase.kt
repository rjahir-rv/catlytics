package com.catlytics.core.domain.usecase.statistics

import com.catlytics.core.domain.repository.PlaybackEventRepository
import com.catlytics.core.model.ListeningTotals
import kotlinx.coroutines.flow.Flow

class ObserveListeningTotalsUseCase(
    private val playbackEventRepository: PlaybackEventRepository,
) {
    operator fun invoke(): Flow<ListeningTotals> = playbackEventRepository.observeListeningTotals()
}
