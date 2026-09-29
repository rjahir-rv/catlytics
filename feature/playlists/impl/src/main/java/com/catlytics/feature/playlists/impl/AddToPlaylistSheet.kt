package com.catlytics.feature.playlists.impl

import android.content.res.Resources
import android.widget.Toast
import androidx.annotation.StringRes
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
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import coil3.compose.AsyncImage
import com.catlytics.core.designsystem.R
import com.catlytics.core.designsystem.text.UiText
import com.catlytics.core.designsystem.text.resolve
import com.catlytics.core.domain.usecase.playlist.AddToPlaylistUseCase
import com.catlytics.core.domain.usecase.playlist.CreatePlaylistUseCase
import com.catlytics.core.domain.usecase.playlist.ObservePlaylistsUseCase
import com.catlytics.core.domain.usecase.playlist.ResolvePlaylistSourcePreviewUseCase
import com.catlytics.core.model.LIKED_PLAYLIST_ID
import com.catlytics.core.model.Playlist
import com.catlytics.core.model.PlaylistSource
import com.catlytics.core.model.PlaylistSourceKind
import com.catlytics.core.model.PlaylistSourcePreview
import com.catlytics.feature.playlists.impl.R as PlaylistsR
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Playlist agregada, con su id para poder resolver el nombre de la playlist del sistema. */
data class PlaylistRef(
    val id: String,
    val name: String,
)

