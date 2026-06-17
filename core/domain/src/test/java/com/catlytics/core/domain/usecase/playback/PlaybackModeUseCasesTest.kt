package com.catlytics.core.domain.usecase.playback

import com.catlytics.core.domain.repository.PlaybackController
import com.catlytics.core.model.PlaybackQueueSource
import com.catlytics.core.model.PlaybackRepeatMode
import com.catlytics.core.model.PlaybackState
import com.catlytics.core.model.Track
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackModeUseCasesTest {
    private val playbackController = PlaybackModeFakePlaybackController()

    @Test
    fun `toggle shuffle use case forwards requested mode`() = runTest {
        ToggleShuffleUseCase(playbackController)(enabled = true)

        assertEquals(true, playbackController.shuffleEnabled)
    }

    @Test
    fun `cycle repeat mode follows off one all off`() = runTest {
        val useCase = CycleRepeatModeUseCase(playbackController)

        useCase(PlaybackRepeatMode.Off)
        assertEquals(PlaybackRepeatMode.One, playbackController.repeatMode)

        useCase(PlaybackRepeatMode.One)
        assertEquals(PlaybackRepeatMode.All, playbackController.repeatMode)

        useCase(PlaybackRepeatMode.All)
        assertEquals(PlaybackRepeatMode.Off, playbackController.repeatMode)
    }
}

private class PlaybackModeFakePlaybackController : PlaybackController {
    override val playbackState: Flow<PlaybackState> = MutableStateFlow(PlaybackState())
    var shuffleEnabled: Boolean? = null
    var repeatMode: PlaybackRepeatMode? = null

    override suspend fun play(
        track: Track,
        queue: List<Track>,
        startIndex: Int,
        queueSource: PlaybackQueueSource,
    ) = Unit

    override suspend fun playQueueItem(index: Int) = Unit

    override suspend fun addQueueItem(track: Track) = Unit

    override suspend fun moveQueueItem(fromIndex: Int, toIndex: Int) = Unit

    override suspend fun removeQueueItem(index: Int) = Unit

    override suspend fun togglePlayPause() = Unit

    override suspend fun pause() = Unit

    override suspend fun skipNext() = Unit

    override suspend fun skipPrevious() = Unit

    override suspend fun seekTo(positionMillis: Long) = Unit

    override suspend fun setShuffleEnabled(enabled: Boolean) {
        shuffleEnabled = enabled
    }

    override suspend fun setRepeatMode(mode: PlaybackRepeatMode) {
        repeatMode = mode
    }

    override suspend fun restoreLastSession() = Unit

    override suspend fun stop() = Unit
}
