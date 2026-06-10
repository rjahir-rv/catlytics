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
    val artworkUri: String? = null,
)

data class LibraryFolder(
    val id: String,
    val name: String,
    val path: String,
    val trackCount: Int,
    val isVisible: Boolean,
)

data class LibraryFolderContent(
    val folder: LibraryFolder,
    val subfolders: List<LibraryFolder>,
    val tracks: List<Track>,
)

data class PlaybackState(
    val status: PlaybackStatus = PlaybackStatus.Idle,
    val currentTrack: Track? = null,
    val queue: List<Track> = emptyList(),
    val currentIndex: Int = 0,
    val positionMillis: Long = 0L,
    val durationMillis: Long = 0L,
    val bufferedPositionMillis: Long = 0L,
    val isShuffleEnabled: Boolean = false,
    val repeatMode: PlaybackRepeatMode = PlaybackRepeatMode.Off,
)

enum class PlaybackStatus {
    Idle,
    Buffering,
    Playing,
    Paused,
    Ended,
    Error,
}

enum class PlaybackRepeatMode {
    Off,
    One,
    All,
}

data class PlaybackSessionSnapshot(
    val queueTrackIds: List<String> = emptyList(),
    val currentTrackId: String? = null,
    val currentIndex: Int = 0,
    val positionMillis: Long = 0L,
    val isShuffleEnabled: Boolean = false,
    val repeatMode: PlaybackRepeatMode = PlaybackRepeatMode.Off,
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

enum class ThemeMode {
    System,
    Light,
    Dark,
}
