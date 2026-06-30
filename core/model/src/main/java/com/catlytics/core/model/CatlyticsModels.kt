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

enum class PlaylistViewMode {
    List,
    Mosaic,
}

enum class SortDirection {
    Ascending,
    Descending,
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
    val albumId: String? = null,
    val albumTitle: String? = null,
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
    val queueSource: PlaybackQueueSource = PlaybackQueueSource.Static,
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

sealed interface PlaybackQueueSource {
    data object Static : PlaybackQueueSource

    data class Playlist(val playlistId: String) : PlaybackQueueSource
}

data class PlaybackSessionSnapshot(
    val queueTrackIds: List<String> = emptyList(),
    val currentTrackId: String? = null,
    val queueSource: PlaybackQueueSource = PlaybackQueueSource.Static,
    val currentIndex: Int = 0,
    val positionMillis: Long = 0L,
    val isShuffleEnabled: Boolean = false,
    val repeatMode: PlaybackRepeatMode = PlaybackRepeatMode.Off,
)

data class Playlist(
    val id: String,
    val name: String,
    val trackIds: List<String>,
    val artworkUri: String? = null,
)

const val LIKED_PLAYLIST_ID = "system_liked"
const val LIKED_PLAYLIST_NAME = "Tus me gusta"

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

data class PlaylistSourcePreview(
    val title: String,
    val subtitle: String? = null,
    val artworkUri: String? = null,
    val itemCount: Int = 0,
    val trackIds: List<String> = emptyList(),
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

data class EqualizerState(
    val enabled: Boolean = false,
    val mode: EqualizerMode = EqualizerMode.Preset,
    val selectedPresetName: String? = null,
    val presets: List<EqualizerPreset> = emptyList(),
    val bands: List<EqualizerBand> = emptyList(),
    val levelRange: EqualizerLevelRange? = null,
    val isAvailable: Boolean = false,
    val errorMessage: String? = null,
)

enum class EqualizerMode {
    Preset,
    Custom,
}

data class EqualizerPreset(
    val id: Short,
    val name: String,
)

data class EqualizerBand(
    val id: Short,
    val centerFrequencyHz: Int,
    val levelMilliBel: Int,
)

data class EqualizerLevelRange(
    val minMilliBel: Int,
    val maxMilliBel: Int,
)

data class TopTrack(
    val trackId: String,
    val title: String,
    val artistName: String,
    val artworkUri: String?,
    val playCount: Int,
    val totalListenedMillis: Long,
)

data class TopArtist(
    val artistId: String,
    val name: String,
    val artworkUri: String?,
    val playCount: Int,
    val totalListenedMillis: Long,
)

data class WeeklyStats(
    val weekStart: Long,
    val weekEnd: Long,
    val topTracks: List<TopTrack>,
    val topArtists: List<TopArtist>,
    val totalListenedMillis: Long,
)

data class PlaybackEvent(
    val trackId: String,
    val trackTitle: String,
    val artistId: String,
    val artistName: String,
    val artworkUri: String?,
    val durationListenedMillis: Long,
    val trackDurationMillis: Long,
    val timestamp: Long,
)


