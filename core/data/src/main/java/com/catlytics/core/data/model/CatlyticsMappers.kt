package com.catlytics.core.data.model

import com.catlytics.core.model.Artist
import com.catlytics.core.model.Track

internal fun TrackEntity.toDomain() = Track(
    id = id,
    title = title,
    artist = Artist(
        id = artistId,
        name = artistName,
    ),
    durationMillis = durationMillis,
    addedAtMillis = addedAtMillis,
    mediaUri = mediaUri,
    artworkUri = artworkUri,
    albumId = albumId,
    albumTitle = albumTitle,
)

