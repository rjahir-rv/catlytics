package com.catlytics.feature.playlists.impl

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.catlytics.core.designsystem.R
import com.catlytics.core.designsystem.theme.CatlyticsTheme
import com.catlytics.core.model.LIKED_PLAYLIST_ID
import com.catlytics.core.model.Playlist
import com.catlytics.core.model.PlaylistViewMode
import com.catlytics.core.model.SortDirection
import kotlinx.coroutines.launch

@Composable
internal fun PlaylistsScreen(
    playlists: List<Playlist>,
    viewMode: PlaylistViewMode,
    modifier: Modifier = Modifier,
    onViewModeChange: (PlaylistViewMode) -> Unit,
    onPlaylistSelected: (Playlist) -> Unit,
    onCreate: (String) -> Unit,
    onRename: (String, String) -> Unit,
    onDelete: (String) -> Unit,
    onSetCover: (String, String?) -> Unit,
    searchQuery: String = "",
    sortDirection: SortDirection = SortDirection.Ascending,
    onSortDirectionChange: (SortDirection) -> Unit = {}
) {
    var editor by remember { mutableStateOf<Playlist?>(null) }
    var creating by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<Playlist?>(null) }
    var pendingCoverForId by remember { mutableStateOf<String?>(null) }

    val coverPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri: Uri? ->
        val id = pendingCoverForId
        if (uri != null && id != null) {
            onSetCover(id, uri.toString())
        }
        pendingCoverForId = null
    }

    fun requestCoverChange(playlistId: String) {
        pendingCoverForId = playlistId
        coverPickerLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
        )
    }

    val filteredPlaylists = remember(playlists, searchQuery) {
        playlists.filterByQuery(searchQuery)
    }
    val listState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    val mosaicState = rememberSaveable(saver = LazyGridState.Saver) { LazyGridState() }
    val coroutineScope = rememberCoroutineScope()

    fun selectSortDirection(direction: SortDirection) {
        if (direction == sortDirection) {
            onSortDirectionChange(direction)
            return
        }
        coroutineScope.launch {
            when (viewMode) {
                PlaylistViewMode.List -> listState.scrollToItem(0)
                PlaylistViewMode.Mosaic -> mosaicState.scrollToItem(0)
            }
            onSortDirectionChange(direction)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (filteredPlaylists.isEmpty()) {
            if (searchQuery.isNotBlank()) {
                NoSearchResultsContent(modifier = Modifier.align(Alignment.Center))
            } else {
                EmptyPlaylistsContent(modifier = Modifier.align(Alignment.Center))
            }
        } else {
            when (viewMode) {
                PlaylistViewMode.List -> PlaylistList(
                    playlists = filteredPlaylists,
                    sortDirection = sortDirection,
                    state = listState,
                    onClick = onPlaylistSelected,
                    onRename = { editor = it },
                    onDelete = { deleting = it },
                    onChangeCover = ::requestCoverChange,
                    onClearCover = { id -> onSetCover(id, null) },
                    modifier = Modifier.fillMaxSize(),
                )
                PlaylistViewMode.Mosaic -> PlaylistMosaic(
                    playlists = filteredPlaylists,
                    sortDirection = sortDirection,
                    state = mosaicState,
                    onClick = onPlaylistSelected,
                    onRename = { editor = it },
                    onDelete = { deleting = it },
                    onChangeCover = ::requestCoverChange,
                    onClearCover = { id -> onSetCover(id, null) },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        // View mode + sort toggles (same level as artists)
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .fillMaxWidth()
                .padding(start = 12.dp, top = 8.dp, end = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            var expanded by remember { mutableStateOf(false) }
            Box {
                IconButton(onClick = { expanded = true }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_filter),
                        contentDescription = "Ordenar alfabéticamente",
                    )
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("A-Z") },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(R.drawable.ic_arrow_down),
                                contentDescription = null,
                                modifier = Modifier.graphicsLayer { rotationZ = 180f }
                            )
                        },
                        onClick = {
                            selectSortDirection(SortDirection.Ascending)
                            expanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Z-A") },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(R.drawable.ic_arrow_down),
                                contentDescription = null,
                            )
                        },
                        onClick = {
                            selectSortDirection(SortDirection.Descending)
                            expanded = false
                        }
                    )
                }
            }
            IconButton(
                onClick = {
                    onViewModeChange(
                        if (viewMode == PlaylistViewMode.List) PlaylistViewMode.Mosaic else PlaylistViewMode.List,
                    )
                },
            ) {
                val isList = viewMode == PlaylistViewMode.List
                Icon(
                    painter = painterResource(
                        if (isList) R.drawable.ic_grid else R.drawable.ic_list_shadow,
                    ),
                    contentDescription = if (isList) "Mostrar en mosaico" else "Mostrar en lista",
                )
            }
        }

        ExtendedFloatingActionButton(
            onClick = { creating = true },
            icon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_add),
                    contentDescription = "Agregar"
                )
            },
            text = { Text("Nueva playlist") },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
        )
    }
    if (creating) NameDialog("Nueva playlist", "", { creating = false }) {
        creating = false
        onCreate(it)
    }
    editor?.let { playlist ->
        NameDialog("Renombrar playlist", playlist.name, { editor = null }) {
            editor = null
            onRename(playlist.id, it)
        }
    }
    deleting?.let { playlist ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Eliminar ${playlist.name}") },
            text = { Text("Esta acción no eliminará las canciones del dispositivo.") },
            confirmButton = {
                TextButton(onClick = {
                    deleting = null; onDelete(playlist.id)
                }) { Text("Eliminar") }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun PlaylistList(
    playlists: List<Playlist>,
    modifier: Modifier = Modifier,
    sortDirection: SortDirection,
    state: LazyListState = rememberLazyListState(),
    onClick: (Playlist) -> Unit,
    onRename: (Playlist) -> Unit,
    onDelete: (Playlist) -> Unit,
    onChangeCover: (String) -> Unit,
    onClearCover: (String) -> Unit,

    ) {
    val sorted: List<Playlist> = remember(playlists, sortDirection) {
        playlists.sortedByDirection(sortDirection)
    }

    LazyColumn(
        state = state,
        modifier = modifier,
        contentPadding = PaddingValues(
            start = 20.dp,
            top = 56.dp,
            end = 20.dp,
            bottom = 104.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        items(sorted, key = { playlist -> "${sortDirection.name}:${playlist.id}" }) { playlist ->
            PlaylistListRow(
                playlist = playlist,
                onClick = { onClick(playlist) },
                onRename = { onRename(playlist) },
                onDelete = { onDelete(playlist) },
                onChangeCover = { onChangeCover(playlist.id) },
                onClearCover = { onClearCover(playlist.id) },
            )
        }
    }
}

@Composable
private fun PlaylistListRow(
    playlist: Playlist,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onChangeCover: () -> Unit,
    onClearCover: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = playlist.artworkUri,
            contentDescription = "Portada de ${playlist.name}",
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(16.dp)),
            placeholder = painterResource(R.drawable.placeholder_playlist),
            error = painterResource(R.drawable.placeholder_playlist),
            fallback = painterResource(R.drawable.placeholder_playlist),
            contentScale = ContentScale.Crop,
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp),
        ) {
            Text(
                text = playlist.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${playlist.trackIds.size} canciones",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        PlaylistActionsMenu(
            playlist = playlist,
            onRename = onRename,
            onDelete = onDelete,
            onChangeCover = onChangeCover,
            onClearCover = onClearCover,
        )
    }
}

