package com.catlytics.feature.library.impl.metadata

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.catlytics.core.designsystem.text.UiText
import com.catlytics.core.domain.usecase.library.ObserveAlbumsUseCase
import com.catlytics.core.domain.usecase.library.ObserveArtistAliasesUseCase
import com.catlytics.core.domain.usecase.library.ObserveArtistsUseCase
import com.catlytics.core.domain.usecase.library.ObserveTracksWithOverridesUseCase
import com.catlytics.core.domain.usecase.library.SaveBulkTrackMetadataUseCase
import com.catlytics.core.domain.usecase.library.UnmergeArtistUseCase
import com.catlytics.core.domain.usecase.library.WriteTracksTagsToFilesUseCase
import com.catlytics.core.model.Album
import com.catlytics.core.model.ArtistAlias
import com.catlytics.core.model.ArtistSummary
import com.catlytics.core.model.ArtworkEdit
import com.catlytics.core.model.BulkTrackMetadataEdit
import com.catlytics.core.model.FieldEdit
import com.catlytics.core.model.TrackWithOverride
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
internal class EditTracksMetadataViewModel @Inject constructor(
    private val observeTracksWithOverrides: ObserveTracksWithOverridesUseCase,
    observeArtists: ObserveArtistsUseCase,
    observeAlbums: ObserveAlbumsUseCase,
    private val saveBulkTrackMetadata: SaveBulkTrackMetadataUseCase,
    private val writeTracksTagsToFiles: WriteTracksTagsToFilesUseCase,
    observeArtistAliases: ObserveArtistAliasesUseCase,
    private val unmergeArtist: UnmergeArtistUseCase,
) : ViewModel() {
    private val trackIds = MutableStateFlow<List<String>?>(null)
    private val form = MutableStateFlow<EditTracksMetadataForm?>(null)
    private val isSaving = MutableStateFlow(false)
    private val _events = MutableSharedFlow<EditTracksMetadataEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<EditTracksMetadataEvent> = _events.asSharedFlow()

    private val tracks = trackIds.filterNotNull().flatMapLatest(observeTracksWithOverrides::invoke)

    private val artistsWithAliases = combine(
        observeArtists().catch { emit(emptyList()) },
        observeArtistAliases().catch { emit(emptyList()) },
        ::Pair,
    )

    val uiState: StateFlow<EditTracksMetadataUiState> = combine(
        tracks,
        form,
        isSaving,
        artistsWithAliases,
        observeAlbums().catch { emit(emptyList()) },
    ) { items, form, saving, (artists, aliases), albums ->
        when {
            items.isEmpty() -> EditTracksMetadataUiState.NotFound
            form == null -> EditTracksMetadataUiState.Loading
            else -> items.toUiState(form, saving, artists, albums).copy(
                mergedArtistAlias = (form.artist.edit as? FieldEdit.Set)?.let { aliases.aliasFor(it.value) },
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EditTracksMetadataUiState.Loading)

    fun open(ids: List<String>) {
        form.value = null
        trackIds.value = ids
        viewModelScope.launch {
            val items = observeTracksWithOverrides(ids).first()
            if (trackIds.value == ids) form.value = items.initialForm()
        }
    }

    fun onArtistChange(value: String) = updateForm { copy(artist = artist.copy(value = value, restore = false)) }

    fun onAlbumChange(value: String) = updateForm { copy(album = album.copy(value = value, restore = false)) }

    fun onRestoreArtist() = updateForm { copy(artist = artist.copy(value = "", restore = true)) }

    fun onRestoreAlbum() = updateForm { copy(album = album.copy(value = "", restore = true)) }

    fun onArtworkPicked(uri: String) = updateForm {
        copy(artwork = ArtworkEdit.Replace(uri), artworkPreviewUri = uri)
    }

    fun onSeparateArtistChange(value: Boolean) = updateForm { copy(separateArtist = value) }

    fun onWriteToFileChange(value: Boolean) = updateForm { copy(writeToFile = value) }

    fun onRestoreArtwork() = updateForm { copy(artwork = ArtworkEdit.Restore, artworkPreviewUri = null) }

    fun save() {
        val state = uiState.value as? EditTracksMetadataUiState.Editing ?: return
        val ids = trackIds.value ?: return
        if (!state.canSave) return
        runSaving {
            val separated = state.mergedArtistAlias?.takeIf { state.form.separateArtist }
            if (separated != null) unmergeArtist(separated.source)
            val result = saveBulkTrackMetadata(
                ids,
                BulkTrackMetadataEdit(
                    artistName = state.form.artist.edit,
                    albumTitle = state.form.album.edit,
                    artwork = state.form.artwork,
                ),
            )
            if (state.form.writeToFile) {
                _events.emit(EditTracksMetadataEvent.RequestFileWrite(state.mediaUris))
            } else {
                _events.emit(EditTracksMetadataEvent.Message(savedMessage(result.updated, separated)))
                _events.emit(EditTracksMetadataEvent.Close)
            }
        }
    }

    /** Called with the result of the system's MediaStore write-permission dialog. */
    fun onFileWritePermissionResult(granted: Boolean) {
        val ids = trackIds.value ?: return
        if (!granted) {
            viewModelScope.launch {
                _events.emit(EditTracksMetadataEvent.Message(UiText.Resource(R.string.library_edit_file_denied)))
                _events.emit(EditTracksMetadataEvent.Close)
            }
            return
        }
        // The edits are already saved, so failed file writes still close the editor.
        runSaving(errorMessage = UiText.Resource(R.string.library_edit_file_error), closeOnError = true) {
            val failed = writeTracksTagsToFiles(ids)
            val written = ids.size - failed
            _events.emit(
                EditTracksMetadataEvent.Message(
                    if (failed == 0) {
                        UiText.Plural(R.plurals.library_bulk_edit_files_saved, written)
                    } else {
                        UiText.Plural(R.plurals.library_bulk_edit_files_failed, failed)
                    },
                ),
            )
            _events.emit(EditTracksMetadataEvent.Close)
        }
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
                _events.emit(EditTracksMetadataEvent.Message(errorMessage))
                if (closeOnError) _events.emit(EditTracksMetadataEvent.Close)
            } finally {
                isSaving.value = false
            }
        }
    }

    private fun updateForm(transform: EditTracksMetadataForm.() -> EditTracksMetadataForm) {
        form.value = form.value?.transform()
    }
}

