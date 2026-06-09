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
    val folderId: String? = null,
    val folderName: String? = null,
    val folderPath: String? = null,
)

data class PlaylistEntity(
    val id: String,
    val name: String,
    val trackIds: List<String>,
)
