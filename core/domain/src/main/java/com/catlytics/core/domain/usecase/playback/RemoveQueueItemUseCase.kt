package com.catlytics.core.domain.usecase.playback

import com.catlytics.core.domain.repository.PlaybackController
import javax.inject.Inject

class RemoveQueueItemUseCase @Inject constructor(
    private val playbackController: PlaybackController,
) {
    suspend operator fun invoke(index: Int) {
        playbackController.removeQueueItem(index)
    }
}
