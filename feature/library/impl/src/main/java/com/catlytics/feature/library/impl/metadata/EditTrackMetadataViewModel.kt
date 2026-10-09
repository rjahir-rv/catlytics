package com.catlytics.feature.library.impl.metadata

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.catlytics.core.designsystem.text.UiText
import com.catlytics.core.domain.usecase.library.MergeArtistsUseCase
import com.catlytics.core.domain.usecase.library.ObserveAlbumsUseCase
import com.catlytics.core.domain.usecase.library.ObserveArtistsUseCase
import com.catlytics.core.domain.usecase.library.ObserveEditableTrackMetadataUseCase
import com.catlytics.core.domain.usecase.library.ResetTrackMetadataUseCase
import com.catlytics.core.domain.usecase.library.SaveTrackMetadataUseCase
import com.catlytics.core.domain.usecase.library.WriteTrackTagsToFileUseCase
import com.catlytics.core.model.Album
import com.catlytics.core.model.Artist
import com.catlytics.core.model.ArtistSummary
import com.catlytics.core.model.ArtworkEdit
import com.catlytics.core.model.EditableTrackMetadata
import com.catlytics.core.model.TrackMetadataEdit
import com.catlytics.core.model.UNKNOWN_ARTIST_NAME
import com.catlytics.core.model.artistIdentityKey
import com.catlytics.feature.library.impl.R
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
internal class EditTrackMetadataViewModel @Inject constructor(
    private val observeEditableTrackMetadata: ObserveEditableTrackMetadataUseCase,
    observeArtists: ObserveArtistsUseCase,
    observeAlbums: ObserveAlbumsUseCase,
    private val saveTrackMetadata: SaveTrackMetadataUseCase,
    private val resetTrackMetadata: ResetTrackMetadataUseCase,
    private val writeTrackTagsToFile: WriteTrackTagsToFileUseCase,
    private val mergeArtists: MergeArtistsUseCase,
) : ViewModel() {
    private val trackId = MutableStateFlow<String?>(null)
    private val form = MutableStateFlow<EditTrackMetadataForm?>(null)
    private val isSaving = MutableStateFlow(false)
    private val _events = MutableSharedFlow<EditTrackMetadataEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<EditTrackMetadataEvent> = _events.asSharedFlow()

    private val editable = trackId.filterNotNull().flatMapLatest(observeEditableTrackMetadata::invoke)

    val uiState: StateFlow<EditTrackMetadataUiState> = combine(
        editable,
        form,
        isSaving,
        observeArtists().catch { emit(emptyList()) },
        observeAlbums().catch { emit(emptyList()) },
    ) { metadata, form, saving, artists, albums ->
        when {
            metadata == null -> EditTrackMetadataUiState.NotFound
            form == null -> EditTrackMetadataUiState.Loading
            else -> metadata.toUiState(form, saving, artists, albums)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EditTrackMetadataUiState.Loading)

    fun open(id: String) {
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
            saveTrackMetadata(
                state.original.id,
                TrackMetadataEdit(
                    title = form.title,
                    // A merge already moves this track, so no per-track artist edit is needed.
                    artistName = if (unifyTarget != null) state.original.artist.name else form.artistName,
                    albumTitle = form.albumTitle,
                    artwork = form.artwork,
                ),
            )
            if (form.writeToFile) {
                _events.emit(EditTrackMetadataEvent.RequestFileWrite(state.original.mediaUri))
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

    /** Called with the result of the system's MediaStore write-permission dialog. */
    fun onFileWritePermissionResult(granted: Boolean) {
        val id = trackId.value ?: return
        if (!granted) {
            viewModelScope.launch {
                _events.emit(EditTrackMetadataEvent.Message(UiText.Resource(R.string.library_edit_file_denied)))
                _events.emit(EditTrackMetadataEvent.Close)
            }
            return
        }
        // The edit itself is already saved, so a failed file write still closes the editor.
        runSaving(
            errorMessage = UiText.Resource(R.string.library_edit_file_error),
            closeOnError = true,
        ) {
            writeTrackTagsToFile(id)
            _events.emit(EditTrackMetadataEvent.Message(UiText.Resource(R.string.library_edit_file_saved)))
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

private fun List<String>.suggestionsFor(query: String): List<String> {
    val normalized = query.trim()
    if (normalized.isEmpty()) return emptyList()
    return asSequence()
        .distinctBy(::artistIdentityKey)
        .filter { it.contains(normalized, ignoreCase = true) && it != normalized }
        .take(MAX_SUGGESTIONS)
        .toList()
}

private const val MAX_SUGGESTIONS = 5
