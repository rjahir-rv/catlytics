package com.catlytics.core.data.local.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "playback_events",
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["track_id", "timestamp"]),
        Index(value = ["artist_id", "timestamp"])
    ]
)
data class PlaybackEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "track_id") val trackId: String,
    @ColumnInfo(name = "track_title") val trackTitle: String,
    @ColumnInfo(name = "artist_id") val artistId: String,
    @ColumnInfo(name = "artist_name") val artistName: String,
    @ColumnInfo(name = "artwork_uri") val artworkUri: String?,
    @ColumnInfo(name = "duration_listened_millis") val durationListenedMillis: Long,
    @ColumnInfo(name = "track_duration_millis") val trackDurationMillis: Long,
    @ColumnInfo(name = "timestamp") val timestamp: Long
)
