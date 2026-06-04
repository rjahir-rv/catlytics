package com.catlytics.core.domain.usecase

import com.catlytics.core.domain.repository.PlaybackController

class SeekPlaybackUseCase(
    private val playbackController: PlaybackController,
) {
    suspend operator fun invoke(positionMillis: Long) {
        playbackController.seekTo(positionMillis)
    }
}