@Composable
private fun PlaylistMosaic(
    playlists: List<Playlist>,
    sortDirection: SortDirection,
    modifier: Modifier = Modifier,
    state: LazyGridState = rememberLazyGridState(),
    onClick: (Playlist) -> Unit,
    onRename: (Playlist) -> Unit,
    onDelete: (Playlist) -> Unit,
    onChangeCover: (String) -> Unit,
    onClearCover: (String) -> Unit,

    ) {
    val sorted: List<Playlist> = remember(playlists, sortDirection) {
        playlists.sortedByDirection(sortDirection)
    }

    LazyVerticalGrid(
        state = state,
        columns = GridCells.Adaptive(minSize = 160.dp),
        modifier = modifier,
        contentPadding = PaddingValues(
            start = 20.dp,
            top = 56.dp,
            end = 20.dp,
            bottom = 104.dp,
        ),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        items(sorted, key = { playlist -> "${sortDirection.name}:${playlist.id}" }) { playlist ->
            PlaylistMosaicCard(
                playlist = playlist,
                onClick = { onClick(playlist) },
                onRename = { onRename(playlist) },
                onDelete = { onDelete(playlist) },
                onChangeCover = { onChangeCover(playlist.id) },
                onClearCover = { onClearCover(playlist.id) },
            )
        }
    }
}

