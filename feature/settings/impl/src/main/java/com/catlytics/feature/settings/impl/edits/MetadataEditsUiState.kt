package com.catlytics.feature.settings.impl.edits

import com.catlytics.core.model.ArtistAlias

internal enum class MetadataEditFilter { All, Title, Artist, Album, Artwork, Missing, Artists }

internal enum class EditedField { Title, Artist, Album, Artwork }

internal data class FieldChange(
    val field: EditedField,
    val original: String? = null,
    val edited: String? = null,
)

internal data class TrackEditItem(
    val trackId: String,
    val title: String,
    val artistName: String,
    val artworkUri: String?,
    val inLibrary: Boolean,
    val changes: List<FieldChange>,
    val updatedAtMillis: Long,
) {
    fun has(field: EditedField): Boolean = changes.any { it.field == field }
}

internal enum class ArtistEditKind { Merge, Rename, Spelling }

internal data class ArtistEditItem(
    val alias: ArtistAlias,
    val kind: ArtistEditKind,
)

internal sealed interface MetadataEditsDialog {
    data object Hidden : MetadataEditsDialog

    /** Reverting everything the current filter shows. */
    data class ConfirmRevertFiltered(val trackCount: Int, val artistCount: Int) : MetadataEditsDialog

    data class ConfirmRevertSelected(val trackCount: Int) : MetadataEditsDialog
}

internal data class MetadataEditsUiState(
    val isLoading: Boolean = true,
    val query: String = "",
    val filter: MetadataEditFilter = MetadataEditFilter.All,
    val tracks: List<TrackEditItem> = emptyList(),
    val artists: List<ArtistEditItem> = emptyList(),
    val totalTrackEdits: Int = 0,
    val totalArtistEdits: Int = 0,
    val selectedTrackIds: Set<String> = emptySet(),
    val selectionMode: Boolean = false,
    val dialog: MetadataEditsDialog = MetadataEditsDialog.Hidden,
    val isBusy: Boolean = false,
) {
    val selectionActive: Boolean get() = selectionMode || selectedTrackIds.isNotEmpty()

    val hasAnyEdits: Boolean get() = totalTrackEdits + totalArtistEdits > 0

    val hasResults: Boolean get() = tracks.isNotEmpty() || artists.isNotEmpty()
}
