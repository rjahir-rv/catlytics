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

class QueueUseCasesTest {
    private val playbackController = QueueFakePlaybackController()

    @Test
    fun `play queue item dispatches selected index`() = runTest {
        PlayQueueItemUseCase(playbackController)(index = 3)

        assertEquals(3, playbackController.playedIndex)
    }

    @Test
    fun `add queue item dispatches selected track`() = runTest {
        val track = Track(
            id = "track-1",
            title = "Track 1",
            artist = com.catlytics.core.model.Artist(
                id = "artist-1",
                name = "Artist 1",
            ),
            durationMillis = 180_000L,
            mediaUri = "content://track-1",
        )

        AddQueueItemUseCase(playbackController)(track)

        assertEquals(track, playbackController.addedTrack)
    }

    @Test
    fun `play next dispatches selected track`() = runTest {
        val track = Track(
            id = "track-2",
            title = "Track 2",
            artist = com.catlytics.core.model.Artist(
                id = "artist-1",
                name = "Artist 1",
            ),
            durationMillis = 180_000L,
            mediaUri = "content://track-2",
        )

        PlayNextUseCase(playbackController)(track)

        assertEquals(track, playbackController.playNextTrack)
    }

    @Test
    fun `add queue items skips the current track and keeps visible order`() = runTest {
        val first = track("one")
        val current = track("current")
        val second = track("two")

        val added = AddQueueItemUseCase(playbackController).invokeAll(
            tracks = listOf(first, current, second),
            currentTrackId = current.id,
        )

        assertEquals(2, added)
        assertEquals(listOf(first, second), playbackController.addedTracks)
    }

    @Test
    fun `play next items insert in visible order by reversing controller calls`() = runTest {
        val first = track("one")
        val current = track("current")
        val second = track("two")

        val added = PlayNextUseCase(playbackController).invokeAll(
            tracks = listOf(first, current, second),
            currentTrackId = current.id,
        )

        assertEquals(2, added)
        assertEquals(listOf(second, first), playbackController.playNextTracks)
    }

    @Test
    fun `queue batch actions do nothing when nothing is playing`() = runTest {
        assertEquals(
            0,
            AddQueueItemUseCase(playbackController).invokeAll(listOf(track("one")), null),
        )
        assertEquals(
            0,
            PlayNextUseCase(playbackController).invokeAll(listOf(track("one")), null),
        )
    }

    @Test
    fun `move queue item dispatches source and destination indices`() = runTest {
        MoveQueueItemUseCase(playbackController)(fromIndex = 1, toIndex = 4)

        assertEquals(1 to 4, playbackController.movedIndices)
    }

    @Test
    fun `remove queue item dispatches selected index`() = runTest {
        RemoveQueueItemUseCase(playbackController)(index = 2)

        assertEquals(2, playbackController.removedIndex)
    }
}

private class QueueFakePlaybackController : PlaybackController {
    override val playbackState: Flow<PlaybackState> = MutableStateFlow(PlaybackState())
    var playedIndex = -1
    var movedIndices = -1 to -1
    var removedIndex = -1
    var addedTrack: Track? = null
    var playNextTrack: Track? = null
    val addedTracks = mutableListOf<Track>()
    val playNextTracks = mutableListOf<Track>()

    override suspend fun play(
        track: Track,
        queue: List<Track>,
        startIndex: Int,
        queueSource: PlaybackQueueSource,
    ) = Unit

    override suspend fun playQueueItem(index: Int) {
        playedIndex = index
    }

    override suspend fun addQueueItem(track: Track) {
        addedTrack = track
        addedTracks += track
    }

    override suspend fun playNext(track: Track) {
        playNextTrack = track
        playNextTracks += track
    }

    override suspend fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        movedIndices = fromIndex to toIndex
    }

    override suspend fun removeQueueItem(index: Int) {
        removedIndex = index
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

private fun track(id: String) = Track(
    id = id,
    title = id,
    artist = com.catlytics.core.model.Artist(id = "artist-1", name = "Artist 1"),
    durationMillis = 180_000L,
    mediaUri = "content://$id",
)
