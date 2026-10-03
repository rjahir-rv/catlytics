package com.catlytics.feature.playlists.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.catlytics.core.domain.usecase.playback.ObservePlaybackStateUseCase
import com.catlytics.core.domain.usecase.playback.PlayShuffledQueueUseCase
import com.catlytics.core.domain.usecase.playback.PlayTrackUseCase
import com.catlytics.core.domain.usecase.playback.TogglePlaybackUseCase
import com.catlytics.core.domain.usecase.playlist.DeletePlaylistUseCase
import com.catlytics.core.domain.usecase.playlist.ObservePlaylistContentUseCase
import com.catlytics.core.domain.usecase.playlist.ObservePlaylistSortDirectionUseCase
import com.catlytics.core.domain.usecase.playlist.ObservePlaylistSummariesUseCase
import com.catlytics.core.domain.usecase.playlist.ObservePlaylistViewModeUseCase
import com.catlytics.core.domain.usecase.playlist.RenamePlaylistUseCase
import com.catlytics.core.domain.usecase.playlist.SetPlaylistCoverUseCase
import com.catlytics.core.domain.usecase.playlist.SetPlaylistSortDirectionUseCase
import com.catlytics.core.domain.usecase.playlist.SetPlaylistViewModeUseCase
import com.catlytics.core.model.PlaybackQueueSource
import com.catlytics.core.model.PlaybackStatus
import com.catlytics.core.model.PlaylistSummary
import com.catlytics.core.model.PlaylistViewMode
import com.catlytics.core.model.SortDirection
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal sealed interface PlaylistsUiState {
    data object Loading : PlaylistsUiState

    data class Content(val playlists: List<PlaylistSummary>) : PlaylistsUiState
}

internal data class ActivePlaylist(val id: String, val isPlaying: Boolean)

@HiltViewModel
internal class PlaylistsViewModel @Inject constructor(
    observePlaylistSummariesUseCase: ObservePlaylistSummariesUseCase,
    observePlaylistViewModeUseCase: ObservePlaylistViewModeUseCase,
    observePlaylistSortDirectionUseCase: ObservePlaylistSortDirectionUseCase,
    observePlaybackStateUseCase: ObservePlaybackStateUseCase,
    private val observePlaylistContentUseCase: ObservePlaylistContentUseCase,
    private val playTrackUseCase: PlayTrackUseCase,
    private val playShuffledQueueUseCase: PlayShuffledQueueUseCase,
    private val togglePlaybackUseCase: TogglePlaybackUseCase,
    private val renamePlaylistUseCase: RenamePlaylistUseCase,
    private val deletePlaylistUseCase: DeletePlaylistUseCase,
    private val setPlaylistViewModeUseCase: SetPlaylistViewModeUseCase,
    private val setPlaylistSortDirectionUseCase: SetPlaylistSortDirectionUseCase,
    private val setPlaylistCoverUseCase: SetPlaylistCoverUseCase,
) : ViewModel() {
    val uiState: StateFlow<PlaylistsUiState> = observePlaylistSummariesUseCase()
        .map<List<PlaylistSummary>, PlaylistsUiState>(PlaylistsUiState::Content)
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            PlaylistsUiState.Loading,
        )

    val activePlaylist: StateFlow<ActivePlaylist?> = observePlaybackStateUseCase()
        .map { state ->
            val source = state.queueSource as? PlaybackQueueSource.Playlist
                ?: return@map null
            ActivePlaylist(
                id = source.playlistId,
                isPlaying = state.status == PlaybackStatus.Playing ||
                    state.status == PlaybackStatus.Buffering,
            )
        }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val viewMode: StateFlow<PlaylistViewMode> = observePlaylistViewModeUseCase().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        PlaylistViewMode.List,
    )

    val sortDirection: StateFlow<SortDirection> = observePlaylistSortDirectionUseCase().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        SortDirection.Ascending,
    )

    fun rename(id: String, name: String) = viewModelScope.launch {
        val existing = (uiState.value as? PlaylistsUiState.Content)?.playlists.orEmpty()
        val isTaken = existing.any {
            it.playlist.id != id && it.playlist.name.equals(name.trim(), ignoreCase = true)
        }
        if (!isTaken) renamePlaylistUseCase(id, name)
    }

    fun delete(id: String) = viewModelScope.launch { deletePlaylistUseCase(id) }

    fun play(playlistId: String) = viewModelScope.launch {
        val tracks = observePlaylistContentUseCase(playlistId).first()?.tracks.orEmpty()
        val first = tracks.firstOrNull() ?: return@launch
        playTrackUseCase(first, tracks, PlaybackQueueSource.Playlist(playlistId))
    }

    fun playShuffled(playlistId: String) = viewModelScope.launch {
        val tracks = observePlaylistContentUseCase(playlistId).first()?.tracks.orEmpty()
        if (tracks.isEmpty()) return@launch
        playShuffledQueueUseCase(tracks, PlaybackQueueSource.Playlist(playlistId))
    }

    fun togglePlayback() = viewModelScope.launch { togglePlaybackUseCase() }

    fun setViewMode(mode: PlaylistViewMode) = viewModelScope.launch {
        setPlaylistViewModeUseCase(mode)
    }

    fun setSortDirection(direction: SortDirection) = viewModelScope.launch {
        setPlaylistSortDirectionUseCase(direction)
    }

    fun setCover(playlistId: String, artworkUri: String?) = viewModelScope.launch {
        setPlaylistCoverUseCase(playlistId, artworkUri)
    }
}
