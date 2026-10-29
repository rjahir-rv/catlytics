package com.catlytics.core.data.local.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "artist_aliases",
    indices = [Index(value = ["target_key"])],
)
data class ArtistAliasEntity(
    @PrimaryKey
    @ColumnInfo(name = "source_key") val sourceKey: String,
    @ColumnInfo(name = "source_artist_id") val sourceArtistId: String,
    @ColumnInfo(name = "source_artist_name") val sourceArtistName: String,
    @ColumnInfo(name = "target_key") val targetKey: String,
    @ColumnInfo(name = "target_artist_id") val targetArtistId: String,
    @ColumnInfo(name = "target_artist_name") val targetArtistName: String,
)
