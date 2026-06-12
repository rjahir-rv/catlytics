package com.catlytics.core.domain.usecase.playback

import com.catlytics.core.domain.repository.PlaybackController
import com.catlytics.core.model.PlaybackRepeatMode
import com.catlytics.core.model.PlaybackState
import com.catlytics.core.model.Track
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class QueueUseCasesTest {
    private val playbackController = QueueFakePlaybackController()

    @Test
    fun `play queue item dispatches selected index`() = runTest {
        PlayQueueItemUseCase(playbackController)(index = 3)

        assertEquals(3, playbackController.playedIndex)
    }

    @Test
    fun `move queue item dispatches source and destination indices`() = runTest {
        MoveQueueItemUseCase(playbackController)(fromIndex = 1, toIndex = 4)

        assertEquals(1 to 4, playbackController.movedIndices)
    }
}

private class QueueFakePlaybackController : PlaybackController {
    override val playbackState: Flow<PlaybackState> = MutableStateFlow(PlaybackState())
    var playedIndex = -1
    var movedIndices = -1 to -1

    override suspend fun play(track: Track, queue: List<Track>, startIndex: Int) = Unit

    override suspend fun playQueueItem(index: Int) {
        playedIndex = index
    }

    override suspend fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        movedIndices = fromIndex to toIndex
    }

    override suspend fun togglePlayPause() = Unit
    override suspend fun pause() = Unit
    override suspend fun skipNext() = Unit
    override suspend fun skipPrevious() = Unit
    override suspend fun seekTo(positionMillis: Long) = Unit
    override suspend fun setShuffleEnabled(enabled: Boolean) = Unit
    override suspend fun setRepeatMode(mode: PlaybackRepeatMode) = Unit
    override suspend fun restoreLastSession() = Unit
    override suspend fun stop() = Unit
}
