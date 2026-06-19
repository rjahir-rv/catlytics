package com.catlytics.feature.playlists.impl

import com.catlytics.core.model.Playlist
import com.catlytics.core.model.SortDirection

internal fun List<Playlist>.filterByQuery(query: String): List<Playlist> {
    val normalizedQuery = query.trim()
    if (normalizedQuery.isEmpty()) return this
    return filter { playlist ->
        playlist.name.contains(normalizedQuery, ignoreCase = true)
    }
}

internal fun List<Playlist>.sortedByDirection(direction: SortDirection): List<Playlist> {
    val selector = { p: Playlist -> p.name.lowercase() }
    return if (direction == SortDirection.Ascending) {
        sortedBy(selector)
    } else {
        sortedByDescending(selector)
    }
}
