package com.catlytics.feature.playlists.impl

import com.catlytics.core.model.LIKED_PLAYLIST_ID
import com.catlytics.core.model.LIKED_PLAYLIST_NAME
import com.catlytics.core.model.Playlist
import com.catlytics.core.model.PlaylistSummary
import com.catlytics.core.model.SortDirection
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaylistsSearchTest {
    private val liked = summary(LIKED_PLAYLIST_ID, LIKED_PLAYLIST_NAME, trackCount = 3, duration = 600_000)
    private val zen = summary("zen", "Zen")
    private val angel = summary("angel", "Ángel", trackCount = 2, duration = 300_000)
    private val bravo = summary("bravo", "bravo", trackCount = 5, duration = 900_000)

    @Test
    fun `blank query returns every playlist`() {
        val all = listOf(zen, angel)

        assertEquals(all, all.filterByQuery("   "))
    }

    @Test
    fun `query matches by name ignoring case`() {
        assertEquals(listOf(zen), listOf(zen, angel, bravo).filterByQuery("ZEN"))
    }

    @Test
    fun `ascending sort ignores accents and case`() {
        val sorted = listOf(zen, bravo, angel).sortedByDirection(SortDirection.Ascending)

        assertEquals(listOf("angel", "bravo", "zen"), sorted.map { it.playlist.id })
    }

    @Test
    fun `descending sort reverses the alphabetical order`() {
        val sorted = listOf(zen, bravo, angel).sortedByDirection(SortDirection.Descending)

        assertEquals(listOf("zen", "bravo", "angel"), sorted.map { it.playlist.id })
    }

    @Test
    fun `liked playlist stays first in both directions`() {
        val playlists = listOf(zen, angel, liked, bravo)

        assertEquals(
            LIKED_PLAYLIST_ID,
            playlists.sortedByDirection(SortDirection.Ascending).first().playlist.id,
        )
        assertEquals(
            LIKED_PLAYLIST_ID,
            playlists.sortedByDirection(SortDirection.Descending).first().playlist.id,
        )
    }

    private fun summary(
        id: String,
        name: String,
        trackCount: Int = 0,
        duration: Long = 0L,
    ) = PlaylistSummary(
        playlist = Playlist(id = id, name = name, trackIds = emptyList()),
        trackCount = trackCount,
        totalDurationMillis = duration,
    )
}
