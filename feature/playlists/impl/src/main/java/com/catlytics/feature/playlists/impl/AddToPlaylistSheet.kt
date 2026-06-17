package com.catlytics.feature.playlists.impl

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon

import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import coil3.compose.AsyncImage
import com.catlytics.core.designsystem.R
import com.catlytics.core.domain.usecase.playlist.AddToPlaylistUseCase
import com.catlytics.core.domain.usecase.playlist.CreatePlaylistUseCase
import com.catlytics.core.domain.usecase.playlist.ObservePlaylistsUseCase
import com.catlytics.core.domain.usecase.playlist.ResolvePlaylistSourcePreviewUseCase
import com.catlytics.core.model.Playlist
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

data class AddToPlaylistResult(
    val totalAdded: Int,
    val playlistNames: List<String>,
)

internal fun pendingTrackCount(playlist: Playlist, sourceTrackIds: List<String>): Int =
    sourceTrackIds.count { it !in playlist.trackIds }

internal fun isPlaylistFullyAdded(playlist: Playlist, sourceTrackIds: List<String>): Boolean =
    sourceTrackIds.isNotEmpty() && pendingTrackCount(playlist, sourceTrackIds) == 0

internal fun togglePlaylistSelectionState(
    selectedIds: Set<String>,
    playlistId: String,
): Set<String> = if (playlistId in selectedIds) {
    selectedIds - playlistId
} else {
    selectedIds + playlistId
}

