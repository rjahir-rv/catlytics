@file:OptIn(InternalSerializationApi::class)

package com.catlytics.core.data.repository

import com.catlytics.core.model.Artist
import com.catlytics.core.model.ArtistAlias
import com.catlytics.core.model.LIKED_PLAYLIST_ID
import com.catlytics.core.model.PlaybackEvent
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable


@Serializable
internal data class BackupDocument(
    val format: String,
    val schemaVersion: Int,
    val exportedAtMillis: Long,
    val appVersion: String = "",
    val events: List<PlaybackEventDto> = emptyList(),
    val artistAliases: List<ArtistAliasDto> = emptyList(),
    val playlists: List<PlaylistBackupDto> = emptyList(),
    val trackMetadataOverrides: List<TrackMetadataOverrideDto> = emptyList(),
    val artistArtworks: List<ArtistArtworkDto> = emptyList(),
) {
    val hasStatistics: Boolean
        get() = events.isNotEmpty() ||
            artistAliases.isNotEmpty() ||
            trackMetadataOverrides.isNotEmpty() ||
            artistArtworks.isNotEmpty()
}

@Serializable
internal data class PlaybackEventDto(
    val trackId: String,
    val trackTitle: String,
    val artistId: String,
    val artistName: String,
    val albumId: String? = null,
    val albumTitle: String? = null,
    val artworkUri: String? = null,
    val durationListenedMillis: Long,
    val trackDurationMillis: Long = 0L,
    val timestamp: Long,
)

@Serializable
internal data class ArtistAliasDto(
    val sourceArtistId: String,
    val sourceArtistName: String,
    val targetArtistId: String,
    val targetArtistName: String,
)

@Serializable
internal data class ArtistArtworkDto(
    val artistName: String,
    val artworkBase64: String,
    val updatedAtMillis: Long = 0L,
)

@Serializable
internal data class PlaylistBackupDto(
    val id: String,
    val name: String,
    val description: String = "",
    val isLikedPlaylist: Boolean = false,
    val coverBase64: String? = null,
    val tracks: List<PlaylistTrackBackupDto> = emptyList(),
) {
    /** Respaldos antiguos marcaban "Me gusta" solo por su id. */
    val isLiked: Boolean
        get() = isLikedPlaylist || id == LIKED_PLAYLIST_ID
}

@Serializable
internal data class PlaylistTrackBackupDto(
    val trackId: String,
    val title: String,
    val artistName: String,
    val albumTitle: String? = null,
    val durationMillis: Long = 0L,
    val relativePath: String? = null,
    val trackNumber: Int? = null,
)

internal fun PlaybackEvent.toDto() = PlaybackEventDto(
    trackId = trackId,
    trackTitle = trackTitle,
    artistId = artistId,
    artistName = artistName,
    albumId = albumId,
    albumTitle = albumTitle,
    artworkUri = artworkUri,
    durationListenedMillis = durationListenedMillis,
    trackDurationMillis = trackDurationMillis,
    timestamp = timestamp,
)

internal fun PlaybackEventDto.toDomain() = PlaybackEvent(
    trackId = trackId,
    trackTitle = trackTitle,
    artistId = artistId,
    artistName = artistName,
    albumId = albumId,
    albumTitle = albumTitle,
    artworkUri = artworkUri,
    durationListenedMillis = durationListenedMillis,
    trackDurationMillis = trackDurationMillis,
    timestamp = timestamp,
)

internal fun ArtistAlias.toDto() = ArtistAliasDto(
    sourceArtistId = source.id,
    sourceArtistName = source.name,
    targetArtistId = target.id,
    targetArtistName = target.name,
)

internal fun ArtistAliasDto.toDomain() = ArtistAlias(
    source = Artist(sourceArtistId, sourceArtistName),
    target = Artist(targetArtistId, targetArtistName),
)
