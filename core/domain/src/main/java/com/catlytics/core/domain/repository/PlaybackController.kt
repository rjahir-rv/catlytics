package com.catlytics.core.domain.repository

import com.catlytics.core.model.PlaybackState
import com.catlytics.core.model.Track
import kotlinx.coroutines.flow.Flow

interface PlaybackController {
    val playbackState: Flow<PlaybackState>

    suspend fun play(track: Track, queue: List<Track>, startIndex: Int)

    suspend fun togglePlayPause()

    suspend fun pause()

    suspend fun skipNext()

    suspend fun skipPrevious()

    suspend fun seekTo(positionMillis: Long)

    suspend fun stop()
}
