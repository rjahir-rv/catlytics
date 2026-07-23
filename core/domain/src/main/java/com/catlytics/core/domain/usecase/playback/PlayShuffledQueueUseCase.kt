package com.catlytics.core.domain.usecase.playback

import com.catlytics.core.domain.repository.PlaybackController
import com.catlytics.core.model.PlaybackQueueSource
import com.catlytics.core.model.Track
import kotlin.random.Random

class PlayShuffledQueueUseCase(
    private val playbackController: PlaybackController,
    private val random: Random = Random.Default,
) {
    suspend operator fun invoke(
        queue: List<Track>,
        queueSource: PlaybackQueueSource = PlaybackQueueSource.Static,
    ) {
        val playbackQueue = queue.distinctBy(Track::id)
        if (playbackQueue.isEmpty()) return

        val startIndex = random.nextInt(playbackQueue.size)
        playbackController.play(
            track = playbackQueue[startIndex],
            queue = playbackQueue,
            startIndex = startIndex,
            queueSource = queueSource,
        )
        playbackController.setShuffleEnabled(true)
    }
}
