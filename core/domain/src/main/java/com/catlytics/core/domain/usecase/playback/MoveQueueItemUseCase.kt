package com.catlytics.core.domain.usecase.playback

import com.catlytics.core.domain.repository.PlaybackController
import javax.inject.Inject

class MoveQueueItemUseCase @Inject constructor(
    private val playbackController: PlaybackController,
) {
    suspend operator fun invoke(fromIndex: Int, toIndex: Int) {
        playbackController.moveQueueItem(fromIndex, toIndex)
    }
}
