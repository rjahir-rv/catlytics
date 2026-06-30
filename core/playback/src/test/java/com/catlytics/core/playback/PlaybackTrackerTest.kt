package com.catlytics.core.playback

import com.catlytics.core.domain.repository.PlaybackEventRepository
import com.catlytics.core.model.Artist
import com.catlytics.core.model.PlaybackEvent
import com.catlytics.core.model.TopArtist
import com.catlytics.core.model.TopTrack
import com.catlytics.core.model.Track
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowSystemClock
import java.time.Duration

class FakePlaybackEventRepository : PlaybackEventRepository {
    val recordedEvents = mutableListOf<PlaybackEvent>()

    override suspend fun recordEvent(event: PlaybackEvent) {
        recordedEvents.add(event)
    }

    override fun observeTopTracks(startMillis: Long, endMillis: Long, limit: Int): Flow<List<TopTrack>> = emptyFlow()
    override fun observeTopArtists(startMillis: Long, endMillis: Long, limit: Int): Flow<List<TopArtist>> = emptyFlow()
    override fun observeTotalListeningTime(startMillis: Long, endMillis: Long): Flow<Long> = emptyFlow()
    override suspend fun cleanOldEvents(beforeMillis: Long): Int = 0
}

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class PlaybackTrackerTest {

    private val repository = FakePlaybackEventRepository()
    private val tracker = PlaybackTracker(repository)

    private val artist = Artist("artist-1", "Artist One")
    private val track = Track(
        id = "track-1",
        title = "Track One",
        artist = artist,
        durationMillis = 180_000L,
        mediaUri = "uri",
        artworkUri = null,
        albumId = null,
        albumTitle = null
    )

    @Test
    fun `does not record event if listened time is under 30 seconds`() = runTest {
        tracker.trackerScope = this
        tracker.onPlayingChanged(true, track)
        ShadowSystemClock.advanceBy(Duration.ofSeconds(20))
        tracker.onPlayingChanged(false, track)
        tracker.onSessionEnd()

        runCurrent()

        assertEquals(0, repository.recordedEvents.size)
    }

    @Test
    fun `records event if listened time is at least 30 seconds`() = runTest {
        tracker.trackerScope = this
        tracker.onPlayingChanged(true, track)
        ShadowSystemClock.advanceBy(Duration.ofSeconds(35))
        tracker.onPlayingChanged(false, track)
        tracker.onSessionEnd()

        runCurrent()

        assertEquals(1, repository.recordedEvents.size)
        val event = repository.recordedEvents[0]
        assertEquals("track-1", event.trackId)
        assertEquals(35000L, event.durationListenedMillis)
    }

    @Test
    fun `accumulates duration across multiple plays and pauses`() = runTest {
        tracker.trackerScope = this
        // First play session: 15s
        tracker.onPlayingChanged(true, track)
        ShadowSystemClock.advanceBy(Duration.ofSeconds(15))
        tracker.onPlayingChanged(false, track)

        // Second play session: 20s
        tracker.onPlayingChanged(true, track)
        ShadowSystemClock.advanceBy(Duration.ofSeconds(20))
        tracker.onPlayingChanged(false, track)

        // End session
        tracker.onSessionEnd()

        runCurrent()

        // Total should be 15 + 20 = 35s, which is >= 30s threshold
        assertEquals(1, repository.recordedEvents.size)
        val event = repository.recordedEvents[0]
        assertEquals(35000L, event.durationListenedMillis)
    }

    @Test
    fun `flushes and tracks separately on track transition`() = runTest {
        tracker.trackerScope = this
        val nextTrack = Track(
            id = "track-2",
            title = "Track Two",
            artist = artist,
            durationMillis = 200_000L,
            mediaUri = "uri2",
            artworkUri = null,
            albumId = null,
            albumTitle = null
        )

        // Play track-1 for 35s
        tracker.onPlayingChanged(true, track)
        ShadowSystemClock.advanceBy(Duration.ofSeconds(35))

        // Transition to track-2 and play for 10s
        tracker.onTrackTransition(nextTrack)
        ShadowSystemClock.advanceBy(Duration.ofSeconds(10))

        // End session
        tracker.onSessionEnd()

        runCurrent()

        // Only track-1 should be recorded (35s >= 30s), track-2 should be skipped (10s < 30s)
        assertEquals(1, repository.recordedEvents.size)
        assertEquals("track-1", repository.recordedEvents[0].trackId)
    }
}
