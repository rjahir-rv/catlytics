package com.catlytics.feature.settings.impl.edits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.catlytics.core.designsystem.text.UiText
import com.catlytics.core.domain.usecase.library.ObserveArtistAliasesUseCase
import com.catlytics.core.domain.usecase.library.ObserveArtistArtworkOverridesUseCase
import com.catlytics.core.domain.usecase.library.ObserveTrackMetadataOverridesUseCase
import com.catlytics.core.domain.usecase.library.ObserveTracksWithOverridesUseCase
import com.catlytics.core.domain.usecase.library.RemoveArtistArtworkUseCase
import com.catlytics.core.domain.usecase.library.ResetTracksMetadataUseCase
import com.catlytics.core.domain.usecase.library.SaveBulkTrackMetadataUseCase
import com.catlytics.core.domain.usecase.library.UnmergeArtistUseCase
import com.catlytics.core.model.ArtistAlias
import com.catlytics.core.model.ArtworkEdit
import com.catlytics.core.model.BulkTrackMetadataEdit
import com.catlytics.core.model.FieldEdit
import com.catlytics.core.model.TrackMetadataOverride
import com.catlytics.core.model.TrackWithOverride
import com.catlytics.core.model.isRenamed
import com.catlytics.feature.settings.impl.R
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
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Lists every metadata edit, artist merge/rename and artist image, and reverts them one by one or in bulk. */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
internal class MetadataEditsViewModel @Inject constructor(
    observeTrackMetadataOverrides: ObserveTrackMetadataOverridesUseCase,
    private val observeTracksWithOverrides: ObserveTracksWithOverridesUseCase,
    observeArtistAliases: ObserveArtistAliasesUseCase,
    private val saveBulkTrackMetadata: SaveBulkTrackMetadataUseCase,
    private val resetTracksMetadata: ResetTracksMetadataUseCase,
    private val unmergeArtist: UnmergeArtistUseCase,
    observeArtistArtworkOverrides: ObserveArtistArtworkOverridesUseCase,
    private val removeArtistArtwork: RemoveArtistArtworkUseCase,
) : ViewModel() {
    private val query = MutableStateFlow("")
    private val filter = MutableStateFlow(MetadataEditFilter.All)
    private val selectedTrackIds = MutableStateFlow(emptySet<String>())
    private val selectionMode = MutableStateFlow(false)
    private val dialog = MutableStateFlow<MetadataEditsDialog>(MetadataEditsDialog.Hidden)
    private val isBusy = MutableStateFlow(false)
    private val _messages = MutableSharedFlow<UiText>(extraBufferCapacity = 4)
    val messages: SharedFlow<UiText> = _messages.asSharedFlow()

    private val trackItems = observeTrackMetadataOverrides().flatMapLatest { overrides ->
        observeTracksWithOverrides(overrides.map(TrackMetadataOverride::trackId)).map { inLibrary ->
            val tracksById = inLibrary.associateBy { it.track.id }
            overrides
                .map { override -> override.toItem(tracksById[override.trackId]) }
                .filter { it.changes.isNotEmpty() }
                .sortedByDescending(TrackEditItem::updatedAtMillis)
        }
    }

    private val artistItems = observeArtistAliases().map { aliases ->
        aliases.map { alias -> ArtistEditItem(alias, alias.kind) }
    }

    private val artistImageItems = observeArtistArtworkOverrides().map { overrides ->
        overrides
            .map { ArtistImageEditItem(it.artistKey, it.artistName, it.artworkUri, it.updatedAtMillis) }
            .sortedByDescending(ArtistImageEditItem::updatedAtMillis)
    }

    private val edits = combine(trackItems, artistItems, artistImageItems, ::Triple)
        .catch { emit(Triple(emptyList(), emptyList(), emptyList())) }

    private val selection = combine(selectedTrackIds, selectionMode, ::Pair)

    private val controls = combine(query, filter, selection, dialog, isBusy) { query, filter, (selected, mode), dialog, busy ->
        MetadataEditsUiState(
            isLoading = false,
            query = query,
            filter = filter,
            selectedTrackIds = selected,
            selectionMode = mode,
            dialog = dialog,
            isBusy = busy,
        )
    }

    val uiState: StateFlow<MetadataEditsUiState> = combine(edits, controls) { (tracks, artists, images), controls ->
        val visibleTracks = tracks.filter { it.matches(controls.filter, controls.query) }
        controls.copy(
            tracks = visibleTracks,
            artists = artists.filter { it.matches(controls.filter, controls.query) },
            artistImages = images.filter { it.matches(controls.filter, controls.query) },
            totalTrackEdits = tracks.size,
            totalArtistEdits = artists.size,
            totalArtistImageEdits = images.size,
            // Edits reverted elsewhere leave the selection.
            selectedTrackIds = controls.selectedTrackIds.intersect(tracks.mapTo(HashSet(), TrackEditItem::trackId)),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MetadataEditsUiState())

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun onFilterChange(value: MetadataEditFilter) {
        filter.value = value
        clearSelection()
    }

    fun startSelection() {
        selectionMode.value = true
    }

    fun toggleSelection(trackId: String) {
        selectedTrackIds.value = selectedTrackIds.value.let { if (trackId in it) it - trackId else it + trackId }
    }

    fun selectAllVisible() {
        selectedTrackIds.value = uiState.value.tracks.mapTo(HashSet(), TrackEditItem::trackId)
    }

    fun clearSelection() {
        selectedTrackIds.value = emptySet()
        selectionMode.value = false
    }

    fun revertField(trackId: String, field: EditedField) {
        val edit = when (field) {
            EditedField.Title -> BulkTrackMetadataEdit(title = FieldEdit.Restore)
            EditedField.Artist -> BulkTrackMetadataEdit(artistName = FieldEdit.Restore)
            EditedField.Album -> BulkTrackMetadataEdit(albumTitle = FieldEdit.Restore)
            EditedField.Artwork -> BulkTrackMetadataEdit(artwork = ArtworkEdit.Restore)
        }
        runAction(UiText.Resource(R.string.settings_edits_field_reverted)) {
            saveBulkTrackMetadata(listOf(trackId), edit)
        }
    }

    fun revertTrack(trackId: String) {
        runAction(UiText.Plural(R.plurals.settings_edits_tracks_reverted, 1)) {
            resetTracksMetadata(listOf(trackId))
        }
    }

    fun revertArtist(alias: ArtistAlias) {
        runAction(UiText.Resource(R.string.settings_edits_artist_reverted, listOf(alias.source.name))) {
            unmergeArtist(alias.source)
        }
    }

    fun revertArtistImage(item: ArtistImageEditItem) {
        runAction(UiText.Resource(R.string.settings_edits_artist_image_reverted, listOf(item.artistName))) {
            removeArtistArtwork(item.artistKey)
        }
    }

    fun requestRevertSelected() {
        val count = uiState.value.selectedTrackIds.size
        if (count > 0) dialog.value = MetadataEditsDialog.ConfirmRevertSelected(count)
    }

    fun requestRevertFiltered() {
        val state = uiState.value
        if (state.hasResults) {
            dialog.value = MetadataEditsDialog.ConfirmRevertFiltered(
                trackCount = state.tracks.size,
                artistCount = state.artists.size + state.artistImages.size,
            )
        }
    }

    fun dismissDialog() {
        if (!isBusy.value) dialog.value = MetadataEditsDialog.Hidden
    }

    fun confirmDialog() {
        val state = uiState.value
        when (dialog.value) {
            MetadataEditsDialog.Hidden -> Unit
            is MetadataEditsDialog.ConfirmRevertSelected -> {
                val trackIds = state.selectedTrackIds.toList()
                runAction(UiText.Plural(R.plurals.settings_edits_tracks_reverted, trackIds.size)) {
                    resetTracksMetadata(trackIds)
                    clearSelection()
                }
            }
            is MetadataEditsDialog.ConfirmRevertFiltered -> {
                val trackIds = state.tracks.map(TrackEditItem::trackId)
                val aliases = state.artists.map(ArtistEditItem::alias)
                val artistImageKeys = state.artistImages.map(ArtistImageEditItem::artistKey)
                runAction(UiText.Resource(R.string.settings_edits_filtered_reverted)) {
                    if (trackIds.isNotEmpty()) resetTracksMetadata(trackIds)
                    aliases.forEach { unmergeArtist(it.source) }
                    artistImageKeys.forEach { removeArtistArtwork(it) }
                    clearSelection()
                }
            }
        }
    }

    private fun runAction(successMessage: UiText, action: suspend () -> Unit) {
        if (isBusy.value) return
        viewModelScope.launch {
            isBusy.value = true
            try {
                action()
                dialog.value = MetadataEditsDialog.Hidden
                _messages.emit(successMessage)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                _messages.emit(UiText.Resource(R.string.settings_edits_error))
            } finally {
                isBusy.value = false
            }
        }
    }
}

private val ArtistAlias.kind: ArtistEditKind
    get() = when {
        isDisplayRename -> ArtistEditKind.Spelling
        target.isRenamed -> ArtistEditKind.Rename
        else -> ArtistEditKind.Merge
    }

private fun TrackMetadataOverride.toItem(item: TrackWithOverride?): TrackEditItem {
    val track = item?.track
    return TrackEditItem(
        trackId = trackId,
        title = track?.title ?: title ?: originalTitle,
        artistName = track?.artist?.name ?: artistName ?: originalArtistName,
        artworkUri = track?.artworkUri ?: artworkUri,
        inLibrary = track != null,
        changes = buildList {
            title?.let { add(FieldChange(EditedField.Title, originalTitle, it)) }
            artistName?.let { add(FieldChange(EditedField.Artist, originalArtistName, it)) }
            albumTitle?.let { add(FieldChange(EditedField.Album, originalAlbumTitle, it)) }
            if (artworkUri != null) add(FieldChange(EditedField.Artwork))
        },
        updatedAtMillis = updatedAtMillis,
    )
}

private fun TrackEditItem.matches(filter: MetadataEditFilter, query: String): Boolean {
    val visible = when (filter) {
        MetadataEditFilter.All -> true
        MetadataEditFilter.Title -> has(EditedField.Title)
        MetadataEditFilter.Artist -> has(EditedField.Artist)
        MetadataEditFilter.Album -> has(EditedField.Album)
        MetadataEditFilter.Artwork -> has(EditedField.Artwork)
        MetadataEditFilter.Missing -> !inLibrary
        MetadataEditFilter.Artists -> false
    }
    if (!visible) return false
    val text = query.trim()
    if (text.isEmpty()) return true
    return sequenceOf(title, artistName)
        .plus(changes.asSequence().flatMap { sequenceOf(it.original, it.edited) })
        .any { it?.contains(text, ignoreCase = true) == true }
}

private fun ArtistImageEditItem.matches(filter: MetadataEditFilter, query: String): Boolean {
    val visible = filter == MetadataEditFilter.All ||
        filter == MetadataEditFilter.Artists ||
        filter == MetadataEditFilter.Artwork
    val text = query.trim()
    return visible && (text.isEmpty() || artistName.contains(text, ignoreCase = true))
}

private fun ArtistEditItem.matches(filter: MetadataEditFilter, query: String): Boolean {
    if (filter != MetadataEditFilter.All && filter != MetadataEditFilter.Artists) return false
    val text = query.trim()
    return text.isEmpty() ||
        alias.source.name.contains(text, ignoreCase = true) ||
        alias.target.name.contains(text, ignoreCase = true)
}
