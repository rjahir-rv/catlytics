package com.catlytics.feature.playlists.impl

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.catlytics.core.designsystem.R
import com.catlytics.core.designsystem.theme.CatlyticsCorners
import com.catlytics.core.model.LIKED_PLAYLIST_ID
import com.catlytics.core.model.PlaylistSummary
import com.catlytics.feature.playlists.impl.R as PlaylistsR
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PlaylistActionsSheet(
    summary: PlaylistSummary,
    onDismiss: () -> Unit,
    onPlay: () -> Unit,
    onShuffle: () -> Unit,
    onRename: () -> Unit,
    onChangeCover: () -> Unit,
    onClearCover: () -> Unit,
    onDelete: () -> Unit,
) {
    val playlist = summary.playlist
    val isLiked = playlist.id == LIKED_PLAYLIST_ID
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    fun dismissThen(action: () -> Unit) {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            onDismiss()
            action()
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PlaylistCover(
                playlistId = playlist.id,
                name = playlist.name,
                artworkModel = playlist.artworkUri,
                mosaicArtworkUris = summary.coverArtworkUris,
                shape = CatlyticsCorners.Medium,
                modifier = Modifier.size(64.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = playlist.displayName(),
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = playlistSummaryLabel(summary.trackCount, summary.totalDurationMillis),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        Column(modifier = Modifier.padding(bottom = 16.dp)) {
            if (summary.trackCount > 0) {
                SheetAction(
                    iconRes = R.drawable.ic_play_fill,
                    label = stringResource(R.string.ds_action_play),
                    onClick = { dismissThen(onPlay) },
                )
                SheetAction(
                    iconRes = R.drawable.ic_shuffle,
                    label = stringResource(PlaylistsR.string.playlists_action_shuffle),
                    onClick = { dismissThen(onShuffle) },
                )
            }
            if (!isLiked) {
                SheetAction(
                    iconRes = R.drawable.ic_edit,
                    label = stringResource(PlaylistsR.string.playlists_action_rename),
                    onClick = { dismissThen(onRename) },
                )
                SheetAction(
                    iconRes = R.drawable.ic_album,
                    label = stringResource(PlaylistsR.string.playlists_change_cover),
                    onClick = { dismissThen(onChangeCover) },
                )
                if (playlist.artworkUri != null) {
                    SheetAction(
                        iconRes = R.drawable.ic_close,
                        label = stringResource(PlaylistsR.string.playlists_remove_cover),
                        onClick = { dismissThen(onClearCover) },
                    )
                }
                SheetAction(
                    iconRes = R.drawable.ic_delete,
                    label = stringResource(PlaylistsR.string.playlists_action_delete),
                    destructive = true,
                    onClick = { dismissThen(onDelete) },
                )
            }
        }
    }
}

@Composable
private fun SheetAction(
    iconRes: Int,
    label: String,
    onClick: () -> Unit,
    destructive: Boolean = false,
) {
    val contentColor = if (destructive) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    ListItem(
        headlineContent = { Text(label) },
        leadingContent = { Icon(painter = painterResource(iconRes), contentDescription = null) },
        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent,
            headlineColor = contentColor,
            leadingIconColor = contentColor,
        ),
        modifier = Modifier.clickable(role = Role.Button, onClick = onClick),
    )
}

/** Pide un nombre; marca el error en vivo cuando [isNameTaken] lo rechaza. */
@Composable
internal fun NameDialog(
    title: String,
    initialName: String,
    isNameTaken: (String) -> Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by remember(initialName) { mutableStateOf(initialName) }
    val taken = isNameTaken(name)
    val canConfirm = name.isNotBlank() && !taken
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                isError = taken,
                shape = CatlyticsCorners.Medium,
                label = { Text(stringResource(PlaylistsR.string.playlists_name_label)) },
                supportingText = if (taken) {
                    { Text(stringResource(PlaylistsR.string.create_playlist_name_taken)) }
                } else {
                    null
                },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(
                    onDone = { if (canConfirm) onConfirm(name.trim()) },
                ),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name.trim()) },
                enabled = canConfirm,
            ) { Text(stringResource(PlaylistsR.string.playlists_action_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(PlaylistsR.string.playlists_action_cancel))
            }
        },
    )
}

@Composable
internal fun DeletePlaylistDialog(
    playlistName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    message: String = stringResource(PlaylistsR.string.playlists_delete_message),
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                painter = painterResource(R.drawable.ic_delete),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
            )
        },
        title = { Text(stringResource(PlaylistsR.string.playlists_delete_title, playlistName)) },
        text = { Text(message) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            ) { Text(stringResource(PlaylistsR.string.playlists_action_delete)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(PlaylistsR.string.playlists_action_cancel))
            }
        },
    )
}
