package com.catlytics.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.catlytics.core.designsystem.R
import com.catlytics.core.model.Track

internal data class TrackOption(
    val text: String,
    val icon: Int,
    val enabled: Boolean = true,
    val onClick: () -> Unit,
)

internal data class TrackOptionsCallbacks(
    val onAddToPlaylist: () -> Unit,
    val onToggleLiked: () -> Unit,
    val onAddToQueue: () -> Unit,
    val onGoToAlbum: () -> Unit,
    val onGoToArtist: () -> Unit,
    val onRemoveFromPlaylist: () -> Unit,
)

internal fun buildTrackOptions(
    track: Track,
    isLiked: Boolean,
    canAddToQueue: Boolean,
    canRemoveFromPlaylist: Boolean,
    callbacks: TrackOptionsCallbacks,
): List<TrackOption> = buildList {
    add(
        TrackOption(
            text = "Agregar a playlist",
            icon = R.drawable.ic_add_playlist,
            onClick = callbacks.onAddToPlaylist,
        ),
    )
    add(
        TrackOption(
            text = if (isLiked) "Quitar de Tus me gusta" else "Guardar en Tus me gusta",
            icon = if (isLiked) R.drawable.ic_favorite_fill else R.drawable.ic_favorite,
            onClick = callbacks.onToggleLiked,
        ),
    )
    add(
        TrackOption(
            text = "Agregar a la cola",
            icon = R.drawable.ic_add,
            enabled = canAddToQueue,
            onClick = callbacks.onAddToQueue,
        ),
    )
    add(
        TrackOption(
            text = "Ir al álbum",
            icon = R.drawable.ic_album,
            enabled = track.albumId != null && track.albumTitle != null,
            onClick = callbacks.onGoToAlbum,
        ),
    )
    add(
        TrackOption(
            text = "Ir al artista",
            icon = R.drawable.ic_artist,
            onClick = callbacks.onGoToArtist,
        ),
    )
    if (canRemoveFromPlaylist) {
        add(
            TrackOption(
                text = "Quitar de playlist",
                icon = R.drawable.ic_delete,
                onClick = callbacks.onRemoveFromPlaylist,
            ),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TrackOptionsSheet(
    track: Track,
    isLiked: Boolean,
    canAddToQueue: Boolean,
    canRemoveFromPlaylist: Boolean,
    onDismiss: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onToggleLiked: () -> Unit,
    onAddToQueue: () -> Unit,
    onGoToAlbum: () -> Unit,
    onGoToArtist: () -> Unit,
    onRemoveFromPlaylist: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val options = buildTrackOptions(
        track = track,
        isLiked = isLiked,
        canAddToQueue = canAddToQueue,
        canRemoveFromPlaylist = canRemoveFromPlaylist,
        callbacks = TrackOptionsCallbacks(
            onAddToPlaylist = onAddToPlaylist,
            onToggleLiked = onToggleLiked,
            onAddToQueue = onAddToQueue,
            onGoToAlbum = onGoToAlbum,
            onGoToArtist = onGoToArtist,
            onRemoveFromPlaylist = onRemoveFromPlaylist,
        ),
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
    ) {
        Column(Modifier.padding(bottom = 24.dp)) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
            options.forEach { option ->
                TrackOptionItem(
                    text = option.text,
                    icon = option.icon,
                    enabled = option.enabled,
                    onClick = option.onClick,
                )
            }
        }
    }
}

@Composable
internal fun TrackOptionsDropdownMenu(
    track: Track,
    isLiked: Boolean,
    canAddToQueue: Boolean,
    onAddToPlaylist: () -> Unit,
    onToggleLiked: () -> Unit,
    onAddToQueue: () -> Unit,
    onGoToAlbum: () -> Unit,
    onGoToArtist: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val options = buildTrackOptions(
        track = track,
        isLiked = isLiked,
        canAddToQueue = canAddToQueue,
        canRemoveFromPlaylist = false,
        callbacks = TrackOptionsCallbacks(
            onAddToPlaylist = onAddToPlaylist,
            onToggleLiked = onToggleLiked,
            onAddToQueue = onAddToQueue,
            onGoToAlbum = onGoToAlbum,
            onGoToArtist = onGoToArtist,
            onRemoveFromPlaylist = {},
        ),
    )

    Box(modifier = modifier) {
        IconButton(onClick = { expanded = true }) {
            Icon(
                painter = painterResource(id = R.drawable.ic_options),
                contentDescription = "Opciones de ${track.title}",
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = RoundedCornerShape(20.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.text) },
                    onClick = {
                        expanded = false
                        option.onClick()
                    },
                    enabled = option.enabled,
                    leadingIcon = {
                        Icon(
                            painter = painterResource(option.icon),
                            contentDescription = null,
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun TrackOptionItem(
    text: String,
    icon: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val color = if (enabled) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = DisabledAlpha)
    }
    ListItem(
        headlineContent = {
            Text(
                text = text,
                color = color,
            )
        },
        leadingContent = {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = color,
            )
        },
        modifier = modifier.clickable(
            enabled = enabled,
            onClick = onClick,
        ),
    )
}

private const val DisabledAlpha = 0.38f