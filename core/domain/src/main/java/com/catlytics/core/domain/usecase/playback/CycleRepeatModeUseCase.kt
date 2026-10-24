package com.catlytics.core.domain.usecase.playback

import com.catlytics.core.domain.repository.PlaybackController
import com.catlytics.core.model.PlaybackRepeatMode
import javax.inject.Inject

class CycleRepeatModeUseCase @Inject constructor(
    private val playbackController: PlaybackController,
) {
    suspend operator fun invoke(currentMode: PlaybackRepeatMode) {
        playbackController.setRepeatMode(currentMode.next())
    }
}

private fun PlaybackRepeatMode.next(): PlaybackRepeatMode = when (this) {
    PlaybackRepeatMode.Off -> PlaybackRepeatMode.One
    PlaybackRepeatMode.One -> PlaybackRepeatMode.All
    PlaybackRepeatMode.All -> PlaybackRepeatMode.Off
}
