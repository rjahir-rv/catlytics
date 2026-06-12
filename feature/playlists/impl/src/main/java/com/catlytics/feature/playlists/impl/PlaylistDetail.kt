package com.catlytics.feature.playlists.impl

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.catlytics.core.designsystem.R
import com.catlytics.core.domain.usecase.playback.PlayTrackUseCase
import com.catlytics.core.domain.usecase.playlist.ObservePlaylistContentUseCase
import com.catlytics.core.domain.usecase.playlist.RemoveTrackFromPlaylistUseCase
import com.catlytics.core.model.PlaylistContent
import com.catlytics.core.model.Track
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
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

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
internal class PlaylistDetailViewModel @Inject constructor(
    private val observeContent: ObservePlaylistContentUseCase,
    private val removeTrack: RemoveTrackFromPlaylistUseCase,
    private val playTrack: PlayTrackUseCase,
) : ViewModel() {
    private val playlistId = MutableStateFlow<String?>(null)
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

    fun open(id: String) {
        playlistId.value = id
    }

    fun remove(trackId: String) =
        viewModelScope.launch { playlistId.value?.let { removeTrack(it, trackId) } }

    fun play(track: Track, queue: List<Track>) = viewModelScope.launch { playTrack(track, queue) }
}

@Composable
internal fun PlaylistDetailRoute(
    playlistId: String,
    viewModel: PlaylistDetailViewModel = hiltViewModel(key = playlistId),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(playlistId) { viewModel.open(playlistId) }
    PlaylistDetailScreen(uiState, viewModel::play, viewModel::remove)
}

@Composable
private fun PlaylistDetailScreen(
    uiState: PlaylistDetailUiState,
    onPlay: (Track, List<Track>) -> Unit,
    onRemove: (String) -> Unit,
) {
    when (uiState) {
        PlaylistDetailUiState.Loading -> {
            Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }
            return
        }

        PlaylistDetailUiState.NotFound -> {
            Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) { Text("Playlist no disponible.") }
            return
        }

        is PlaylistDetailUiState.Success -> Unit
    }

    val content = uiState.content
    if (content.tracks.isEmpty()) {
        Box(
            Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) { Text("Esta playlist está vacía.") }
        return
    }
    LazyColumn(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        items(content.tracks, key = Track::id) { track ->
            var expanded by remember { mutableStateOf(false) }
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { onPlay(track, content.tracks) }
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(track.title, style = MaterialTheme.typography.titleMedium)
                    Text(track.artist.name, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Box {
                    IconButton(onClick = { expanded = true }) {
                        Icon(painterResource(R.drawable.ic_options), "Opciones de ${track.title}")
                    }
                    DropdownMenu(expanded, { expanded = false }) {
                        DropdownMenuItem(
                            text = { Text("Quitar de playlist") },
                            onClick = { expanded = false; onRemove(track.id) },
                            leadingIcon = { Icon(Icons.Default.Delete, null) },
                        )
                    }
                }
            }
        }
    }
}
