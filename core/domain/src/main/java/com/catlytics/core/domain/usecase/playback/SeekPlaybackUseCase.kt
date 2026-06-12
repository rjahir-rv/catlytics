package com.catlytics.core.domain.usecase.playback

import com.catlytics.core.domain.repository.PlaybackController

class SeekPlaybackUseCase(
    private val playbackController: PlaybackController,
) {
    suspend operator fun invoke(positionMillis: Long) {
        playbackController.seekTo(positionMillis)
    }
}
