package com.catlytics.core.domain.usecase.playback

import com.catlytics.core.domain.repository.PlaybackController

class RestorePlaybackSessionUseCase(
    private val playbackController: PlaybackController,
) {
    suspend operator fun invoke() {
        playbackController.restoreLastSession()
    }
}
