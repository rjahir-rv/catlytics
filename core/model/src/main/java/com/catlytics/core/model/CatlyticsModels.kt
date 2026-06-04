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
    val mediaUri: String,
)

data class PlaybackState(
    val status: PlaybackStatus = PlaybackStatus.Idle,
    val currentTrack: Track? = null,
    val queue: List<Track> = emptyList(),
    val currentIndex: Int = 0,
    val positionMillis: Long = 0L,
    val durationMillis: Long = 0L,
    val bufferedPositionMillis: Long = 0L,
)

enum class PlaybackStatus {
    Idle,
    Buffering,
    Playing,
    Paused,
    Ended,
    Error,
}

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
