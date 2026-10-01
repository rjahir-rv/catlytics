package com.catlytics.feature.playlists.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.catlytics.core.domain.usecase.playlist.CreatePlaylistUseCase
import com.catlytics.core.domain.usecase.playlist.ObservePlaylistsUseCase
import com.catlytics.core.model.Playlist
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
internal class CreatePlaylistViewModel @Inject constructor(
    observePlaylists: ObservePlaylistsUseCase,
    private val createPlaylist: CreatePlaylistUseCase,
) : ViewModel() {
    val playlists: StateFlow<List<Playlist>> = observePlaylists().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList(),
    )

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    fun create(
        name: String,
        description: String,
        artworkUri: String?,
        trackIds: List<String>,
        onCreated: (Playlist) -> Unit,
    ) {
        if (_isSaving.value || isPlaylistNameTaken(name, playlists.value)) return
        _isSaving.value = true
        viewModelScope.launch {
            try {
                onCreated(
                    createPlaylist(
                        name = name,
                        trackIds = trackIds,
                        artworkUri = artworkUri,
                        description = description,
                    ),
                )
            } finally {
                _isSaving.value = false
            }
        }
    }
}

internal fun isPlaylistNameTaken(name: String, playlists: List<Playlist>): Boolean {
    val normalized = name.trim()
    return normalized.isNotEmpty() &&
        playlists.any { it.name.equals(normalized, ignoreCase = true) }
}

//Siguiente número libre para el nombre por defecto
internal fun nextDefaultPlaylistNumber(
    playlists: List<Playlist>,
    formatName: (Int) -> String,
): Int {
    var number = playlists.size.coerceAtLeast(1)
    while (isPlaylistNameTaken(formatName(number), playlists)) number++
    return number
}