private fun savedMessage(updated: Int, separated: ArtistAlias?): UiText = when {
    updated > 0 -> UiText.Plural(R.plurals.library_bulk_edit_saved, updated)
    separated != null -> UiText.Resource(
        R.string.library_edit_artist_separated,
        listOf(separated.source.name, separated.target.name),
    )
    else -> UiText.Resource(R.string.library_bulk_edit_no_changes)
}

private fun List<TrackWithOverride>.initialForm() = EditTracksMetadataForm(
    artist = BulkFieldForm(initial = commonValue { it.track.artist.name }.orEmpty()),
    album = BulkFieldForm(initial = commonValue { it.track.albumTitle.orEmpty() }.orEmpty()),
    artworkPreviewUri = commonValue { it.track.artworkUri },
)

private fun List<TrackWithOverride>.toUiState(
    form: EditTracksMetadataForm,
    saving: Boolean,
    artists: List<ArtistSummary>,
    albums: List<Album>,
): EditTracksMetadataUiState.Editing {
    val artistKey = artistIdentityKey(form.artist.value.ifBlank { form.artist.initial })
    return EditTracksMetadataUiState.Editing(
        trackCount = size,
        mediaUris = map { it.track.mediaUri },
        form = form,
        artistIsMixed = commonValue { it.track.artist.name } == null,
        albumIsMixed = commonValue { it.track.albumTitle.orEmpty() } == null,
        artistHasEdits = any { it.override?.artistName != null },
        albumHasEdits = any { it.override?.albumTitle != null },
        artworkHasEdits = any { it.override?.artworkUri != null },
        artistSuggestions = artists.map { it.artist.name }.suggestionsFor(form.artist.value),
        albumSuggestions = albums
            .sortedByDescending { artistIdentityKey(it.artist.name) == artistKey }
            .map(Album::title)
            .suggestionsFor(form.album.value),
        isSaving = saving,
    )
}

private fun <T> List<TrackWithOverride>.commonValue(selector: (TrackWithOverride) -> T?): T? =
    map(selector).distinct().singleOrNull()
