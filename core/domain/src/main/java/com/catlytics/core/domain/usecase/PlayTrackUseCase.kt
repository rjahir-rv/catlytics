package com.catlytics.core.domain.usecase

import com.catlytics.core.domain.repository.PlaybackController
import com.catlytics.core.model.Track

class PlayTrackUseCase(
    private val playbackController: PlaybackController,
) {
    suspend operator fun invoke(track: Track, queue: List<Track>) {
        val startIndex = queue.indexOfFirst { it.id == track.id }.takeUnless { it < 0 } ?: 0
        playbackController.play(
            track = track,
            queue = queue.ifEmpty { listOf(track) },
            startIndex = startIndex,
        )
    }
}
