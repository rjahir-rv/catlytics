package com.catlytics.core.domain.usecase

import com.catlytics.core.domain.repository.PlaybackController
import com.catlytics.core.model.PlaybackRepeatMode

class CycleRepeatModeUseCase(
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
