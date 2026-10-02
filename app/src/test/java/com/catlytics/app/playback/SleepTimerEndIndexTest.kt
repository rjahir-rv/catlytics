package com.catlytics.app.playback

import com.catlytics.core.model.Artist
import com.catlytics.core.model.PlaybackRepeatMode
import com.catlytics.core.model.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SleepTimerEndIndexTest {
    // Tres pistas de 3 minutos.
    private val queue = listOf(track("a"), track("b"), track("c"))

    @Test
    fun endsWithinCurrentTrack() {
        assertEquals(0, endIndex(positionMillis = 60_000L, remainingMillis = 60_000L))
    }

    @Test
    fun endsSeveralTracksAhead() {
        // Quedan 2 min de la actual + 3 min de "b" + 1 min de "c".
        assertEquals(2, endIndex(positionMillis = 60_000L, remainingMillis = 360_000L))
    }

    @Test
    fun returnsNullWhenTimerOutlastsQueue() {
        assertNull(endIndex(positionMillis = 0L, remainingMillis = 600_000L))
    }

    @Test
    fun repeatOneAlwaysEndsOnCurrentTrack() {
        assertEquals(
            1,
            sleepTimerEndIndex(queue, currentIndex = 1, positionMillis = 0L, remainingMillis = 3_600_000L, PlaybackRepeatMode.One),
        )
    }

    @Test
    fun repeatAllWrapsAroundQueue() {
        // Desde el inicio de "c": 3 min de "c" + 3 min de "a" + 1 min de "b".
        assertEquals(
            1,
            sleepTimerEndIndex(queue, currentIndex = 2, positionMillis = 0L, remainingMillis = 420_000L, PlaybackRepeatMode.All),
        )
    }

    @Test
    fun zeroDurationTracksAreSkipped() {
        val withEmpty = listOf(track("a"), track("empty", durationMillis = 0L), track("c"))
        assertEquals(
            2,
            sleepTimerEndIndex(withEmpty, currentIndex = 0, positionMillis = 0L, remainingMillis = 240_000L),
        )
    }

    @Test
    fun inactiveOrInvalidReturnsNull() {
        assertNull(endIndex(positionMillis = 0L, remainingMillis = 0L))
        assertNull(sleepTimerEndIndex(queue, currentIndex = -1, positionMillis = 0L, remainingMillis = 60_000L))
    }

    private fun endIndex(positionMillis: Long, remainingMillis: Long) =
        sleepTimerEndIndex(queue, currentIndex = 0, positionMillis = positionMillis, remainingMillis = remainingMillis)

    private fun track(id: String, durationMillis: Long = 180_000L) = Track(
        id = id,
        title = "Canción $id",
        artist = Artist(id = "artist", name = "Artista"),
        durationMillis = durationMillis,
        mediaUri = "content://media/$id",
    )
}
