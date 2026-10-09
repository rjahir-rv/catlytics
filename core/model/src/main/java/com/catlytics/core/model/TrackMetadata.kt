package com.catlytics.core.model

data class TrackMetadataOverride(
    val trackId: String,
    val fileKey: String?,
    val title: String? = null,
    val artistName: String? = null,
    val albumTitle: String? = null,
    val artworkUri: String? = null,
    val originalTitle: String,
    val originalArtistName: String,
    val originalAlbumTitle: String?,
    val durationMillis: Long,
    val updatedAtMillis: Long,
) {
    val isEmpty: Boolean
        get() = title == null && artistName == null && albumTitle == null && artworkUri == null
}

data class EditableTrackMetadata(
    val original: Track,
    val current: Track,
    val override: TrackMetadataOverride?,
)

data class TrackMetadataEdit(
    val title: String,
    val artistName: String,
    val albumTitle: String,
    val artwork: ArtworkEdit = ArtworkEdit.Keep,
)

sealed interface ArtworkEdit {
    /** Keep whatever artwork override is currently stored. */
    data object Keep : ArtworkEdit

    /** Drop the artwork override and go back to the scanned artwork. */
    data object Restore : ArtworkEdit

    data class Replace(val sourceUri: String) : ArtworkEdit
}
