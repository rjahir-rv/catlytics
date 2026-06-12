package com.catlytics.feature.playlists.impl

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.catlytics.core.designsystem.R
import com.catlytics.core.model.Playlist

@Composable
internal fun PlaylistsScreen(
    playlists: List<Playlist>,
    onPlaylistSelected: (Playlist) -> Unit,
    onCreate: (String) -> Unit,
    onRename: (String, String) -> Unit,
    onDelete: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var editor by remember { mutableStateOf<Playlist?>(null) }
    var creating by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<Playlist?>(null) }

    Box(modifier.fillMaxSize()) {
        if (playlists.isEmpty()) {
            Text(
                "Aún no tienes playlists.",
                modifier = Modifier.align(Alignment.Center),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(
                    start = 20.dp,
                    top = 12.dp,
                    end = 20.dp,
                    bottom = 104.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(playlists, key = Playlist::id) { playlist ->
                    PlaylistRow(
                        playlist = playlist,
                        onClick = { onPlaylistSelected(playlist) },
                        onRename = { editor = playlist },
                        onDelete = { deleting = playlist },
                    )
                }
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
private fun PlaylistRow(
    playlist: Playlist,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(playlist.name, style = MaterialTheme.typography.titleMedium)
            Text(
                "${playlist.trackIds.size} canciones",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Box {
            IconButton(onClick = { expanded = true }) {
                Icon(painterResource(R.drawable.ic_options), "Opciones de ${playlist.name}")
            }
            DropdownMenu(expanded, { expanded = false }) {
                DropdownMenuItem(
                    { Text("Renombrar") },
                    { expanded = false; onRename() },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_edit),
                            contentDescription = null,
                        )
                    })
                DropdownMenuItem(
                    { Text("Eliminar") },
                    { expanded = false; onDelete() },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_delete),
                            contentDescription = null,
                        )
                    })
            }
        }
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
