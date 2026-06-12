package com.catlytics.core.domain.usecase.playback

import com.catlytics.core.domain.repository.PlaybackController

class SkipPlaybackUseCase(
    private val playbackController: PlaybackController,
) {
    suspend fun next() {
        playbackController.skipNext()
    }

    suspend fun previous() {
        playbackController.skipPrevious()
    }
}
