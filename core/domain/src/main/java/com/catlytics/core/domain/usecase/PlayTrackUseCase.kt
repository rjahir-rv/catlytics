package com.catlytics.core.domain.usecase

import com.catlytics.core.domain.repository.PlaybackController
import com.catlytics.core.model.Track

class PlayTrackUseCase(
    private val playbackController: PlaybackController,
) {
    suspend operator fun invoke(track: Track, queue: List<Track>) {
        val distinctQueue = queue.distinctBy(Track::id)
        val playbackQueue = distinctQueue.takeIf { tracks ->
            tracks.any { it.id == track.id }
        } ?: listOf(track)
        val startIndex = playbackQueue.indexOfFirst { it.id == track.id }
        playbackController.play(
            track = track,
            queue = playbackQueue,
            startIndex = startIndex,
        )
    }
}
