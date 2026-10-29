package com.catlytics.core.domain.usecase.playback

import com.catlytics.core.domain.repository.PlaybackController
import com.catlytics.core.model.PlaybackQueueSource
import com.catlytics.core.model.Track
import kotlin.random.Random

/**
 * Activa el modo aleatorio y reproduce [queue] desde [startTrack] o una canción al azar. La cola se
 * pasa en su orden original: la mezcla el controlador, que lo conserva para cuando se desactive.
 */
class PlayShuffledQueueUseCase(
    private val playbackController: PlaybackController,
    private val random: Random = Random.Default,
) {
    suspend operator fun invoke(
        queue: List<Track>,
        queueSource: PlaybackQueueSource = PlaybackQueueSource.Static,
        startTrack: Track? = null,
    ) {
        val playbackQueue = queue.distinctBy(Track::id)
        if (playbackQueue.isEmpty()) return

        val startIndex = startTrack
            ?.let { candidate -> playbackQueue.indexOfFirst { it.id == candidate.id } }
            ?.takeUnless { it < 0 }
            ?: random.nextInt(playbackQueue.size)

        playbackController.setShuffleEnabled(true)
        playbackController.play(
            track = playbackQueue[startIndex],
            queue = playbackQueue,
            startIndex = startIndex,
            queueSource = queueSource,
        )
    }
}
