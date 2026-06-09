package com.catlytics.feature.home.impl

import com.catlytics.core.model.Track

internal fun List<Track>.filterByQuery(query: String): List<Track> {
    val normalizedQuery = query.trim()
    if (normalizedQuery.isEmpty()) return this
    return filter { track ->
        track.title.contains(normalizedQuery, ignoreCase = true) ||
            track.artist.name.contains(normalizedQuery, ignoreCase = true)
    }
}
