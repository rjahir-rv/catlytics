package com.catlytics.feature.playlists.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.catlytics.core.domain.usecase.library.ObserveLibraryUseCase
import com.catlytics.core.domain.usecase.playback.ObservePlaybackStateUseCase
import com.catlytics.core.domain.usecase.playback.PlayShuffledQueueUseCase
import com.catlytics.core.domain.usecase.playback.PlayTrackUseCase
import com.catlytics.core.domain.usecase.playback.TogglePlaybackUseCase
import com.catlytics.core.domain.usecase.playlist.AddToPlaylistUseCase
import com.catlytics.core.domain.usecase.playlist.DeletePlaylistUseCase
import com.catlytics.core.domain.usecase.playlist.ObservePlaylistContentUseCase
import com.catlytics.core.domain.usecase.playlist.RemoveTrackFromPlaylistUseCase
import com.catlytics.core.domain.usecase.playlist.ReorderPlaylistTracksUseCase
import com.catlytics.core.domain.usecase.playlist.SetPlaylistCoverUseCase
import com.catlytics.core.domain.usecase.playlist.UpdatePlaylistDetailsUseCase
import com.catlytics.core.model.PlaybackQueueSource
import com.catlytics.core.model.PlaybackState
import com.catlytics.core.model.PlaylistContent
import com.catlytics.core.model.PlaylistSource
import com.catlytics.core.model.Track
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal sealed interface PlaylistDetailUiState {
    data object Loading : PlaylistDetailUiState
    data object NotFound : PlaylistDetailUiState
    data class Success(val content: PlaylistContent) : PlaylistDetailUiState
}

internal sealed interface PlaylistDetailEffect {
    data class Message(val text: String) : PlaylistDetailEffect
    data object Deleted : PlaylistDetailEffect
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
internal class PlaylistDetailViewModel @Inject constructor(
    private val observeContent: ObservePlaylistContentUseCase,
    private val removeTrack: RemoveTrackFromPlaylistUseCase,
    private val playTrack: PlayTrackUseCase,
    private val playShuffledQueue: PlayShuffledQueueUseCase,
    observePlaybackState: ObservePlaybackStateUseCase,
    private val togglePlaybackUseCase: TogglePlaybackUseCase,
    private val updatePlaylistDetails: UpdatePlaylistDetailsUseCase,
    private val setPlaylistCover: SetPlaylistCoverUseCase,
    private val deletePlaylist: DeletePlaylistUseCase,
    private val reorderPlaylistTracks: ReorderPlaylistTracksUseCase,
    observeLibrary: ObserveLibraryUseCase,
    private val addToPlaylist: AddToPlaylistUseCase,
) : ViewModel() {
    private val playlistId = MutableStateFlow<String?>(null)
    private val _effects = MutableSharedFlow<PlaylistDetailEffect>()
    val effects = _effects.asSharedFlow()

    val uiState = playlistId
        .filterNotNull()
        .flatMapLatest(observeContent::invoke)
        .map { content ->
            content?.let(PlaylistDetailUiState::Success) ?: PlaylistDetailUiState.NotFound
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            PlaylistDetailUiState.Loading,
        )

    val playbackState = observePlaybackState().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        PlaybackState(),
    )

    val allTracks = observeLibrary().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList(),
    )

    fun open(id: String) {
        playlistId.value = id
    }

    fun remove(trackId: String) = viewModelScope.launch {
        playlistId.value?.let { removeTrack(it, trackId) }
    }

    fun play(track: Track, queue: List<Track>) = viewModelScope.launch {
        val source = playlistId.value
            ?.let(PlaybackQueueSource::Playlist)
            ?: PlaybackQueueSource.Static
        playTrack(track, queue, source)
    }

    fun playShuffled(queue: List<Track>) = viewModelScope.launch {
        val source = playlistId.value
            ?.let(PlaybackQueueSource::Playlist)
            ?: PlaybackQueueSource.Static
        playShuffledQueue(queue, source)
    }

    fun togglePlayback() = viewModelScope.launch { togglePlaybackUseCase() }

    fun saveDetails(
        name: String,
        description: String,
        artworkUri: String?,
        artworkChanged: Boolean,
        onSaved: () -> Unit,
    ) = launchOperation(onSuccess = onSaved) { id ->
        updatePlaylistDetails(id, name, description)
        if (artworkChanged) setPlaylistCover(id, artworkUri)
    }

    fun saveOrder(trackIds: List<String>, onSaved: () -> Unit = {}) =
        launchOperation(onSuccess = onSaved) { id -> reorderPlaylistTracks(id, trackIds) }

    fun delete() = launchOperation(onSuccess = {
        viewModelScope.launch { _effects.emit(PlaylistDetailEffect.Deleted) }
    }) { id ->
        deletePlaylist(id)
    }

    fun addTracks(trackIds: List<String>, onAdded: () -> Unit) = viewModelScope.launch {
        val id = playlistId.value ?: return@launch
        try {
            val added = addToPlaylist(
                playlistId = id,
                source = PlaylistSource.TrackCollectionSource(
                    title = "Canciones seleccionadas",
                    artworkUri = null,
                    trackIds = trackIds,
                ),
            )
            _effects.emit(
                PlaylistDetailEffect.Message(
                    if (added == 1) "1 canción agregada" else "$added canciones agregadas",
                ),
            )
            onAdded()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            _effects.emit(
                PlaylistDetailEffect.Message(
                    error.message ?: "No se pudieron agregar las canciones.",
                ),
            )
        }
    }

    private fun launchOperation(
        onSuccess: () -> Unit = {},
        operation: suspend (String) -> Unit,
    ) = viewModelScope.launch {
        val id = playlistId.value ?: return@launch
        try {
            operation(id)
            onSuccess()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            _effects.emit(
                PlaylistDetailEffect.Message(
                    error.message ?: "No se pudo completar la operación.",
                ),
            )
        }
    }
}
