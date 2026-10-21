package com.catlytics.feature.playlists.impl

import com.catlytics.core.model.LIKED_PLAYLIST_ID
import com.catlytics.core.model.PlaylistSummary
import com.catlytics.core.model.SortDirection
import java.text.Collator
import java.util.Locale

internal fun List<PlaylistSummary>.filterByQuery(
    query: String,
    likedPlaylistName: String,
): List<PlaylistSummary> {
    val normalizedQuery = query.trim()
    if (normalizedQuery.isEmpty()) return this
    return filter { summary ->
        val name = if (summary.playlist.id == LIKED_PLAYLIST_ID) likedPlaylistName else summary.playlist.name
        name.contains(normalizedQuery, ignoreCase = true)
    }
}

internal fun List<PlaylistSummary>.sortedByDirection(
    direction: SortDirection,
): List<PlaylistSummary> {
    val collator = Collator.getInstance(Locale.getDefault()).apply {
        strength = Collator.PRIMARY
    }
    val (liked, others) = partition { it.playlist.id == LIKED_PLAYLIST_ID }
    val comparator = Comparator<PlaylistSummary> { first, second ->
        collator.compare(first.playlist.name, second.playlist.name)
    }
    val sortedOthers = if (direction == SortDirection.Ascending) {
        others.sortedWith(comparator)
    } else {
        others.sortedWith(comparator.reversed())
    }
    return liked + sortedOthers
}
