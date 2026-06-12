package com.catlytics.core.data.model

data class ArtistEntity(
    val id: String,
    val name: String,
)

data class TrackEntity(
    val id: String,
    val title: String,
    val artistId: String,
    val artistName: String,
    val durationMillis: Long,
    val mediaUri: String,
    val artworkUri: String? = null,
    val albumId: String? = null,
    val albumTitle: String? = null,
    val trackNumber: Int? = null,
    val folderId: String? = null,
    val folderName: String? = null,
    val folderPath: String? = null,
)
