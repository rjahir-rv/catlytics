package com.catlytics.feature.library.impl.metadata

import com.catlytics.core.designsystem.text.UiText
import com.catlytics.core.model.Artist
import com.catlytics.core.model.ArtworkEdit
import com.catlytics.core.model.Track

internal data class EditTrackMetadataForm(
    val title: String,
    val artistName: String,
    val albumTitle: String,
    val artwork: ArtworkEdit = ArtworkEdit.Keep,
    val artworkPreviewUri: String?,
    val unifyArtist: Boolean = false,
    val writeToFile: Boolean = false,
)

internal sealed interface EditTrackMetadataUiState {
    data object Loading : EditTrackMetadataUiState

    data object NotFound : EditTrackMetadataUiState

    data class Editing(
        val original: Track,
        val form: EditTrackMetadataForm,
        val artistSuggestions: List<String>,
        val albumSuggestions: List<String>,
        val unifyTarget: Artist?,
        val hasArtworkEdit: Boolean,
        val hasOverride: Boolean,
        val isSaving: Boolean,
    ) : EditTrackMetadataUiState {
        val canSave: Boolean get() = form.title.isNotBlank() && !isSaving
    }
}

internal sealed interface EditTrackMetadataEvent {
    data class Message(val text: UiText) : EditTrackMetadataEvent
    data class RequestFileWrite(val mediaUri: String) : EditTrackMetadataEvent

    data object Close : EditTrackMetadataEvent
}
