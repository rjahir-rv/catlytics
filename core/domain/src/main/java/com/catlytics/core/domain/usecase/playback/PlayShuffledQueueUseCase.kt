package com.catlytics.core.domain.usecase.playback

import com.catlytics.core.domain.repository.PlaybackController
import com.catlytics.core.model.PlaybackQueueSource
import com.catlytics.core.model.Track
import com.catlytics.core.model.reorderedForShuffle
import kotlin.random.Random

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

        val chosenStartTrack = startTrack?.takeIf { candidate ->
            playbackQueue.any { it.id == candidate.id }
        } ?: playbackQueue[random.nextInt(playbackQueue.size)]

        val reorderedQueue = playbackQueue.reorderedForShuffle(chosenStartTrack, random)

        playbackController.setShuffleEnabled(true)
        playbackController.play(
            track = chosenStartTrack,
            queue = reorderedQueue,
            startIndex = 0,
            queueSource = queueSource,
        )
    }
}
