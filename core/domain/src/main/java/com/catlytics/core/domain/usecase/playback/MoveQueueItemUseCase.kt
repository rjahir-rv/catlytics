package com.catlytics.core.domain.usecase.playback

import com.catlytics.core.domain.repository.PlaybackController

class MoveQueueItemUseCase(
    private val playbackController: PlaybackController,
) {
    suspend operator fun invoke(fromIndex: Int, toIndex: Int) {
        playbackController.moveQueueItem(fromIndex, toIndex)
    }
}
