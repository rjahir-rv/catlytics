package com.catlytics.core.domain.usecase.playback

import com.catlytics.core.domain.repository.PlaybackController
import com.catlytics.core.model.Track

class AddQueueItemUseCase(
    private val playbackController: PlaybackController,
) {
    suspend operator fun invoke(track: Track) {
        playbackController.addQueueItem(track)
    }

    suspend fun invokeAll(tracks: List<Track>, currentTrackId: String?): Int {
        if (currentTrackId == null) return 0
        val toAdd = tracks.filter { it.id != currentTrackId }
        toAdd.forEach { playbackController.addQueueItem(it) }
        return toAdd.size
    }
}
