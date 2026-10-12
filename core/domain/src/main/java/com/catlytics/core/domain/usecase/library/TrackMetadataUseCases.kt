package com.catlytics.core.domain.usecase.library

import com.catlytics.core.domain.repository.LibraryRepository
import com.catlytics.core.domain.repository.TrackMetadataRepository
import com.catlytics.core.model.BulkEditResult
import com.catlytics.core.model.BulkTrackMetadataEdit
import com.catlytics.core.model.EditableTrackMetadata
import com.catlytics.core.model.FieldEdit
import com.catlytics.core.model.TrackMetadataOverride
import com.catlytics.core.model.TrackWithOverride
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

class ObserveTrackMetadataOverridesUseCase(
    private val repository: TrackMetadataRepository,
) {
    operator fun invoke(): Flow<List<TrackMetadataOverride>> = repository.observeOverrides()
}

class SaveBulkTrackMetadataUseCase(
    private val repository: TrackMetadataRepository,
) {
    suspend operator fun invoke(trackIds: List<String>, edit: BulkTrackMetadataEdit): BulkEditResult {
        require(trackIds.isNotEmpty()) { "No hay canciones seleccionadas." }
        listOf(edit.title, edit.artistName, edit.albumTitle).forEach { field ->
            require(field !is FieldEdit.Set || field.value.isNotBlank()) {
                "Los campos editados no pueden estar vacíos."
            }
        }
        return repository.saveBulkEdit(trackIds, edit)
    }
}

class ResetTracksMetadataUseCase(
    private val repository: TrackMetadataRepository,
) {
    suspend operator fun invoke(trackIds: List<String>) = repository.resetMany(trackIds)
}

class WriteTracksTagsToFilesUseCase(
    private val repository: TrackMetadataRepository,
) {
    suspend operator fun invoke(trackIds: List<String>): Int = repository.writeTagsToFiles(trackIds)
}

class ObserveTracksWithOverridesUseCase(
    private val metadataRepository: TrackMetadataRepository,
    private val libraryRepository: LibraryRepository,
) {
    operator fun invoke(trackIds: List<String>): Flow<List<TrackWithOverride>> = combine(
        libraryRepository.observeAllTracks(),
        metadataRepository.observeOverrides(),
    ) { tracks, overrides ->
        val tracksById = tracks.associateBy { it.id }
        val overridesById = overrides.associateBy(TrackMetadataOverride::trackId)
        trackIds.distinct().mapNotNull { id ->
            tracksById[id]?.let { TrackWithOverride(it, overridesById[id]) }
        }
    }
}
