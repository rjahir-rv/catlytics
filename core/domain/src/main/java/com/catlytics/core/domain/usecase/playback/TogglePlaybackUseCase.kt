package com.catlytics.core.domain.usecase.playback

import com.catlytics.core.domain.repository.PlaybackController
import javax.inject.Inject

class TogglePlaybackUseCase @Inject constructor(
    private val playbackController: PlaybackController,
) {
    suspend operator fun invoke() {
        playbackController.togglePlayPause()
    }
}
