package com.catlytics.core.domain.usecase.home

import com.catlytics.core.model.Artist
import com.catlytics.core.model.Track
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GenerateDailyPlaylistUseCaseTest {
    @Test
    fun `returns empty playlist with fewer than five tracks`() {
        val useCase = GenerateDailyPlaylistUseCase { LocalDate.of(2026, 7, 22) }

        assertTrue(useCase((1..4).map(::track)).isEmpty())
    }

    @Test
    fun `returns at most ten distinct tracks`() {
        val useCase = GenerateDailyPlaylistUseCase { LocalDate.of(2026, 7, 22) }
        val tracks = (1..12).map(::track) + track(1)

        val result = useCase(tracks)

        assertEquals(10, result.size)
        assertEquals(10, result.distinctBy(Track::id).size)
    }

    @Test
    fun `selection is stable for the same date and library regardless of input order`() {
        val useCase = GenerateDailyPlaylistUseCase { LocalDate.of(2026, 7, 22) }
        val tracks = (1..12).map(::track)

        assertEquals(useCase(tracks), useCase(tracks.reversed()))
    }

    @Test
    fun `selection changes with the local date`() {
        var date = LocalDate.of(2026, 7, 22)
        val useCase = GenerateDailyPlaylistUseCase { date }
        val tracks = (1..12).map(::track)
        val firstDay = useCase(tracks)

        date = date.plusDays(1)

        assertNotEquals(firstDay, useCase(tracks))
    }

    private fun track(index: Int) = Track(
        id = "track-$index",
        title = "Track $index",
        artist = Artist("artist-$index", "Artist $index"),
        durationMillis = 180_000L,
        mediaUri = "content://track/$index",
    )
}
