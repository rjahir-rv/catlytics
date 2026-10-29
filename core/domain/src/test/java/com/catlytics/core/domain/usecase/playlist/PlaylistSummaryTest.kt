package com.catlytics.core.domain.usecase.playlist

import com.catlytics.core.model.Artist
import com.catlytics.core.model.Playlist
import com.catlytics.core.model.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaylistSummaryTest {
    @Test
    fun `summary counts only tracks present in the library`() {
        val playlist = Playlist(id = "p", name = "Focus", trackIds = listOf("a", "gone", "b"))

        val summary = playlist.toSummary(tracksById(track("a", 1_000), track("b", 2_000)))

        assertEquals(2, summary.trackCount)
        assertEquals(3_000L, summary.totalDurationMillis)
    }

    @Test
    fun `summary of an empty playlist has no tracks duration or covers`() {
        val summary = Playlist(id = "p", name = "Empty", trackIds = emptyList())
            .toSummary(tracksById(track("a", 1_000)))

        assertEquals(0, summary.trackCount)
        assertEquals(0L, summary.totalDurationMillis)
        assertTrue(summary.coverArtworkUris.isEmpty())
    }

    @Test
    fun `summary keeps at most four artworks from different albums in playlist order`() {
        val tracks = listOf(
            track("1", albumId = "album-1", artworkUri = "art-1"),
            track("2", albumId = "album-1", artworkUri = "art-1"),
            track("3", albumId = "album-2", artworkUri = "art-2"),
            track("4", albumId = "album-3", artworkUri = "art-3"),
            track("5", albumId = "album-4", artworkUri = "art-4"),
            track("6", albumId = "album-5", artworkUri = "art-5"),
        )
        val playlist = Playlist(id = "p", name = "Mix", trackIds = tracks.map { it.id })

        val summary = playlist.toSummary(tracksById(*tracks.toTypedArray()))

        assertEquals(listOf("art-1", "art-2", "art-3", "art-4"), summary.coverArtworkUris)
        assertEquals(6, summary.trackCount)
    }

    @Test
    fun `summary skips tracks without artwork and dedupes by artwork when album is unknown`() {
        val tracks = listOf(
            track("1", artworkUri = null),
            track("2", artworkUri = "shared"),
            track("3", artworkUri = "shared"),
            track("4", artworkUri = "other"),
        )
        val playlist = Playlist(id = "p", name = "Loose", trackIds = tracks.map { it.id })

        val summary = playlist.toSummary(tracksById(*tracks.toTypedArray()))

        assertEquals(listOf("shared", "other"), summary.coverArtworkUris)
    }

    private fun tracksById(vararg tracks: Track): Map<String, Track> = tracks.associateBy { it.id }

    private fun track(
        id: String,
        durationMillis: Long = 1_000L,
        albumId: String? = null,
        artworkUri: String? = null,
    ) = Track(
        id = id,
        title = "Track $id",
        artist = Artist(id = "artist", name = "Artist"),
        durationMillis = durationMillis,
        mediaUri = "content://media/$id",
        artworkUri = artworkUri,
        albumId = albumId,
    )
}
