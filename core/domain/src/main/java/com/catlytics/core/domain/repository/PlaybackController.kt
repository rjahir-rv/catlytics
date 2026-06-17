package com.catlytics.core.domain.repository

import com.catlytics.core.model.PlaybackState
import com.catlytics.core.model.PlaybackRepeatMode
import com.catlytics.core.model.PlaybackQueueSource
import com.catlytics.core.model.Track
import kotlinx.coroutines.flow.Flow

interface PlaybackController {
    val playbackState: Flow<PlaybackState>

    suspend fun play(
        track: Track,
        queue: List<Track>,
        startIndex: Int,
        queueSource: PlaybackQueueSource = PlaybackQueueSource.Static,
    )

    suspend fun playQueueItem(index: Int)

    suspend fun addQueueItem(track: Track)

    suspend fun moveQueueItem(fromIndex: Int, toIndex: Int)

    suspend fun removeQueueItem(index: Int)

    suspend fun togglePlayPause()

    suspend fun pause()

    suspend fun skipNext()

    suspend fun skipPrevious()

    suspend fun seekTo(positionMillis: Long)

    suspend fun setShuffleEnabled(enabled: Boolean)

    suspend fun setRepeatMode(mode: PlaybackRepeatMode)

    suspend fun restoreLastSession()

    suspend fun stop()
}
