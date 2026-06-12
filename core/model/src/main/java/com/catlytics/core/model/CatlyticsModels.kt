package com.catlytics.core.model

data class Artist(
    val id: String,
    val name: String,
)

data class ArtistSummary(
    val artist: Artist,
    val artworkUri: String? = null,
    val albumCount: Int,
    val trackCount: Int,
)

data class ArtistContent(
    val summary: ArtistSummary,
    val albums: List<Album>,
    val tracks: List<Track>,
)

enum class ArtistViewMode {
    List,
    Grid,
}

data class Album(
    val id: String,
    val title: String,
    val artist: Artist,
    val artworkUri: String? = null,
    val trackCount: Int,
)

data class AlbumContent(
    val album: Album,
    val tracks: List<Track>,
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

data class PlaylistContent(
    val playlist: Playlist,
    val tracks: List<Track>,
)

sealed interface PlaylistSource {
    data class TrackSource(val trackId: String) : PlaylistSource
    data class AlbumSource(val albumId: String) : PlaylistSource
    data class ArtistSource(val artistId: String) : PlaylistSource
    data class FolderSource(val folderId: String) : PlaylistSource
}

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
