package com.catlytics.core.domain.usecase.playback

import com.catlytics.core.domain.repository.PlaybackController
import javax.inject.Inject

class ObservePlaybackStateUseCase @Inject constructor(
    private val playbackController: PlaybackController,
) {
    operator fun invoke() = playbackController.playbackState
}
