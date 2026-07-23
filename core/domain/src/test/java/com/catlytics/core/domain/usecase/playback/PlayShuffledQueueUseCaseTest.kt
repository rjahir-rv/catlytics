package com.catlytics.core.domain.usecase.playback

import com.catlytics.core.domain.repository.PlaybackController
import com.catlytics.core.model.Artist
import com.catlytics.core.model.PlaybackQueueSource
import com.catlytics.core.model.PlaybackRepeatMode
import com.catlytics.core.model.PlaybackState
import com.catlytics.core.model.Track
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class PlayShuffledQueueUseCaseTest {
    private val controller = ShuffledQueueFakePlaybackController()
    private val useCase = PlayShuffledQueueUseCase(controller, Random(7))

    @Test
    fun `empty queue does not start playback`() = runTest {
        useCase(emptyList())

        assertNull(controller.playedTrack)
        assertEquals(null, controller.shuffleEnabled)
    }

    @Test
    fun `starts a valid queue item and enables shuffle`() = runTest {
        val tracks = listOf(track("one"), track("two"), track("three"))

        useCase(tracks + tracks.first())

        assertTrue(controller.playedTrack in tracks)
        assertEquals(tracks, controller.playedQueue)
        assertEquals(controller.playedTrack, tracks[controller.startIndex])
        assertEquals(true, controller.shuffleEnabled)
        assertEquals(PlaybackQueueSource.Static, controller.queueSource)
    }

    private fun track(id: String) = Track(
        id = id,
        title = id,
        artist = Artist("artist-$id", "Artist $id"),
        durationMillis = 180_000L,
        mediaUri = "content://track/$id",
    )
}

private class ShuffledQueueFakePlaybackController : PlaybackController {
    override val playbackState: Flow<PlaybackState> = flowOf(PlaybackState())
    var playedTrack: Track? = null
    var playedQueue: List<Track> = emptyList()
    var startIndex = -1
    var queueSource: PlaybackQueueSource? = null
    var shuffleEnabled: Boolean? = null

    override suspend fun play(
        track: Track,
        queue: List<Track>,
        startIndex: Int,
        queueSource: PlaybackQueueSource,
    ) {
        playedTrack = track
        playedQueue = queue
        this.startIndex = startIndex
        this.queueSource = queueSource
    }

    override suspend fun setShuffleEnabled(enabled: Boolean) {
        shuffleEnabled = enabled
    }

    override suspend fun playQueueItem(index: Int) = Unit
    override suspend fun addQueueItem(track: Track) = Unit
    override suspend fun moveQueueItem(fromIndex: Int, toIndex: Int) = Unit
    override suspend fun removeQueueItem(index: Int) = Unit
    override suspend fun togglePlayPause() = Unit
    override suspend fun pause() = Unit
    override suspend fun skipNext() = Unit
    override suspend fun skipPrevious() = Unit
    override suspend fun seekTo(positionMillis: Long) = Unit
    override suspend fun setRepeatMode(mode: PlaybackRepeatMode) = Unit
    override suspend fun restoreLastSession() = Unit
    override suspend fun stop() = Unit
}
