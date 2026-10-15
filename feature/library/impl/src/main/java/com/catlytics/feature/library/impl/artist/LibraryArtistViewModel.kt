package com.catlytics.feature.library.impl.artist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.catlytics.core.designsystem.text.UiText
import com.catlytics.core.domain.usecase.library.ObserveArtistContentUseCase
import com.catlytics.core.domain.usecase.library.ObserveArtistsUseCase
import com.catlytics.core.domain.usecase.library.ObserveArtistAliasesUseCase
import com.catlytics.core.domain.usecase.library.MergeArtistsUseCase
import com.catlytics.core.domain.usecase.library.ObserveDetailSortSettingsUseCase
import com.catlytics.core.domain.usecase.library.ObserveLibraryTrackUseCase
import com.catlytics.core.domain.usecase.library.ObserveTrackPlayCountsUseCase
import com.catlytics.core.domain.usecase.library.SetArtistAlbumSortUseCase
import com.catlytics.core.domain.usecase.library.SetArtistTrackSortUseCase
import com.catlytics.core.domain.usecase.playback.PlayShuffledQueueUseCase
import com.catlytics.core.domain.usecase.library.RenameArtistUseCase
import com.catlytics.core.domain.usecase.library.UnmergeArtistUseCase
import com.catlytics.core.domain.usecase.playback.PlayTrackUseCase
import com.catlytics.core.model.Album
import com.catlytics.core.model.ArtistAlbumSort
import com.catlytics.core.model.ArtistTrackSort
import com.catlytics.core.model.DetailSortSettings
import com.catlytics.core.model.Track
import com.catlytics.core.model.Artist
import com.catlytics.core.model.artistIdentityKey
import com.catlytics.feature.library.impl.R
import com.catlytics.feature.library.impl.collection.needsPlayCounts
import com.catlytics.feature.library.impl.collection.sortedFor
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
internal class LibraryArtistViewModel @Inject constructor(
    private val observeArtistContentUseCase: ObserveArtistContentUseCase,
    observeArtistsUseCase: ObserveArtistsUseCase,
    observeArtistAliasesUseCase: ObserveArtistAliasesUseCase,
    private val mergeArtistsUseCase: MergeArtistsUseCase,
    private val unmergeArtistUseCase: UnmergeArtistUseCase,
    private val playTrackUseCase: PlayTrackUseCase,
    private val renameArtistUseCase: RenameArtistUseCase,
    private val observeLibraryTrack: ObserveLibraryTrackUseCase,
    private val playShuffledQueueUseCase: PlayShuffledQueueUseCase,
    observeDetailSortSettings: ObserveDetailSortSettingsUseCase,
    observeTrackPlayCounts: ObserveTrackPlayCountsUseCase,
    private val setArtistTrackSort: SetArtistTrackSortUseCase,
    private val setArtistAlbumSort: SetArtistAlbumSortUseCase,
) : ViewModel() {
    private val artistId = MutableStateFlow<String?>(null)
    private val searchQuery = MutableStateFlow("")
    private val mergeDialog = MutableStateFlow<ArtistMergeDialog>(ArtistMergeDialog.Hidden)
    private val isMergeBusy = MutableStateFlow(false)
    private val _messages = MutableSharedFlow<UiText>(extraBufferCapacity = 1)
    val messages: SharedFlow<UiText> = _messages.asSharedFlow()

    private var lastTrackIds: List<String> = emptyList()

    private val content = artistId
        .filterNotNull()
        .flatMapLatest { id ->
            observeArtistContentUseCase(id).transformLatest { content ->
                if (content != null) {
                    lastTrackIds = content.tracks.map(Track::id)
                    emit(content)
                    return@transformLatest
                }
                val newArtistId = lastTrackIds.firstNotNullOfOrNull { trackId ->
                    observeLibraryTrack(trackId).first()?.artist?.id?.takeIf { it != id }
                }
                if (newArtistId != null) artistId.value = newArtistId else emit(null)
            }
        }

    private val sortWithCounts = observeDetailSortSettings()
        .catch { emit(DetailSortSettings()) }
        .distinctUntilChanged()
        .flatMapLatest { settings ->
            if (settings.artistTracks.needsPlayCounts) {
                observeTrackPlayCounts().catch { emit(emptyMap()) }.map { counts -> settings to counts }
            } else {
                flowOf(settings to emptyMap())
            }
        }

    private val sortedContent = combine(content, sortWithCounts) { content, (settings, counts) ->
        content?.copy(
            tracks = content.tracks.sortedFor(settings.artistTracks, counts),
            albums = content.albums.sortedFor(settings.artistAlbums),
        ) to settings
    }

    private val artistData = combine(
        sortedContent,
        searchQuery,
        observeArtistsUseCase().catch { emit(emptyList()) },
        observeArtistAliasesUseCase().catch { emit(emptyList()) },
    ) { (content, sortSettings), query, artists, aliases ->
        if (content == null) {
            LibraryArtistUiState.NotFound
        } else {
            val filteredTracks = content.tracks.filterArtistTracksByQuery(query)
            val filteredAlbums = content.albums.filterArtistAlbumsByQuery(query)
            LibraryArtistUiState.Success(
                content = content.copy(
                    summary = content.summary.copy(
                        albumCount = filteredAlbums.size,
                        trackCount = filteredTracks.size
                    ),
                    tracks = filteredTracks,
                    albums = filteredAlbums
                ),
                searchQuery = query,
                playbackQueue = content.tracks,
                trackSort = sortSettings.artistTracks,
                albumSort = sortSettings.artistAlbums,
                mergeCandidates = artists
                    .map { it.artist }
                    .filter { candidate ->
                        candidate.id != content.summary.artist.id &&
                            artistIdentityKey(candidate.name) !=
                            artistIdentityKey(content.summary.artist.name)
                    },
                aliases = aliases.filter { alias ->
                    artistIdentityKey(alias.target.name) ==
                        artistIdentityKey(content.summary.artist.name)
                },
            )
        }
    }

    val uiState = combine(artistData, mergeDialog, isMergeBusy) { state, dialog, busy ->
        if (state is LibraryArtistUiState.Success) {
            state.copy(mergeDialog = dialog, isMergeBusy = busy)
        } else {
            state
        }
    }
        .catch { error ->
            emit(
                LibraryArtistUiState.Error(
                    UiText.Resource(R.string.library_artist_load_error),
                ),
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = LibraryArtistUiState.Loading,
        )

    fun openArtist(id: String) {
        if (artistId.value == null) artistId.value = id
    }

    fun onSearchQueryChanged(query: String) {
        searchQuery.value = query
    }

    fun showMergePicker() {
        mergeDialog.value = ArtistMergeDialog.SelectTarget()
    }

    fun updateMergeQuery(query: String) {
        mergeDialog.value = ArtistMergeDialog.SelectTarget(query)
    }

    fun selectMergeTarget(target: Artist) {
        mergeDialog.value = ArtistMergeDialog.ConfirmMerge(target)
    }

    fun showAliasManager() {
        mergeDialog.value = ArtistMergeDialog.ManageAliases
    }

    fun requestUnmerge(alias: com.catlytics.core.model.ArtistAlias) {
        mergeDialog.value = ArtistMergeDialog.ConfirmUnmerge(alias)
    }

    fun showRename() {
        val state = uiState.value as? LibraryArtistUiState.Success ?: return
        if (state.canRename) mergeDialog.value = ArtistMergeDialog.Rename(state.content.summary.artist.name)
    }

    fun updateRenameName(name: String) {
        if (mergeDialog.value is ArtistMergeDialog.Rename) mergeDialog.value = ArtistMergeDialog.Rename(name)
    }

    fun confirmRename() {
        val state = uiState.value as? LibraryArtistUiState.Success ?: return
        val name = (mergeDialog.value as? ArtistMergeDialog.Rename)?.name?.trim() ?: return
        val artist = state.content.summary.artist
        if (name.isEmpty() || !state.canRename) return
        if (name == artist.name) {
            mergeDialog.value = ArtistMergeDialog.Hidden
            return
        }
        // An existing artist's name is a merge, which the usual confirmation explains.
        state.renameMergeTarget?.let { target ->
            mergeDialog.value = ArtistMergeDialog.ConfirmMerge(target)
            return
        }
        runMergeAction(UiText.Resource(R.string.library_artist_rename_success)) {
            renameArtistUseCase(artist, name)
        }
    }

    fun dismissMergeDialog() {
        if (!isMergeBusy.value) mergeDialog.value = ArtistMergeDialog.Hidden
    }

    fun confirmMerge() {
        val state = uiState.value as? LibraryArtistUiState.Success ?: return
        val target = (mergeDialog.value as? ArtistMergeDialog.ConfirmMerge)?.target ?: return
        runMergeAction(UiText.Resource(R.string.library_artist_merge_success)) {
            mergeArtistsUseCase(state.content.summary.artist, target)
        }
    }

    fun confirmUnmerge() {
        val alias = (mergeDialog.value as? ArtistMergeDialog.ConfirmUnmerge)?.alias ?: return
        runMergeAction(UiText.Resource(R.string.library_artist_unmerge_success)) {
            unmergeArtistUseCase(alias.source)
        }
    }

    private fun runMergeAction(successMessage: UiText, action: suspend () -> Unit) {
        if (isMergeBusy.value) return
        viewModelScope.launch {
            isMergeBusy.value = true
            try {
                action()
                mergeDialog.value = ArtistMergeDialog.Hidden
                _messages.emit(successMessage)
            } catch (cancellationException: CancellationException) {
                throw cancellationException
            } catch (error: Exception) {
                _messages.emit(UiText.Resource(R.string.library_artist_update_error))
            } finally {
                isMergeBusy.value = false
            }
        }
    }

    fun setTrackSort(sort: ArtistTrackSort) {
        viewModelScope.launch { setArtistTrackSort(sort) }
    }

    fun setAlbumSort(sort: ArtistAlbumSort) {
        viewModelScope.launch { setArtistAlbumSort(sort) }
    }

    fun playShuffled(queue: List<Track>) {
        viewModelScope.launch { playShuffledQueueUseCase(queue) }
    }

    fun playTrack(track: Track, queue: List<Track>) {
        viewModelScope.launch {
            playTrackUseCase(track, queue)
        }
    }
}

private fun List<Track>.filterArtistTracksByQuery(query: String): List<Track> {
    val normalizedQuery = query.trim()
    if (normalizedQuery.isEmpty()) return this
    return filter { track ->
        track.title.contains(normalizedQuery, ignoreCase = true) ||
            track.albumTitle?.contains(normalizedQuery, ignoreCase = true) == true
    }
}

private fun List<Album>.filterArtistAlbumsByQuery(query: String): List<Album> {
    val normalizedQuery = query.trim()
    if (normalizedQuery.isEmpty()) return this
    return filter { album ->
        album.title.contains(normalizedQuery, ignoreCase = true)
    }
}
