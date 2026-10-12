package com.catlytics.feature.library.impl.metadata

import com.catlytics.core.designsystem.text.UiText
import com.catlytics.core.model.ArtworkEdit
import com.catlytics.core.model.FieldEdit

internal data class BulkFieldForm(
    val initial: String,
    val value: String = initial,
    val restore: Boolean = false,
) {
    val edit: FieldEdit
        get() {
            val trimmed = value.trim()
            return when {
                restore -> FieldEdit.Restore
                trimmed.isEmpty() || trimmed == initial -> FieldEdit.Keep
                else -> FieldEdit.Set(trimmed)
            }
        }
}

internal data class EditTracksMetadataForm(
    val artist: BulkFieldForm,
    val album: BulkFieldForm,
    val artwork: ArtworkEdit = ArtworkEdit.Keep,
    val artworkPreviewUri: String?,
    val writeToFile: Boolean = false,
)

internal sealed interface EditTracksMetadataUiState {
    data object Loading : EditTracksMetadataUiState

    data object NotFound : EditTracksMetadataUiState

    data class Editing(
        val trackCount: Int,
        val mediaUris: List<String>,
        val form: EditTracksMetadataForm,
        val artistIsMixed: Boolean,
        val albumIsMixed: Boolean,
        /** Whether any track has an edit on that field that could be restored. */
        val artistHasEdits: Boolean,
        val albumHasEdits: Boolean,
        val artworkHasEdits: Boolean,
        val artistSuggestions: List<String>,
        val albumSuggestions: List<String>,
        val isSaving: Boolean,
    ) : EditTracksMetadataUiState {
        val hasChanges: Boolean
            get() = form.artist.edit != FieldEdit.Keep ||
                form.album.edit != FieldEdit.Keep ||
                form.artwork != ArtworkEdit.Keep

        /** Writing to the files alone is a valid save, e.g. to persist earlier edits. */
        val canSave: Boolean get() = (hasChanges || form.writeToFile) && !isSaving
    }
}

internal sealed interface EditTracksMetadataEvent {
    data class Message(val text: UiText) : EditTracksMetadataEvent
    data class RequestFileWrite(val mediaUris: List<String>) : EditTracksMetadataEvent

    data object Close : EditTracksMetadataEvent
}
