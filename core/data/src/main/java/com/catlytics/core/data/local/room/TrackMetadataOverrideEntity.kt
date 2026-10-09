package com.catlytics.core.data.local.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "track_metadata_overrides",
    indices = [Index(value = ["file_key"], unique = true)],
)
data class TrackMetadataOverrideEntity(
    @PrimaryKey
    @ColumnInfo(name = "track_id") val trackId: String,
    @ColumnInfo(name = "file_key") val fileKey: String?,
    @ColumnInfo(name = "title") val title: String?,
    @ColumnInfo(name = "artist_name") val artistName: String?,
    @ColumnInfo(name = "artist_key") val artistKey: String?,
    @ColumnInfo(name = "artist_id") val artistId: String?,
    @ColumnInfo(name = "album_title") val albumTitle: String?,
    @ColumnInfo(name = "album_id") val albumId: String?,
    @ColumnInfo(name = "artwork_uri") val artworkUri: String?,
    @ColumnInfo(name = "original_title") val originalTitle: String,
    @ColumnInfo(name = "original_artist_name") val originalArtistName: String,
    @ColumnInfo(name = "original_album_title") val originalAlbumTitle: String?,
    @ColumnInfo(name = "duration_millis") val durationMillis: Long,
    @ColumnInfo(name = "updated_at") val updatedAtMillis: Long,
)
