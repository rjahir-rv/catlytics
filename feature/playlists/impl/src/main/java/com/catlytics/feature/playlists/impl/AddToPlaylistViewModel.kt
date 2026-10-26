package com.catlytics.feature.playlists.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.catlytics.core.domain.usecase.playlist.AddToPlaylistUseCase
import com.catlytics.core.domain.usecase.playlist.ObservePlaylistsUseCase
import com.catlytics.core.domain.usecase.playlist.ResolvePlaylistSourcePreviewUseCase
import com.catlytics.core.model.PlaylistSource
import com.catlytics.core.model.PlaylistSourcePreview
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal data class PlaylistRef(
    val id: String,
    val name: String,
)

internal data class AddToPlaylistResult(
    val totalAdded: Int,
    val playlists: List<PlaylistRef>,
)

@HiltViewModel
internal class AddToPlaylistViewModel @Inject constructor(
    observePlaylists: ObservePlaylistsUseCase,
    private val addToPlaylist: AddToPlaylistUseCase,
    private val resolvePreview: ResolvePlaylistSourcePreviewUseCase,
) : ViewModel() {
    val playlists = observePlaylists().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList(),
    )

    private val _preview = MutableStateFlow<PlaylistSourcePreview?>(null)
    val preview: StateFlow<PlaylistSourcePreview?> = _preview.asStateFlow()

    fun loadPreview(source: PlaylistSource) {
        viewModelScope.launch {
            _preview.value = resolvePreview(source)
        }
    }

    fun applyToPlaylists(
        playlistIds: Set<String>,
        source: PlaylistSource,
        sourceTrackIds: List<String>,
        onComplete: (AddToPlaylistResult?) -> Unit,
    ) {
        viewModelScope.launch {
            if (playlistIds.isEmpty()) {
                onComplete(null)
                return@launch
            }
            val targetPlaylistIds = playlistIds.filter { playlistId ->
                val playlist = playlists.value.firstOrNull { it.id == playlistId } ?: return@filter false
                !isPlaylistFullyAdded(playlist, sourceTrackIds)
            }
            if (targetPlaylistIds.isEmpty()) {
                onComplete(AddToPlaylistResult(totalAdded = 0, playlists = emptyList()))
                return@launch
            }
            val addedByPlaylist = addToPlaylist.addToPlaylists(targetPlaylistIds, source)
            var totalAdded = 0
            val addedPlaylists = mutableListOf<PlaylistRef>()
            addedByPlaylist.forEach { (playlistId, addedCount) ->
                totalAdded += addedCount
                if (addedCount > 0) {
                    playlists.value.firstOrNull { it.id == playlistId }?.let { playlist ->
                        addedPlaylists += PlaylistRef(id = playlist.id, name = playlist.name)
                    }
                }
            }
            onComplete(AddToPlaylistResult(totalAdded = totalAdded, playlists = addedPlaylists))
        }
    }
}
