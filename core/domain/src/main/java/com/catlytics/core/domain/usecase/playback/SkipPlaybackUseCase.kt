package com.catlytics.core.domain.usecase.playback

import com.catlytics.core.domain.repository.PlaybackController
import javax.inject.Inject

class SkipPlaybackUseCase @Inject constructor(
    private val playbackController: PlaybackController,
) {
    suspend fun next() {
        playbackController.skipNext()
    }

    suspend fun previous() {
        playbackController.skipPrevious()
    }
}
