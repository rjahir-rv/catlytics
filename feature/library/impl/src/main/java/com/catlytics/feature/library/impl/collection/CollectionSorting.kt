package com.catlytics.feature.library.impl.collection

import com.catlytics.core.model.Album
import com.catlytics.core.model.AlbumTrackSort
import com.catlytics.core.model.ArtistAlbumSort
import com.catlytics.core.model.ArtistTrackSort
import com.catlytics.core.model.Track

private val byTitle = compareBy<Track, String>(String.CASE_INSENSITIVE_ORDER) { it.title }

/** Album tracks come from the library already in track-number order. */
internal fun List<Track>.sortedFor(sort: AlbumTrackSort, playCounts: Map<String, Int>): List<Track> =
    when (sort) {
        AlbumTrackSort.TrackNumber -> this
        AlbumTrackSort.Title -> sortedWith(byTitle)
        AlbumTrackSort.Duration -> sortedWith(compareBy<Track> { it.durationMillis }.then(byTitle))
        AlbumTrackSort.MostPlayed -> sortedByPlays(playCounts)
    }

/** Artist tracks come from the library grouped by album, in track-number order. */
internal fun List<Track>.sortedFor(sort: ArtistTrackSort, playCounts: Map<String, Int>): List<Track> =
    when (sort) {
        ArtistTrackSort.Album -> this
        ArtistTrackSort.Title -> sortedWith(byTitle)
        ArtistTrackSort.Duration -> sortedWith(compareBy<Track> { it.durationMillis }.then(byTitle))
        ArtistTrackSort.MostPlayed -> sortedByPlays(playCounts)
    }

internal fun List<Album>.sortedFor(sort: ArtistAlbumSort): List<Album> = when (sort) {
    ArtistAlbumSort.Title -> sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.title })
    ArtistAlbumSort.TrackCount -> sortedWith(
        compareByDescending<Album> { it.trackCount }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.title },
    )
}

private fun List<Track>.sortedByPlays(playCounts: Map<String, Int>): List<Track> =
    sortedWith(compareByDescending<Track> { playCounts[it.id] ?: 0 }.then(byTitle))

internal val AlbumTrackSort.needsPlayCounts: Boolean get() = this == AlbumTrackSort.MostPlayed

internal val ArtistTrackSort.needsPlayCounts: Boolean get() = this == ArtistTrackSort.MostPlayed
