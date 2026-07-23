package com.catlytics.feature.playlists.impl

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.catlytics.core.designsystem.R
import com.catlytics.core.model.Track

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddTracksToPlaylistSheet(
    tracks: List<Track>,
    existingTrackIds: Set<String>,
    onDismiss: () -> Unit,
    onAdd: (List<String>) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var selectedIds by remember { mutableStateOf(emptySet<String>()) }
    val filteredTracks = remember(tracks, query) {
        tracks
            .filterPlaylistTracksByQuery(query)
            .alphabeticalOrder()
    }

    fun toggle(trackId: String) {
        selectedIds = toggleTrackSelection(selectedIds, trackId, existingTrackIds)
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Agregar canciones", style = MaterialTheme.typography.titleLarge)
            Text(
                "Selecciona canciones de tu biblioteca",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Buscar canciones o artistas") },
                leadingIcon = {
                    Icon(painterResource(R.drawable.ic_search), contentDescription = null)
                },
                trailingIcon = {
                    if (query.isNotBlank()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(painterResource(R.drawable.ic_close), "Limpiar búsqueda")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
            )

            if (filteredTracks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        if (tracks.isEmpty()) {
                            "No hay canciones disponibles."
                        } else {
                            "No encontramos canciones para esta búsqueda."
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 480.dp),
                ) {
                    items(filteredTracks, key = Track::id) { track ->
                        val alreadyAdded = track.id in existingTrackIds
                        val selected = track.id in selectedIds
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = !alreadyAdded) { toggle(track.id) }
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            AsyncImage(
                                model = track.artworkUri,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(10.dp)),
                                placeholder = painterResource(R.drawable.placeholder_album),
                                error = painterResource(R.drawable.placeholder_album),
                                fallback = painterResource(R.drawable.placeholder_album),
                                contentScale = ContentScale.Crop,
                            )
                            Column(Modifier.weight(1f)) {
                                Text(
                                    track.title,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    if (alreadyAdded) "Ya agregada" else track.artist.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Box(
                                modifier = Modifier.size(48.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Checkbox(
                                    checked = alreadyAdded || selected,
                                    onCheckedChange = { toggle(track.id) },
                                    enabled = !alreadyAdded,
                                )
                            }
                        }
                    }
                }
            }

            Button(
                onClick = {
                    onAdd(tracks.selectedTrackIdsInLibraryOrder(selectedIds))
                },
                enabled = selectedIds.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    if (selectedIds.isEmpty()) {
                        "Agregar canciones"
                    } else {
                        "Agregar (${selectedIds.size})"
                    },
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EditPlaylistSheet(
    name: String,
    description: String,
    artworkUri: String?,
    onNameChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onChooseArtwork: () -> Unit,
    onRemoveArtwork: () -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Editar playlist", style = MaterialTheme.typography.titleLarge)
            AsyncImage(
                model = artworkUri,
                contentDescription = "Vista previa de portada",
                modifier = Modifier
                    .size(144.dp)
                    .align(Alignment.CenterHorizontally)
                    .clip(RoundedCornerShape(20.dp)),
                placeholder = painterResource(R.drawable.placeholder_playlist),
                error = painterResource(R.drawable.placeholder_playlist),
                fallback = painterResource(R.drawable.placeholder_playlist),
                contentScale = ContentScale.Crop,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                TextButton(onClick = onChooseArtwork) { Text("Cambiar imagen") }
                if (artworkUri != null) {
                    TextButton(onClick = onRemoveArtwork) { Text("Quitar") }
                }
            }
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Nombre") },
                singleLine = true,
                isError = name.isBlank(),
                supportingText = if (name.isBlank()) {
                    { Text("El nombre no puede estar vacío") }
                } else {
                    null
                },
            )
            OutlinedTextField(
                value = description,
                onValueChange = onDescriptionChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Descripción") },
                minLines = 3,
                maxLines = 5,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
            ) {
                OutlinedButton(onClick = onDismiss) { Text("Cancelar") }
                Button(onClick = onSave, enabled = name.isNotBlank()) { Text("Guardar") }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
