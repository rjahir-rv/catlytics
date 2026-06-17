package com.catlytics.core.domain.usecase.playback

import com.catlytics.core.domain.repository.PlaybackController
import com.catlytics.core.model.Track

class AddQueueItemUseCase(
    private val playbackController: PlaybackController,
) {
    suspend operator fun invoke(track: Track) {
        playbackController.addQueueItem(track)
    }
}