@Composable
private fun PlaylistMosaicCard(
    playlist: Playlist,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onChangeCover: () -> Unit,
    onClearCover: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        AsyncImage(
            model = playlist.artworkUri,
            contentDescription = "Portada de ${playlist.name}",
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(20.dp)),
            placeholder = painterResource(R.drawable.placeholder_playlist),
            error = painterResource(R.drawable.placeholder_playlist),
            fallback = painterResource(R.drawable.placeholder_playlist),
            contentScale = ContentScale.Crop,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = playlist.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${playlist.trackIds.size} canciones",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            PlaylistActionsMenu(
                playlist = playlist,
                onRename = onRename,
                onDelete = onDelete,
                onChangeCover = onChangeCover,
                onClearCover = onClearCover,
            )
        }
    }
}

@Composable
private fun PlaylistActionsMenu(
    playlist: Playlist,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onChangeCover: () -> Unit,
    onClearCover: () -> Unit,
) {
    if (playlist.id == LIKED_PLAYLIST_ID) return

    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                painter = painterResource(R.drawable.ic_options),
                contentDescription = "Opciones de ${playlist.name}",
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            DropdownMenuItem(
                text = { Text("Renombrar") },
                onClick = {
                    expanded = false
                    onRename()
                },
                leadingIcon = { Icon(painterResource(R.drawable.ic_edit), null) },
            )
            DropdownMenuItem(
                text = { Text("Cambiar portada") },
                onClick = {
                    expanded = false
                    onChangeCover()
                },
                leadingIcon = { Icon(painterResource(R.drawable.ic_edit), null) },
            )
            if (playlist.artworkUri != null) {
                DropdownMenuItem(
                    text = { Text("Quitar portada") },
                    onClick = {
                        expanded = false
                        onClearCover()
                    },
                    leadingIcon = { Icon(painterResource(R.drawable.ic_delete), null) },
                )
            }
            DropdownMenuItem(
                text = { Text("Eliminar") },
                onClick = {
                    expanded = false
                    onDelete()
                },
                leadingIcon = { Icon(painterResource(R.drawable.ic_delete), null) },
            )
        }
    }
}

@Composable
private fun EmptyPlaylistsContent(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_playlist),
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "Aún no tienes playlists.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = "Toca el botón + para crear una y organizar tu música.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun NoSearchResultsContent(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_playlist),
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "No encontramos playlists que coincidan con tu búsqueda.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
internal fun NameDialog(
    title: String,
    initialName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember(initialName) { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                name,
                { name = it },
                singleLine = true,
                label = { Text("Nombre") })
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name.trim()) },
                enabled = name.isNotBlank()
            ) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

@Preview(showBackground = true, name = "List mode")
@Composable
private fun PlaylistsScreenListPreview() {
    CatlyticsTheme {
        PlaylistsScreen(
            playlists = listOf(
                Playlist(id = "p1", name = "Focus", trackIds = listOf("t1", "t2", "t3")),
                Playlist(id = "p2", name = "Gym", trackIds = listOf("t4")),
                Playlist(id = "p3", name = "Road trip 2025", trackIds = emptyList()),
            ),
            viewMode = PlaylistViewMode.List,
            onViewModeChange = {},
            onPlaylistSelected = {},
            onCreate = {},
            onRename = { _, _ -> },
            onDelete = {},
            onSetCover = { _, _ -> },
            searchQuery = "",
            sortDirection = SortDirection.Ascending,
            onSortDirectionChange = {},
        )
    }
}

@Preview(showBackground = true, name = "Mosaic mode")
@Composable
private fun PlaylistsScreenMosaicPreview() {
    CatlyticsTheme {
        PlaylistsScreen(
            playlists = listOf(
                Playlist(id = "p1", name = "Focus", trackIds = listOf("t1", "t2", "t3"), artworkUri = "file:///tmp/focus.jpg"),
                Playlist(id = "p2", name = "Gym", trackIds = listOf("t4")),
            ),
            viewMode = PlaylistViewMode.Mosaic,
            onViewModeChange = {},
            onPlaylistSelected = {},
            onCreate = {},
            onRename = { _, _ -> },
            onDelete = {},
            onSetCover = { _, _ -> },
            searchQuery = "",
            sortDirection = SortDirection.Ascending,
            onSortDirectionChange = {},
        )
    }
}

@Preview(showBackground = true, name = "Empty")
@Composable
private fun PlaylistsScreenEmptyPreview() {
    CatlyticsTheme {
        PlaylistsScreen(
            playlists = emptyList(),
            viewMode = PlaylistViewMode.List,
            onViewModeChange = {},
            onPlaylistSelected = {},
            onCreate = {},
            onRename = { _, _ -> },
            onDelete = {},
            onSetCover = { _, _ -> },
            searchQuery = "",
            sortDirection = SortDirection.Ascending,
            onSortDirectionChange = {},
        )
    }
}
