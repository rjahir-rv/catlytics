package com.catlytics.core.data.repository

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable

@OptIn(ExperimentalSerializationApi::class, InternalSerializationApi::class)
@Serializable
internal data class TrackMetadataOverrideDto(
    val trackId: String,
    val fileKey: String? = null,
    val title: String? = null,
    val artistName: String? = null,
    val albumTitle: String? = null,
    val artworkBase64: String? = null,
    val originalTitle: String,
    val originalArtistName: String,
    val originalAlbumTitle: String? = null,
    val durationMillis: Long = 0L,
    val updatedAtMillis: Long = 0L,
) {
    val hasEdits: Boolean
        get() = title != null || artistName != null || albumTitle != null || artworkBase64 != null
}
