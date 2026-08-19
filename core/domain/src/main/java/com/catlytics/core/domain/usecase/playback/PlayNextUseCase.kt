package com.catlytics.core.domain.usecase.playback

import com.catlytics.core.domain.repository.PlaybackController
import com.catlytics.core.model.Track

class PlayNextUseCase(
    private val playbackController: PlaybackController,
) {
    suspend operator fun invoke(track: Track) {
        playbackController.playNext(track)
    }

    suspend fun invokeAll(tracks: List<Track>, currentTrackId: String?): Int {
        if (currentTrackId == null) return 0
        val toAdd = tracks.filter { it.id != currentTrackId }
        toAdd.asReversed().forEach { playbackController.playNext(it) }
        return toAdd.size
    }
}