data class AddToPlaylistResult(
    val totalAdded: Int,
    val playlists: List<PlaylistRef>,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToPlaylistSheet(
    source: PlaylistSource,
    onDismiss: () -> Unit,
    excludedPlaylistIds: Set<String> = emptySet(),
    allowCreate: Boolean = true,
    viewModel: AddToPlaylistViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val resources = LocalResources.current
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
            if (allowCreate) {
                item {
                    ListItem(
                        headlineContent = {
                            Text(stringResource(PlaylistsR.string.playlists_new_playlist))
                        },
                        leadingContent = { Icon(Icons.Default.Add, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { creating = true },
                    )
                }
            }
            items(
                items = playlists.filterNot { it.id in excludedPlaylistIds },
                key = Playlist::id,
            ) { playlist ->
                val isSelected = playlist.id in selectedPlaylistIds
                val isFullyAdded = isPlaylistFullyAdded(playlist, sourceTrackIds)
                val pendingCount = pendingTrackCount(playlist, sourceTrackIds)
                ListItem(
                    headlineContent = {
                        Text(
                            text = playlist.displayName(),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    supportingContent = {
                        Text(
                            text = when {
                                isFullyAdded -> stringResource(PlaylistsR.string.playlists_already_added)
                                pendingCount < sourceTrackIds.size && sourceTrackIds.isNotEmpty() -> {
                                    pluralStringResource(
                                        PlaylistsR.plurals.add_to_playlist_pending_track_count,
                                        pendingCount,
                                        pendingCount,
                                    )
                                }
                                else -> pluralStringResource(
                                    PlaylistsR.plurals.playlists_track_count,
                                    playlist.trackIds.size,
                                    playlist.trackIds.size,
                                )
                            },
                        )
                    },
                    leadingContent = {
                        PlaylistCoverImage(
                            playlistId = playlist.id,
                            artworkUri = playlist.artworkUri,
                            name = playlist.displayName(),
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
                                    addToPlaylistToastMessage(it, resources).resolve(resources),
                                    Toast.LENGTH_SHORT,
                                ).show()
                            }
                            onDismiss()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(PlaylistsR.string.add_to_playlist_done))
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
    if (creating) {
        NameDialog(
            title = stringResource(PlaylistsR.string.playlists_new_playlist),
            initialName = "",
            onDismiss = { creating = false },
        ) { name ->
            creating = false
            viewModel.createPlaylistForSelection(name) { playlistId ->
                selectedPlaylistIds = selectedPlaylistIds + playlistId
            }
        }
    }
}

@Composable
private fun PlaylistCoverImage(
    playlistId: String,
    artworkUri: String?,
    name: String,
    modifier: Modifier = Modifier,
) {
    val coverPlaceholder = painterResource(
        if (playlistId == LIKED_PLAYLIST_ID) {
            R.drawable.placeholder_favorites
        } else {
            R.drawable.placeholder_playlist
        },
    )
    AsyncImage(
        model = artworkUri,
        contentDescription = stringResource(
            PlaylistsR.string.playlists_artwork_content_description,
            name,
        ),
        modifier = modifier
            .size(48.dp)
            .clip(RoundedCornerShape(12.dp)),
        placeholder = coverPlaceholder,
        error = coverPlaceholder,
        fallback = coverPlaceholder,
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
            contentDescription = title?.let {
                stringResource(PlaylistsR.string.playlists_artwork_content_description, it)
            },
            modifier = imageModifier,
            contentScale = ContentScale.Crop,
        )
    } else {
        AsyncImage(
            model = artworkUri,
            contentDescription = title?.let {
                stringResource(PlaylistsR.string.playlists_artwork_content_description, it)
            },
            modifier = imageModifier,
            placeholder = painterResource(R.drawable.placeholder_album),
            error = painterResource(R.drawable.placeholder_album),
            fallback = painterResource(R.drawable.placeholder_album),
            contentScale = ContentScale.Crop,
        )
    }
}

@StringRes
private fun PlaylistSourceKind.fallbackTitleRes(): Int = when (this) {
    PlaylistSourceKind.Track -> PlaylistsR.string.playlists_source_track
    PlaylistSourceKind.Album -> PlaylistsR.string.playlists_source_album
    PlaylistSourceKind.Artist -> PlaylistsR.string.playlists_source_artist
    PlaylistSourceKind.Folder -> PlaylistsR.string.playlists_source_folder
    PlaylistSourceKind.Collection -> PlaylistsR.string.add_to_playlist_title
}

@Composable
private fun AddToPlaylistHeader(
    preview: PlaylistSourcePreview?,
    modifier: Modifier = Modifier,
) {
    val title = preview?.let { it.title ?: stringResource(it.kind.fallbackTitleRes()) }
    val subtitle = preview?.albumCount
        ?.let { pluralStringResource(PlaylistsR.plurals.playlists_album_count, it, it) }
        ?: preview?.subtitle
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SourceCoverImage(
            artworkUri = preview?.artworkUri,
            title = title,
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title ?: stringResource(PlaylistsR.string.add_to_playlist_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            subtitle?.let { subtitle ->
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
                    text = pluralStringResource(
                        PlaylistsR.plurals.playlists_track_count,
                        count,
                        count,
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Icon(
            painter = painterResource(R.drawable.ic_check_list),
            contentDescription = stringResource(
                PlaylistsR.string.add_to_playlist_multiselect_content_description,
            ),
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp),
        )
    }
}

private fun addToPlaylistToastMessage(result: AddToPlaylistResult, resources: Resources): UiText {
    if (result.totalAdded == 0) {
        return UiText.Resource(PlaylistsR.string.add_to_playlist_toast_none)
    }
    return when (result.playlists.size) {
        0 -> UiText.Plural(PlaylistsR.plurals.add_to_playlist_toast_added, result.totalAdded)
        1 -> playlistToastMessage(
            playlist = result.playlists.first(),
            addedCount = result.totalAdded,
            resources = resources,
        )
        else -> UiText.Resource(
            PlaylistsR.string.add_to_playlist_toast_multiple,
            listOf(result.totalAdded, result.playlists.size),
        )
    }
}

private fun playlistToastMessage(
    playlist: PlaylistRef,
    addedCount: Int,
    resources: Resources,
): UiText {
    val playlistName = playlist.displayName(resources)
    return when (addedCount) {
        0 -> UiText.Resource(
            PlaylistsR.string.add_to_playlist_toast_playlist_none,
            listOf(playlistName),
        )
        else -> UiText.Plural(
            PlaylistsR.plurals.add_to_playlist_toast_playlist_added,
            addedCount,
            listOf(addedCount, playlistName),
        )
    }
}

private fun PlaylistRef.displayName(resources: Resources): String =
    if (id == LIKED_PLAYLIST_ID) {
        resources.getString(PlaylistsR.string.playlist_liked_name)
    } else {
        name
    }
