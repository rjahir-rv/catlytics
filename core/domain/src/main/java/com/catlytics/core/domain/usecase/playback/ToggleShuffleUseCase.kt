package com.catlytics.core.domain.usecase.playback

import com.catlytics.core.domain.repository.PlaybackController

class ToggleShuffleUseCase(
    private val playbackController: PlaybackController,
) {
    suspend operator fun invoke(enabled: Boolean) {
        playbackController.setShuffleEnabled(enabled)
    }
}
