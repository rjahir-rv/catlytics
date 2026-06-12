package com.catlytics.feature.playlists.impl

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.catlytics.core.domain.usecase.playlist.AddToPlaylistUseCase
import com.catlytics.core.domain.usecase.playlist.CreatePlaylistUseCase
import com.catlytics.core.domain.usecase.playlist.ObservePlaylistsUseCase
import com.catlytics.core.model.Playlist
import com.catlytics.core.model.PlaylistSource
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class AddToPlaylistViewModel @Inject constructor(
    observePlaylists: ObservePlaylistsUseCase,
    private val createPlaylist: CreatePlaylistUseCase,
    private val addToPlaylist: AddToPlaylistUseCase,
) : ViewModel() {
    val playlists = observePlaylists().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList()
    )

    fun add(playlistId: String, source: PlaylistSource, done: () -> Unit) = viewModelScope.launch {
        addToPlaylist(playlistId, source)
        done()
    }

    fun createAndAdd(name: String, source: PlaylistSource, done: () -> Unit) =
        viewModelScope.launch {
            val playlist =
                playlists.value.firstOrNull { it.name.equals(name.trim(), ignoreCase = true) }
                    ?: createPlaylist(name)
            addToPlaylist(playlist.id, source)
            done()
        }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToPlaylistSheet(
    source: PlaylistSource,
    onDismiss: () -> Unit,
    viewModel: AddToPlaylistViewModel = hiltViewModel(),
) {
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    var creating by remember { mutableStateOf(false) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(bottom = 24.dp)) {
            ListItem(
                headlineContent = { Text("Nueva playlist") },
                leadingContent = { Icon(Icons.Default.Add, null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { creating = true },
            )
            LazyColumn {
                items(playlists, key = Playlist::id) { playlist ->
                    ListItem(
                        headlineContent = { Text(playlist.name) },
                        supportingContent = { Text("${playlist.trackIds.size} canciones") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.add(playlist.id, source, onDismiss)
                            },
                    )
                }
            }
        }
    }
    if (creating) NameDialog("Nueva playlist", "", { creating = false }) { name ->
        creating = false
        viewModel.createAndAdd(name, source, onDismiss)
    }
}
