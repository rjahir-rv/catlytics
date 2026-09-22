package com.catlytics.core.data.model

import com.catlytics.core.model.Artist
import com.catlytics.core.model.Track
import org.junit.Assert.assertEquals
import org.junit.Test

class CatlyticsMappersTest {
    @Test
    fun `toDomain preserves track artwork uri`() {
        val entity = TrackEntity(
            id = "track-42",
            title = "Local Song",
            artistId = "artist-7",
            artistName = "Local Artist",
            durationMillis = 180_000L,
            addedAtMillis = 1_750_000_000_000L,
            mediaUri = "content://media/external/audio/media/42",
            artworkUri = "content://media/external/audio/albumart/9",
            albumId = "album-9",
            albumTitle = "Local Album",
        )

        val track = entity.toDomain()

        assertEquals(1_750_000_000_000L, track.addedAtMillis)
        assertEquals("content://media/external/audio/albumart/9", track.artworkUri)
        assertEquals("album-9", track.albumId)
        assertEquals("Local Album", track.albumTitle)
    }

    @Test
    fun `toEntity preserves track artwork uri`() {
        val track = Track(
            id = "track-42",
            title = "Local Song",
            artist = Artist(
                id = "artist-7",
                name = "Local Artist",
            ),
            durationMillis = 180_000L,
            addedAtMillis = 1_750_000_000_000L,
            mediaUri = "content://media/external/audio/media/42",
            artworkUri = "content://media/external/audio/albumart/9",
            albumId = "album-9",
            albumTitle = "Local Album",
        )

        val entity = track.toEntity()

        assertEquals(1_750_000_000_000L, entity.addedAtMillis)
        assertEquals("content://media/external/audio/albumart/9", entity.artworkUri)
        assertEquals("album-9", entity.albumId)
        assertEquals("Local Album", entity.albumTitle)
    }
}
