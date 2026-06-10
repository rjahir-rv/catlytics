package com.catlytics.core.domain.usecase

import com.catlytics.core.domain.repository.PlaybackController

class MoveQueueItemUseCase(
    private val playbackController: PlaybackController,
) {
    suspend operator fun invoke(fromIndex: Int, toIndex: Int) {
        playbackController.moveQueueItem(fromIndex, toIndex)
    }
}
