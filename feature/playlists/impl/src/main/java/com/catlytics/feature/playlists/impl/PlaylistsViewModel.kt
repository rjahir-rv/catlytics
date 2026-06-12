package com.catlytics.feature.playlists.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.catlytics.core.domain.usecase.playlist.CreatePlaylistUseCase
import com.catlytics.core.domain.usecase.playlist.DeletePlaylistUseCase
import com.catlytics.core.domain.usecase.playlist.ObservePlaylistsUseCase
import com.catlytics.core.domain.usecase.playlist.RenamePlaylistUseCase
import com.catlytics.core.model.Playlist
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
internal class PlaylistsViewModel @Inject constructor(
    observePlaylistsUseCase: ObservePlaylistsUseCase,
    private val createPlaylistUseCase: CreatePlaylistUseCase,
    private val renamePlaylistUseCase: RenamePlaylistUseCase,
    private val deletePlaylistUseCase: DeletePlaylistUseCase,
) : ViewModel() {
    val playlists: StateFlow<List<Playlist>> = observePlaylistsUseCase().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList(),
    )

    fun create(name: String) = viewModelScope.launch {
        if (playlists.value.none { it.name.equals(name.trim(), ignoreCase = true) }) {
            createPlaylistUseCase(name)
        }
    }

    fun rename(id: String, name: String) = viewModelScope.launch {
        if (playlists.value.none {
                it.id != id && it.name.equals(
                    name.trim(),
                    ignoreCase = true
                )
            }) {
            renamePlaylistUseCase(id, name)
        }
    }

    fun delete(id: String) = viewModelScope.launch { deletePlaylistUseCase(id) }
}
