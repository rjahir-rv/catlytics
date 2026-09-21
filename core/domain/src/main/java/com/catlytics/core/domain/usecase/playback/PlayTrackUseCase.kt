package com.catlytics.core.domain.usecase.playback

import com.catlytics.core.domain.repository.PlaybackController
import com.catlytics.core.model.PlaybackQueueSource
import com.catlytics.core.model.Track
import com.catlytics.core.model.reorderedForShuffle
import kotlinx.coroutines.flow.first
import kotlin.random.Random

class PlayTrackUseCase(
    private val playbackController: PlaybackController,
    private val random: Random = Random.Default,
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

        val isShuffleEnabled = playbackController.playbackState.first().isShuffleEnabled
        if (isShuffleEnabled && playbackQueue.size > 1) {
            val reorderedQueue = playbackQueue.reorderedForShuffle(track, random)
            playbackController.play(
                track = track,
                queue = reorderedQueue,
                startIndex = 0,
                queueSource = queueSource,
            )
        } else {
            val startIndex = playbackQueue.indexOfFirst { it.id == track.id }
            playbackController.play(
                track = track,
                queue = playbackQueue,
                startIndex = startIndex,
                queueSource = queueSource,
            )
        }
    }
}
