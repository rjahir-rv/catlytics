package com.catlytics.core.model

data class Artist(
    val id: String,
    val name: String,
)

data class Track(
    val id: String,
    val title: String,
    val artist: Artist,
    val durationMillis: Long,
)

data class Playlist(
    val id: String,
    val name: String,
    val trackIds: List<String>,
)

data class ListeningStats(
    val totalTracks: Int,
    val totalPlaylists: Int,
    val totalDurationMillis: Long,
)
