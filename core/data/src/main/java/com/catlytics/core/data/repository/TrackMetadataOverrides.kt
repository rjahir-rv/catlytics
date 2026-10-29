package com.catlytics.core.data.repository

import com.catlytics.core.data.model.TrackEntity
import com.catlytics.core.model.TrackMetadataOverride
import com.catlytics.core.model.artistIdentityKey

/**
 * Maps edited artist/album names onto ids, so an edited track joins an existing artist or album
 * with the same name instead of creating a lookalike. Used both when applying overrides to the
 * library and when storing them
 */
internal class OverrideIdResolver(rawTracks: List<TrackEntity>) {
    private val artistIdsByKey = buildMap {
        rawTracks.forEach { putIfAbsent(artistIdentityKey(it.artistName), it.artistId) }
    }
    private val albumIdsByKey = buildMap {
        rawTracks.forEach { track ->
            val albumId = track.albumId ?: return@forEach
            val albumTitle = track.albumTitle ?: return@forEach
            putIfAbsent(albumKey(artistIdentityKey(track.artistName), albumTitle), albumId)
        }
    }

    fun artistId(artistName: String): String {
        val key = artistIdentityKey(artistName)
        return artistIdsByKey[key] ?: "override-artist-$key"
    }

    fun albumId(artistName: String, albumTitle: String): String {
        val key = albumKey(artistIdentityKey(artistName), albumTitle)
        return albumIdsByKey[key] ?: "override-album-$key"
    }

    private fun albumKey(artistKey: String, albumTitle: String) =
        "$artistKey/${artistIdentityKey(albumTitle)}"
}

internal fun List<TrackEntity>.applyMetadataOverrides(
    overrides: List<TrackMetadataOverride>,
): List<TrackEntity> {
    if (overrides.isEmpty()) return this
    val overridesById = overrides.associateBy(TrackMetadataOverride::trackId)
    val overridesByFileKey = overrides
        .mapNotNull { override -> override.fileKey?.let { it to override } }
        .toMap()
    val resolver = OverrideIdResolver(this)
    return map { track ->
        // The file key covers overrides whose MediaStore id changed before the next reconcile.
        val override = overridesById[track.id]
            ?: track.fileKey?.let(overridesByFileKey::get)
            ?: return@map track
        track.withOverride(override, resolver)
    }
}

internal fun TrackEntity.withOverride(
    override: TrackMetadataOverride,
    resolver: OverrideIdResolver,
): TrackEntity {
    val artistName = override.artistName ?: artistName
    val albumTitle = override.albumTitle ?: albumTitle
    return copy(
        title = override.title ?: title,
        artistId = override.artistName?.let(resolver::artistId) ?: artistId,
        artistName = artistName,
        albumTitle = albumTitle,
        albumId = override.albumTitle?.let { resolver.albumId(artistName, it) } ?: albumId,
        artworkUri = override.artworkUri ?: artworkUri,
        hasArtworkOverride = override.artworkUri != null,
    )
}

/** Album/artist artwork: an edited cover wins over MediaStore's, which exists even when empty. */
internal fun List<TrackEntity>.groupArtworkUri(): String? =
    firstOrNull(TrackEntity::hasArtworkOverride)?.artworkUri
        ?: firstNotNullOfOrNull(TrackEntity::artworkUri)
