package com.catlytics.core.model

import java.text.Normalizer
import java.util.Locale

data class Artist(
    val id: String,
    val name: String,
)

data class ArtistAlias(
    val source: Artist,
    val target: Artist,
)

fun artistIdentityKey(name: String): String = Normalizer
    .normalize(name.trim(), Normalizer.Form.NFKC)
    .lowercase(Locale.ROOT)
    .replace(Regex("\\s+"), " ")

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
    val description: String = "",
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
    data class TrackCollectionSource(
        val title: String,
        val artworkUri: String?,
        val trackIds: List<String>,
    ) : PlaylistSource
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

data class ListeningTotals(
    val trackCount: Int,
    val artistCount: Int,
    val albumCount: Int,
)

/** Unique entity counts within a time range. */
data class PeriodUniqueCounts(
    val trackCount: Int,
    val artistCount: Int,
    val albumCount: Int,
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

data class RecentlyPlayedTrack(
    val trackId: String,
    val title: String,
    val artistName: String,
    val artworkUri: String?,
    val lastListenedAtMillis: Long,
)

data class TopArtist(
    val artistId: String,
    val name: String,
    val artworkUri: String?,
    val playCount: Int,
    val totalListenedMillis: Long,
)

data class TopAlbum(
    val albumId: String,
    val title: String,
    val artistName: String,
    val artworkUri: String?,
    val playCount: Int,
    val totalListenedMillis: Long,
)

enum class StatsGranularity {
    WEEK,
    MONTH,
}

data class StatsPeriodRange(
    val granularity: StatsGranularity,
    val offset: Int,
    val startMillis: Long,
    val endMillis: Long,
    val label: String,
)

data class PeriodStats(
    val range: StatsPeriodRange,
    val totalListenedMillis: Long,
    val playCount: Int,
    val uniqueTracks: Int,
    val uniqueArtists: Int,
    val uniqueAlbums: Int,
    val dailyListening: List<DailyListeningStat>,
    val topTracks: List<TopTrack>,
    val topArtists: List<TopArtist>,
    val topAlbums: List<TopAlbum>,
) {
    val isEmpty: Boolean
        get() = totalListenedMillis == 0L && playCount == 0
}

data class ListeningStreak(
    val currentDays: Int,
    val lastActiveDayEpochDay: Long?,
)

/**
 * Spotify-style narrative summary for a listening period.
 * [eligible] is true when the period has at least one hour of listening.
 */
data class ListeningNarrative(
    val eligible: Boolean,
    val totalListenedMillis: Long,
    val topArtist: TopArtist?,
    val topTrack: TopTrack?,
    val headline: String,
    val supportingLines: List<String>,
)

data class WeeklyStats(
    val weekStart: Long,
    val weekEnd: Long,
    val topTracks: List<TopTrack>,
    val topArtists: List<TopArtist>,
    val totalListenedMillis: Long,
    val dailyListening: List<DailyListeningStat>,
    val playCount: Int,
)

/**
 * Listening total for a day bucket within a period.
 * [dayIndex] is 1-based: Mon=1..Sun=7 for weeks, day-of-month for months.
 */
data class DailyListeningStat(
    val dayIndex: Int,
    val totalListenedMillis: Long,
)

data class PlaybackEvent(
    val trackId: String,
    val trackTitle: String,
    val artistId: String,
    val artistName: String,
    val albumId: String?,
    val albumTitle: String?,
    val artworkUri: String?,
    val durationListenedMillis: Long,
    val trackDurationMillis: Long,
    val timestamp: Long,
)

/** Local summary of stored listening events for backup UI. */
data class StatisticsBackupSummary(
    val eventCount: Long,
    val firstEventMillis: Long?,
    val lastEventMillis: Long?,
    val artistAliasCount: Int = 0,
)

data class StatisticsBackupPreview(
    val schemaVersion: Int,
    val exportedAtMillis: Long,
    val eventCount: Int,
    val firstEventMillis: Long?,
    val lastEventMillis: Long?,
    val artistAliasCount: Int = 0,
)

data class StatisticsExportResult(
    val eventCount: Int,
    val artistAliasCount: Int = 0,
)

data class StatisticsImportResult(
    val importedCount: Int,
    val skippedDuplicateCount: Int,
    val totalInFile: Int,
    val importedArtistAliasCount: Int = 0,
)

enum class StatisticsImportMode {
    /** Keep existing events and only insert non-duplicates from the file. */
    Merge,
    /** Delete all local events, then insert the file contents. */
    Replace,
}
