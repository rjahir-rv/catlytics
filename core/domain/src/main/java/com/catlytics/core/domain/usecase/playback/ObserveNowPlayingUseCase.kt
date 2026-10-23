package com.catlytics.core.domain.usecase.playback

import com.catlytics.core.domain.repository.PlaybackController
import com.catlytics.core.model.NowPlaying
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class ObserveNowPlayingUseCase(
    private val playbackController: PlaybackController,
) {
    operator fun invoke(): Flow<NowPlaying> = playbackController.playbackState
        .map { state ->
            NowPlaying(
                trackId = state.currentTrack?.id,
                status = state.status,
                queueSource = state.queueSource,
            )
        }
        .distinctUntilChanged()
}
