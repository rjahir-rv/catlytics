package com.catlytics.core.data.local

import com.catlytics.core.data.model.TrackEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class InMemoryLocalDataSourceTest {
    @Test
    fun `replaceTracks replaces current tracks`() = runTest {
        val dataSource = InMemoryLocalDataSource()
        val firstTrack = trackEntity(id = "track-1")
        val secondTrack = trackEntity(id = "track-2")

        dataSource.upsertTracks(listOf(firstTrack))
        dataSource.replaceTracks(listOf(secondTrack))

        assertEquals(listOf(secondTrack), dataSource.observeTracks().first())
    }

    private fun trackEntity(id: String) = TrackEntity(
        id = id,
        title = "Title $id",
        artistId = "artist-$id",
        artistName = "Artist $id",
        durationMillis = 180_000L,
        mediaUri = "content://media/external/audio/media/$id",
    )
}
