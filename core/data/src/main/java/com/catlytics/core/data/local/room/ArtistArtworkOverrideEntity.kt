package com.catlytics.core.data.local.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

internal const val ARTIST_ARTWORK_OVERRIDES_TABLE = "artist_artwork_overrides"

@Entity(tableName = ARTIST_ARTWORK_OVERRIDES_TABLE)
data class ArtistArtworkOverrideEntity(
    @PrimaryKey
    @ColumnInfo(name = "artist_key") val artistKey: String,
    @ColumnInfo(name = "artist_name") val artistName: String,
    @ColumnInfo(name = "artwork_uri") val artworkUri: String,
    @ColumnInfo(name = "updated_at") val updatedAtMillis: Long,
)
