package com.catlytics.core.domain.usecase.playback

import com.catlytics.core.domain.repository.PlaybackController

class TogglePlaybackUseCase(
    private val playbackController: PlaybackController,
) {
    suspend operator fun invoke() {
        playbackController.togglePlayPause()
    }
}
