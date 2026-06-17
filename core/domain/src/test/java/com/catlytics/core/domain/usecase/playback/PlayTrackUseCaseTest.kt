package com.catlytics.core.domain.usecase.playback

import com.catlytics.core.domain.repository.PlaybackController
import com.catlytics.core.model.Artist
import com.catlytics.core.model.PlaybackQueueSource
import com.catlytics.core.model.PlaybackRepeatMode
import com.catlytics.core.model.PlaybackState
import com.catlytics.core.model.Track
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class PlayTrackUseCaseTest {
    private val playbackController = FakePlaybackController()
    private val useCase = PlayTrackUseCase(playbackController)

    @Test
    fun `invoke starts playback at selected track index`() = runTest {
        val queue = listOf(track("track-1"), track("track-2"), track("track-3"))

        useCase(track = queue[1], queue = queue)

        assertEquals(queue[1], playbackController.playedTrack)
        assertEquals(queue, playbackController.playedQueue)
        assertEquals(1, playbackController.startIndex)
    }

    @Test
    fun `invoke falls back to selected track when it is absent from queue`() = runTest {
        val queue = listOf(track("track-1"), track("track-2"))
        val selectedTrack = track("missing")

        useCase(track = selectedTrack, queue = queue)

        assertEquals(selectedTrack, playbackController.playedTrack)
        assertEquals(listOf(selectedTrack), playbackController.playedQueue)
        assertEquals(0, playbackController.startIndex)
    }

    @Test
    fun `invoke removes duplicate tracks while preserving order`() = runTest {
        val first = track("track-1")
        val selected = track("track-2")
        val third = track("track-3")

        useCase(track = selected, queue = listOf(first, selected, first, third, selected))

        assertEquals(listOf(first, selected, third), playbackController.playedQueue)
        assertEquals(1, playbackController.startIndex)
    }

    @Test
    fun `invoke forwards queue source`() = runTest {
        val queue = listOf(track("track-1"), track("track-2"))
        val source = PlaybackQueueSource.Playlist("playlist-1")

        useCase(track = queue[0], queue = queue, queueSource = source)

        assertEquals(source, playbackController.queueSource)
    }

    private fun track(id: String) = Track(
        id = id,
        title = "Track $id",
        artist = Artist(
            id = "artist-$id",
            name = "Artist $id",
        ),
        durationMillis = 180_000L,
        mediaUri = "content://media/external/audio/media/$id",
    )
}

private class FakePlaybackController : PlaybackController {
    override val playbackState: Flow<PlaybackState> = MutableStateFlow(PlaybackState())

    lateinit var playedTrack: Track
    lateinit var playedQueue: List<Track>
    var queueSource: PlaybackQueueSource = PlaybackQueueSource.Static
    var startIndex: Int = -1

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

    override suspend fun playQueueItem(index: Int) = Unit

    override suspend fun addQueueItem(track: Track) = Unit

    override suspend fun moveQueueItem(fromIndex: Int, toIndex: Int) = Unit

    override suspend fun removeQueueItem(index: Int) = Unit

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
