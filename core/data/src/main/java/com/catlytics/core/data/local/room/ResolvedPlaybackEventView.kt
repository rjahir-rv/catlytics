package com.catlytics.core.data.local.room

import androidx.room.ColumnInfo
import androidx.room.DatabaseView

@DatabaseView(viewName = RESOLVED_PLAYBACK_EVENTS_VIEW, value = RESOLVED_PLAYBACK_EVENTS_QUERY)
data class ResolvedPlaybackEventView(
    @ColumnInfo(name = "id") val id: Long,
    @ColumnInfo(name = "track_id") val trackId: String,
    @ColumnInfo(name = "track_title") val trackTitle: String,
    @ColumnInfo(name = "artist_id") val artistId: String,
    @ColumnInfo(name = "artist_name") val artistName: String,
    @ColumnInfo(name = "artist_key") val artistKey: String,
    @ColumnInfo(name = "album_id") val albumId: String?,
    @ColumnInfo(name = "album_title") val albumTitle: String?,
    @ColumnInfo(name = "artwork_uri") val artworkUri: String?,
    @ColumnInfo(name = "duration_listened_millis") val durationListenedMillis: Long,
    @ColumnInfo(name = "timestamp") val timestamp: Long,
)

internal const val RESOLVED_PLAYBACK_EVENTS_VIEW = "resolved_playback_events"

internal const val RESOLVED_PLAYBACK_EVENTS_QUERY =
    "SELECT e.id AS id, e.track_id AS track_id, " +
        "COALESCE(o.title, e.track_title) AS track_title, " +
        "COALESCE(o.artist_id, e.artist_id) AS artist_id, " +
        "COALESCE(o.artist_name, e.artist_name) AS artist_name, " +
        "COALESCE(o.artist_key, e.artist_key) AS artist_key, " +
        "COALESCE(o.album_id, e.album_id) AS album_id, " +
        "COALESCE(o.album_title, e.album_title) AS album_title, " +
        "COALESCE(o.artwork_uri, e.artwork_uri) AS artwork_uri, " +
        "e.duration_listened_millis AS duration_listened_millis, " +
        "e.timestamp AS timestamp " +
        "FROM playback_events e " +
        "LEFT JOIN track_metadata_overrides o ON o.track_id = e.track_id"
