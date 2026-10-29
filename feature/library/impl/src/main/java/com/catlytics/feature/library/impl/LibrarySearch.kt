package com.catlytics.feature.library.impl

import com.catlytics.core.model.Album
import com.catlytics.core.model.ArtistSummary
import com.catlytics.core.model.LibraryFolder
import com.catlytics.core.model.SortDirection

internal fun List<Album>.filterAlbumsByQuery(query: String): List<Album> {
    val normalizedQuery = query.trim()
    if (normalizedQuery.isEmpty()) return this
    return filter { album ->
        album.title.contains(normalizedQuery, ignoreCase = true) ||
            album.artist.name.contains(normalizedQuery, ignoreCase = true)
    }
}

internal fun List<ArtistSummary>.filterArtistsByQuery(query: String): List<ArtistSummary> {
    val normalizedQuery = query.trim()
    if (normalizedQuery.isEmpty()) return this
    return filter { summary ->
        summary.artist.name.contains(normalizedQuery, ignoreCase = true)
    }
}

internal fun List<LibraryFolder>.filterFoldersByQuery(query: String): List<LibraryFolder> {
    val normalizedQuery = query.trim()
    if (normalizedQuery.isEmpty()) return this
    return filter { folder ->
        folder.name.contains(normalizedQuery, ignoreCase = true) ||
            folder.path.contains(normalizedQuery, ignoreCase = true)
    }
}

internal fun List<Album>.sortedAlbumsByDirection(direction: SortDirection): List<Album> {
    val selector = { album: Album -> album.title.lowercase() }
    return if (direction == SortDirection.Ascending) {
        sortedBy(selector)
    } else {
        sortedByDescending(selector)
    }
}

internal fun List<ArtistSummary>.sortedArtistsByDirection(direction: SortDirection): List<ArtistSummary> {
    val selector = { summary: ArtistSummary -> summary.artist.name.lowercase() }
    return if (direction == SortDirection.Ascending) {
        sortedBy(selector)
    } else {
        sortedByDescending(selector)
    }
}

internal fun List<LibraryFolder>.sortedFoldersByDirection(direction: SortDirection): List<LibraryFolder> {
    val selector = { folder: LibraryFolder -> folder.name.lowercase() }
    return if (direction == SortDirection.Ascending) {
        sortedBy(selector)
    } else {
        sortedByDescending(selector)
    }
}