@HiltViewModel
class AddToPlaylistViewModel @Inject constructor(
    observePlaylists: ObservePlaylistsUseCase,
    private val createPlaylistUseCase: CreatePlaylistUseCase,
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

    fun createPlaylistForSelection(name: String, onCreated: (String) -> Unit) {
        viewModelScope.launch {
            val playlist = playlists.value.firstOrNull { it.name.equals(name.trim(), ignoreCase = true) }
                ?: createPlaylistUseCase(name)
            onCreated(playlist.id)
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
                onComplete(AddToPlaylistResult(totalAdded = 0, playlistNames = emptyList()))
                return@launch
            }
            val addedByPlaylist = addToPlaylist.addToPlaylists(targetPlaylistIds, source)
            var totalAdded = 0
            val playlistNames = mutableListOf<String>()
            addedByPlaylist.forEach { (playlistId, addedCount) ->
                totalAdded += addedCount
                if (addedCount > 0) {
                    val playlistName = playlists.value.firstOrNull { it.id == playlistId }?.name
                    if (playlistName != null) {
                        playlistNames += playlistName
                    }
                }
            }
            onComplete(AddToPlaylistResult(totalAdded = totalAdded, playlistNames = playlistNames))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToPlaylistSheet(
    source: PlaylistSource,
    onDismiss: () -> Unit,
    viewModel: AddToPlaylistViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val preview by viewModel.preview.collectAsStateWithLifecycle()
    val sourceTrackIds = preview?.trackIds.orEmpty()
    var selectedPlaylistIds by remember { mutableStateOf(emptySet<String>()) }
    var creating by remember { mutableStateOf(false) }

    LaunchedEffect(source) {
        viewModel.loadPreview(source)
    }

    fun togglePlaylistSelection(playlistId: String) {
        val playlist = playlists.firstOrNull { it.id == playlistId } ?: return
        if (isPlaylistFullyAdded(playlist, sourceTrackIds)) return
        selectedPlaylistIds = togglePlaylistSelectionState(
            selectedIds = selectedPlaylistIds,
            playlistId = playlistId,
        )
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            item {
                AddToPlaylistHeader(preview = preview)
            }
            item {
                ListItem(
                    headlineContent = { Text("Nueva playlist") },
                    leadingContent = { Icon(Icons.Default.Add, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { creating = true },
                )
            }
            items(playlists, key = Playlist::id) { playlist ->
                val isSelected = playlist.id in selectedPlaylistIds
                val isFullyAdded = isPlaylistFullyAdded(playlist, sourceTrackIds)
                val pendingCount = pendingTrackCount(playlist, sourceTrackIds)
                ListItem(
                    headlineContent = {
                        Text(
                            text = playlist.name,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    supportingContent = {
                        Text(
                            text = when {
                                isFullyAdded -> "Ya agregada"
                                pendingCount < sourceTrackIds.size && sourceTrackIds.isNotEmpty() -> {
                                    "$pendingCount canciones nuevas"
                                }
                                else -> "${playlist.trackIds.size} canciones"
                            },
                        )
                    },
                    leadingContent = {
                        PlaylistCoverImage(
                            artworkUri = playlist.artworkUri,
                            name = playlist.name,
                        )
                    },
                    trailingContent = {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = if (isFullyAdded) {
                                null
                            } else {
                                { togglePlaylistSelection(playlist.id) }
                            },
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !isFullyAdded) {
                            togglePlaylistSelection(playlist.id)
                        },
                )
            }
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        viewModel.applyToPlaylists(
                            playlistIds = selectedPlaylistIds,
                            source = source,
                            sourceTrackIds = sourceTrackIds,
                        ) { result ->
                            result?.let {
                                Toast.makeText(
                                    context,
                                    addToPlaylistToastMessage(it),
                                    Toast.LENGTH_SHORT,
                                ).show()
                            }
                            onDismiss()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Hecho")
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
    if (creating) {
        NameDialog("Nueva playlist", "", { creating = false }) { name ->
            creating = false
            viewModel.createPlaylistForSelection(name) { playlistId ->
                selectedPlaylistIds = selectedPlaylistIds + playlistId
            }
        }
    }
}

@Composable
private fun PlaylistCoverImage(
    artworkUri: String?,
    name: String,
    modifier: Modifier = Modifier,
) {
    AsyncImage(
        model = artworkUri,
        contentDescription = "Portada de $name",
        modifier = modifier
            .size(48.dp)
            .clip(RoundedCornerShape(12.dp)),
        placeholder = painterResource(R.drawable.placeholder_playlist),
        error = painterResource(R.drawable.placeholder_playlist),
        fallback = painterResource(R.drawable.placeholder_playlist),
        contentScale = ContentScale.Crop,
    )
}

@Composable
private fun SourceCoverImage(
    artworkUri: String?,
    title: String?,
    modifier: Modifier = Modifier,
) {
    val imageModifier = modifier
        .size(72.dp)
        .clip(RoundedCornerShape(16.dp))

    if (artworkUri == null) {
        Image(
            painter = painterResource(R.drawable.placeholder_album),
            contentDescription = title?.let { "Portada de $it" },
            modifier = imageModifier,
            contentScale = ContentScale.Crop,
        )
    } else {
        AsyncImage(
            model = artworkUri,
            contentDescription = title?.let { "Portada de $it" },
            modifier = imageModifier,
            placeholder = painterResource(R.drawable.placeholder_album),
            error = painterResource(R.drawable.placeholder_album),
            fallback = painterResource(R.drawable.placeholder_album),
            contentScale = ContentScale.Crop,
        )
    }
}

@Composable
private fun AddToPlaylistHeader(
    preview: PlaylistSourcePreview?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SourceCoverImage(
            artworkUri = preview?.artworkUri,
            title = preview?.title,
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = preview?.title ?: "Agregar a playlist",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            preview?.subtitle?.let { subtitle ->
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            preview?.itemCount?.takeIf { it > 0 }?.let { count ->
                Text(
                    text = count.trackCountLabel(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Icon(
            painter = painterResource(R.drawable.ic_check_list),
            contentDescription = "Selección múltiple",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp),
        )
    }
}

private fun Int.trackCountLabel() = if (this == 1) "1 canción" else "$this canciones"

private fun addToPlaylistToastMessage(result: AddToPlaylistResult): String {
    if (result.totalAdded == 0) {
        return "No se agregaron canciones nuevas"
    }
    return when (result.playlistNames.size) {
        0 -> when (result.totalAdded) {
            1 -> "Canción agregada"
            else -> "${result.totalAdded} canciones agregadas"
        }
        1 -> playlistToastMessage(
            playlistName = result.playlistNames.first(),
            addedCount = result.totalAdded,
        )
        else -> "${result.totalAdded} canciones agregadas a ${result.playlistNames.size} playlists"
    }
}

private fun playlistToastMessage(playlistName: String, addedCount: Int): String =
    when (addedCount) {
        0 -> "No se agregaron canciones nuevas a $playlistName"
        1 -> "Canción agregada a $playlistName"
        else -> "$addedCount canciones agregadas a $playlistName"
    }