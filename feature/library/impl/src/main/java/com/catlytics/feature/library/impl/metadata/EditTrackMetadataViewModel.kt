package com.catlytics.feature.library.impl.metadata

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.catlytics.core.designsystem.text.UiText
import com.catlytics.core.domain.usecase.library.MergeArtistsUseCase
import com.catlytics.core.domain.usecase.library.ObserveAlbumContentUseCase
import com.catlytics.core.domain.usecase.library.ObserveAlbumsUseCase
import com.catlytics.core.domain.usecase.library.ObserveArtistAliasesUseCase
import com.catlytics.core.domain.usecase.library.ObserveArtistsUseCase
import com.catlytics.core.domain.usecase.library.ObserveEditableTrackMetadataUseCase
import com.catlytics.core.domain.usecase.library.ResetTrackMetadataUseCase
import com.catlytics.core.domain.usecase.library.SaveBulkTrackMetadataUseCase
import com.catlytics.core.domain.usecase.library.SaveTrackMetadataUseCase
import com.catlytics.core.domain.usecase.library.UnmergeArtistUseCase
import com.catlytics.core.domain.usecase.library.WriteTrackTagsToFileUseCase
import com.catlytics.core.domain.usecase.library.WriteTracksTagsToFilesUseCase
import com.catlytics.core.model.Album
import com.catlytics.core.model.Artist
import com.catlytics.core.model.ArtistAlias
import com.catlytics.core.model.ArtistSummary
import com.catlytics.core.model.ArtworkEdit
import com.catlytics.core.model.BulkTrackMetadataEdit
import com.catlytics.core.model.EditableTrackMetadata
import com.catlytics.core.model.Track
import com.catlytics.core.model.TrackMetadataEdit
import com.catlytics.core.model.UNKNOWN_ARTIST_NAME
import com.catlytics.core.model.artistIdentityKey
import com.catlytics.feature.library.impl.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
internal class EditTrackMetadataViewModel @Inject constructor(
    private val observeEditableTrackMetadata: ObserveEditableTrackMetadataUseCase,
    observeArtists: ObserveArtistsUseCase,
    observeAlbums: ObserveAlbumsUseCase,
    private val saveTrackMetadata: SaveTrackMetadataUseCase,
    private val resetTrackMetadata: ResetTrackMetadataUseCase,
    private val writeTrackTagsToFile: WriteTrackTagsToFileUseCase,
    private val writeTracksTagsToFiles: WriteTracksTagsToFilesUseCase,
    private val mergeArtists: MergeArtistsUseCase,
    private val observeAlbumContent: ObserveAlbumContentUseCase,
    private val saveBulkTrackMetadata: SaveBulkTrackMetadataUseCase,
    observeArtistAliases: ObserveArtistAliasesUseCase,
    private val unmergeArtist: UnmergeArtistUseCase,
) : ViewModel() {
    private val trackId = MutableStateFlow<String?>(null)
    private val form = MutableStateFlow<EditTrackMetadataForm?>(null)
    private val isSaving = MutableStateFlow(false)
    private var fileWriteTrackIds: List<String> = emptyList()
    private val _events = MutableSharedFlow<EditTrackMetadataEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<EditTrackMetadataEvent> = _events.asSharedFlow()

    private val editable = trackId.filterNotNull().flatMapLatest(observeEditableTrackMetadata::invoke)

    private val editableWithAlbum = editable.flatMapLatest { metadata ->
        val albumId = metadata?.current?.albumId ?: return@flatMapLatest flowOf(metadata to emptyList())
        observeAlbumContent(albumId)
            .map { content -> metadata to content?.tracks.orEmpty() }
            .catch { emit(metadata to emptyList()) }
    }

    private val artistsWithAliases = combine(
        observeArtists().catch { emit(emptyList()) },
        observeArtistAliases().catch { emit(emptyList()) },
        ::Pair,
    )

    val uiState: StateFlow<EditTrackMetadataUiState> = combine(
        editableWithAlbum,
        form,
        isSaving,
        artistsWithAliases,
        observeAlbums().catch { emit(emptyList()) },
    ) { (metadata, albumTracks), form, saving, (artists, aliases), albums ->
        when {
            metadata == null -> EditTrackMetadataUiState.NotFound
            form == null -> EditTrackMetadataUiState.Loading
            else -> metadata.toUiState(form, saving, artists, albums).copy(
                albumTracks = albumTracks,
                mergedArtistAlias = aliases.aliasFor(form.artistName)
                    ?.takeIf { artistIdentityKey(it.target.name) == artistIdentityKey(metadata.current.artist.name) },
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EditTrackMetadataUiState.Loading)

    fun open(id: String) {
        if (trackId.value == id) return
        form.value = null
        trackId.value = id
        viewModelScope.launch {
            val metadata = observeEditableTrackMetadata(id).first()
            if (trackId.value == id) form.value = metadata?.initialForm()
        }
    }

    fun onTitleChange(value: String) = updateForm { copy(title = value) }

    fun onArtistChange(value: String) = updateForm { copy(artistName = value, unifyArtist = false) }

    fun onAlbumChange(value: String) = updateForm { copy(albumTitle = value) }

    fun onUnifyArtistChange(value: Boolean) = updateForm { copy(unifyArtist = value) }

    fun onApplyArtworkToAlbumChange(value: Boolean) = updateForm { copy(applyArtworkToAlbum = value) }

    fun onSeparateArtistChange(value: Boolean) = updateForm { copy(separateArtist = value) }

    fun onWriteToFileChange(value: Boolean) = updateForm { copy(writeToFile = value) }

    fun onArtworkPicked(uri: String) = updateForm {
        copy(artwork = ArtworkEdit.Replace(uri), artworkPreviewUri = uri)
    }

    fun onRestoreArtwork() {
        val state = uiState.value as? EditTrackMetadataUiState.Editing ?: return
        updateForm {
            copy(artwork = ArtworkEdit.Restore, artworkPreviewUri = state.original.artworkUri)
        }
    }

    fun restoreField(field: MetadataField) {
        val original = (uiState.value as? EditTrackMetadataUiState.Editing)?.original ?: return
        when (field) {
            MetadataField.Title -> onTitleChange(original.title)
            MetadataField.Artist -> onArtistChange(original.artist.name)
            MetadataField.Album -> onAlbumChange(original.albumTitle.orEmpty())
        }
    }

    fun save() {
        val state = uiState.value as? EditTrackMetadataUiState.Editing ?: return
        if (!state.canSave) return
        runSaving {
            val form = state.form
            val unifyTarget = state.unifyTarget?.takeIf { form.unifyArtist }
            if (unifyTarget != null) mergeArtists(state.original.artist, unifyTarget)
            state.mergedArtistAlias?.takeIf { form.separateArtist }?.let { unmergeArtist(it.source) }
            val artworkForAlbum = form.artwork.takeIf { form.applyArtworkToAlbum && state.canApplyArtworkToAlbum }
            saveTrackMetadata(
                state.original.id,
                TrackMetadataEdit(
                    title = form.title,
                    artistName = if (unifyTarget != null) state.original.artist.name else form.artistName,
                    albumTitle = form.albumTitle,
                    artwork = if (artworkForAlbum != null) ArtworkEdit.Keep else form.artwork,
                ),
            )
            if (artworkForAlbum != null) {
                saveBulkTrackMetadata(state.albumTrackIds, BulkTrackMetadataEdit(artwork = artworkForAlbum))
            }
            if (form.writeToFile) {
                // A cover applied to the album changed every album file, not only this one.
                val tracksToWrite = if (artworkForAlbum != null) state.albumTracks else listOf(state.original)
                fileWriteTrackIds = tracksToWrite.map(Track::id)
                _events.emit(EditTrackMetadataEvent.RequestFileWrite(tracksToWrite.map(Track::mediaUri)))
            } else {
                _events.emit(EditTrackMetadataEvent.Message(UiText.Resource(R.string.library_edit_saved)))
                _events.emit(EditTrackMetadataEvent.Close)
            }
        }
    }

    fun resetAll() {
        val state = uiState.value as? EditTrackMetadataUiState.Editing ?: return
        runSaving {
            resetTrackMetadata(state.original.id)
            _events.emit(EditTrackMetadataEvent.Message(UiText.Resource(R.string.library_edit_reset_done)))
            _events.emit(EditTrackMetadataEvent.Close)
        }
    }

    fun onFileWritePermissionResult(granted: Boolean) {
        val id = trackId.value ?: return
        if (!granted) {
            viewModelScope.launch {
                _events.emit(EditTrackMetadataEvent.Message(UiText.Resource(R.string.library_edit_file_denied)))
                _events.emit(EditTrackMetadataEvent.Close)
            }
            return
        }
        val trackIds = fileWriteTrackIds.ifEmpty { listOf(id) }
        runSaving(
            errorMessage = UiText.Resource(R.string.library_edit_file_error),
            closeOnError = true,
        ) {
            val message = if (trackIds.size == 1) {
                writeTrackTagsToFile(trackIds.single())
                UiText.Resource(R.string.library_edit_file_saved)
            } else {
                val failed = writeTracksTagsToFiles(trackIds)
                if (failed == 0) {
                    UiText.Plural(R.plurals.library_bulk_edit_files_saved, trackIds.size)
                } else {
                    UiText.Plural(R.plurals.library_bulk_edit_files_failed, failed)
                }
            }
            _events.emit(EditTrackMetadataEvent.Message(message))
            _events.emit(EditTrackMetadataEvent.Close)
        }
    }

    private fun updateForm(transform: EditTrackMetadataForm.() -> EditTrackMetadataForm) {
        form.value = form.value?.transform()
    }

    private fun runSaving(
        errorMessage: UiText = UiText.Resource(R.string.library_edit_error),
        closeOnError: Boolean = false,
        action: suspend () -> Unit,
    ) {
        if (isSaving.value) return
        viewModelScope.launch {
            isSaving.value = true
            try {
                action()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                _events.emit(EditTrackMetadataEvent.Message(errorMessage))
                if (closeOnError) _events.emit(EditTrackMetadataEvent.Close)
            } finally {
                isSaving.value = false
            }
        }
    }
}

internal enum class MetadataField { Title, Artist, Album }

private fun EditableTrackMetadata.initialForm() = EditTrackMetadataForm(
    title = override?.title ?: original.title,
    artistName = override?.artistName ?: original.artist.name,
    albumTitle = override?.albumTitle ?: original.albumTitle.orEmpty(),
    artworkPreviewUri = override?.artworkUri ?: original.artworkUri,
)

private fun EditableTrackMetadata.toUiState(
    form: EditTrackMetadataForm,
    saving: Boolean,
    artists: List<ArtistSummary>,
    albums: List<Album>,
): EditTrackMetadataUiState.Editing {
    val artistKey = artistIdentityKey(form.artistName)
    val unifyTarget = artists.map(ArtistSummary::artist)
        .firstOrNull { artistIdentityKey(it.name) == artistKey }
        ?.takeIf {
            artistKey != artistIdentityKey(original.artist.name) &&
                artistIdentityKey(current.artist.name) != artistKey &&
                original.artist.name != UNKNOWN_ARTIST_NAME
        }
    return EditTrackMetadataUiState.Editing(
        original = original,
        form = form,
        artistSuggestions = artists.map { it.artist.name }.suggestionsFor(form.artistName),
        albumSuggestions = albums
            .sortedByDescending { artistIdentityKey(it.artist.name) == artistKey }
            .map(Album::title)
            .suggestionsFor(form.albumTitle),
        unifyTarget = unifyTarget,
        hasArtworkEdit = when (form.artwork) {
            ArtworkEdit.Keep -> override?.artworkUri != null
            ArtworkEdit.Restore -> false
            is ArtworkEdit.Replace -> true
        },
        hasOverride = override != null,
        isSaving = saving,
    )
}

internal fun List<ArtistAlias>.aliasFor(name: String): ArtistAlias? {
    if (name.isBlank()) return null
    val key = artistIdentityKey(name)
    return firstOrNull { !it.isDisplayRename && artistIdentityKey(it.source.name) == key }
}

internal fun List<String>.suggestionsFor(query: String): List<String> {
    val normalized = query.trim()
    if (normalized.isEmpty()) return emptyList()
    return asSequence()
        .distinctBy(::artistIdentityKey)
        .filter { it.contains(normalized, ignoreCase = true) && it != normalized }
        .take(MAX_SUGGESTIONS)
        .toList()
}

private const val MAX_SUGGESTIONS = 5
