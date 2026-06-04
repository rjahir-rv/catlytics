package com.catlytics.core.domain.usecase

import com.catlytics.core.domain.repository.PlaybackController

class ObservePlaybackStateUseCase(
    private val playbackController: PlaybackController,
) {
    operator fun invoke() = playbackController.playbackState
}
