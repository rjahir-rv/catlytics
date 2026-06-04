package com.catlytics.core.data.model

import com.catlytics.core.model.Artist
import com.catlytics.core.model.Playlist
import com.catlytics.core.model.Track

internal fun TrackEntity.toDomain() = Track(
    id = id,
    title = title,
    artist = Artist(
        id = artistId,
        name = artistName,
    ),
    durationMillis = durationMillis,
    mediaUri = mediaUri,
    artworkUri = artworkUri,
)

internal fun Track.toEntity() = TrackEntity(
    id = id,
    title = title,
    artistId = artist.id,
    artistName = artist.name,
    durationMillis = durationMillis,
    mediaUri = mediaUri,
    artworkUri = artworkUri,
)

internal fun PlaylistEntity.toDomain() = Playlist(
    id = id,
    name = name,
    trackIds = trackIds,
)

internal fun Playlist.toEntity() = PlaylistEntity(
    id = id,
    name = name,
    trackIds = trackIds,
)
