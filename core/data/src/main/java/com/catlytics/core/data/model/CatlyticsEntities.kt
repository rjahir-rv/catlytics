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
)

data class PlaylistEntity(
    val id: String,
    val name: String,
    val trackIds: List<String>,
)
