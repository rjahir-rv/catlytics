package com.catlytics.core.domain.usecase.playback

import com.catlytics.core.domain.repository.PlaybackController
import com.catlytics.core.model.PlaybackQueueSource
import com.catlytics.core.model.Track
import javax.inject.Inject

/**
 * Reproduce [Track] dentro de su cola en el orden original; si el modo aleatorio está activo, el
 * controlador la mezcla y conserva ese orden para cuando se desactive.
 */
class PlayTrackUseCase @Inject constructor(
    private val playbackController: PlaybackController,
) {
    suspend operator fun invoke(
        track: Track,
        queue: List<Track>,
        queueSource: PlaybackQueueSource = PlaybackQueueSource.Static,
    ) {
        val distinctQueue = queue.distinctBy(Track::id)
        val playbackQueue = distinctQueue.takeIf { tracks ->
            tracks.any { it.id == track.id }
        } ?: listOf(track)

        playbackController.play(
            track = track,
            queue = playbackQueue,
            startIndex = playbackQueue.indexOfFirst { it.id == track.id },
            queueSource = queueSource,
        )
    }
}
