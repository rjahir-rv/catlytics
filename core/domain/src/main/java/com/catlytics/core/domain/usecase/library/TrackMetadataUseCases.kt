package com.catlytics.core.domain.usecase.library

import com.catlytics.core.domain.repository.LibraryRepository
import com.catlytics.core.domain.repository.TrackMetadataRepository
import com.catlytics.core.model.EditableTrackMetadata
import com.catlytics.core.model.TrackMetadataEdit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class ObserveEditableTrackMetadataUseCase(
    private val metadataRepository: TrackMetadataRepository,
    private val libraryRepository: LibraryRepository,
) {
    operator fun invoke(trackId: String): Flow<EditableTrackMetadata?> = combine(
        metadataRepository.observeOriginalTrack(trackId),
        libraryRepository.observeAllTracks(),
        metadataRepository.observeOverrides(),
    ) { original, tracks, overrides ->
        original ?: return@combine null
        EditableTrackMetadata(
            original = original,
            current = tracks.firstOrNull { it.id == trackId } ?: original,
            override = overrides.firstOrNull { it.trackId == trackId },
        )
    }
}

class SaveTrackMetadataUseCase(
    private val repository: TrackMetadataRepository,
) {
    suspend operator fun invoke(trackId: String, edit: TrackMetadataEdit) {
        require(edit.title.isNotBlank()) { "El título no puede estar vacío." }
        repository.saveEdit(trackId, edit)
    }
}

class ResetTrackMetadataUseCase(
    private val repository: TrackMetadataRepository,
) {
    suspend operator fun invoke(trackId: String) = repository.reset(trackId)
}

class WriteTrackTagsToFileUseCase(
    private val repository: TrackMetadataRepository,
) {
    suspend operator fun invoke(trackId: String) = repository.writeTagsToFile(trackId)
}
