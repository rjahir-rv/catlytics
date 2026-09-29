package com.catlytics.feature.library.impl.artist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.catlytics.core.designsystem.text.UiText
import com.catlytics.core.domain.usecase.library.ObserveArtistContentUseCase
import com.catlytics.core.domain.usecase.library.ObserveArtistsUseCase
import com.catlytics.core.domain.usecase.library.ObserveArtistAliasesUseCase
import com.catlytics.core.domain.usecase.library.MergeArtistsUseCase
import com.catlytics.core.domain.usecase.library.UnmergeArtistUseCase
import com.catlytics.core.domain.usecase.playback.PlayTrackUseCase
import com.catlytics.core.model.Album
import com.catlytics.core.model.Track
import com.catlytics.core.model.Artist
import com.catlytics.core.model.artistIdentityKey
import com.catlytics.feature.library.impl.R
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
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
) : ViewModel() {
    private val artistId = MutableStateFlow<String?>(null)
    private val searchQuery = MutableStateFlow("")
    private val mergeDialog = MutableStateFlow<ArtistMergeDialog>(ArtistMergeDialog.Hidden)
    private val isMergeBusy = MutableStateFlow(false)
    private val _messages = MutableSharedFlow<UiText>(extraBufferCapacity = 1)
    val messages: SharedFlow<UiText> = _messages.asSharedFlow()

    private val artistData = combine(
        artistId
            .filterNotNull()
            .flatMapLatest(observeArtistContentUseCase::invoke),
        searchQuery,
        observeArtistsUseCase().catch { emit(emptyList()) },
        observeArtistAliasesUseCase().catch { emit(emptyList()) },
    ) { content, query, artists, aliases ->
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
        artistId.value = id
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
