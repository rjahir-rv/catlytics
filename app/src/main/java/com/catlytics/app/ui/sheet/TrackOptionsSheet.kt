package com.catlytics.app.ui.sheet

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.catlytics.app.R as AppR
import com.catlytics.core.designsystem.R
import com.catlytics.core.designsystem.component.CatlyticsSheetAction
import com.catlytics.core.designsystem.text.displayTrackTitle
import com.catlytics.core.model.Track

internal data class TrackOption(
    @param:StringRes val textRes: Int,
    val icon: Int,
    val enabled: Boolean = true,
    val onClick: () -> Unit,
)

internal data class TrackOptionsCallbacks(
    val onAddToPlaylist: () -> Unit,
    val onToggleLiked: () -> Unit,
    val onPlayNext: () -> Unit,
    val onAddToQueue: () -> Unit,
    val onGoToAlbum: () -> Unit,
    val onGoToArtist: () -> Unit,
    val onRemoveFromPlaylist: () -> Unit,
    val onEditMetadata: (() -> Unit)? = null,
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
            textRes = AppR.string.app_track_option_add_to_playlist,
            icon = R.drawable.ic_add_playlist,
            onClick = callbacks.onAddToPlaylist,
        ),
    )
    add(
        TrackOption(
            textRes = if (isLiked) {
                AppR.string.app_track_option_remove_from_liked
            } else {
                AppR.string.app_track_option_save_to_liked
            },
            icon = if (isLiked) R.drawable.ic_favorite_fill else R.drawable.ic_favorite,
            onClick = callbacks.onToggleLiked,
        ),
    )
    add(
        TrackOption(
            textRes = AppR.string.app_track_option_play_next,
            icon = R.drawable.ic_skip_next,
            enabled = canAddToQueue,
            onClick = callbacks.onPlayNext,
        ),
    )
    add(
        TrackOption(
            textRes = AppR.string.app_track_option_add_to_queue,
            icon = R.drawable.ic_add,
            enabled = canAddToQueue,
            onClick = callbacks.onAddToQueue,
        ),
    )
    add(
        TrackOption(
            textRes = AppR.string.app_track_option_go_to_album,
            icon = R.drawable.ic_album,
            enabled = track.albumId != null && track.albumTitle != null,
            onClick = callbacks.onGoToAlbum,
        ),
    )
    add(
        TrackOption(
            textRes = AppR.string.app_track_option_go_to_artist,
            icon = R.drawable.ic_artist,
            onClick = callbacks.onGoToArtist,
        ),
    )
    callbacks.onEditMetadata?.let { onEditMetadata ->
        add(
            TrackOption(
                textRes = AppR.string.app_track_option_edit_metadata,
                icon = R.drawable.ic_edit,
                onClick = onEditMetadata,
            ),
        )
    }
    if (canRemoveFromPlaylist) {
        add(
            TrackOption(
                textRes = AppR.string.app_track_option_remove_from_playlist,
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
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onGoToAlbum: () -> Unit,
    onGoToArtist: () -> Unit,
    onRemoveFromPlaylist: () -> Unit,
    onEditMetadata: () -> Unit,
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
            onPlayNext = onPlayNext,
            onAddToQueue = onAddToQueue,
            onGoToAlbum = onGoToAlbum,
            onGoToArtist = onGoToArtist,
            onRemoveFromPlaylist = onRemoveFromPlaylist,
            onEditMetadata = onEditMetadata,
        ),
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
    ) {
        Column(Modifier.padding(bottom = 24.dp)) {
            Text(
                text = track.title.displayTrackTitle(),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
            options.forEach { option ->
                CatlyticsSheetAction(
                    label = stringResource(option.textRes),
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
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onGoToAlbum: () -> Unit,
    onGoToArtist: () -> Unit,
    modifier: Modifier = Modifier,
    onEditMetadata: (() -> Unit)? = null,
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
            onPlayNext = onPlayNext,
            onAddToQueue = onAddToQueue,
            onGoToAlbum = onGoToAlbum,
            onGoToArtist = onGoToArtist,
            onRemoveFromPlaylist = {},
            onEditMetadata = onEditMetadata,
        ),
    )

    Box(modifier = modifier) {
        IconButton(onClick = { expanded = true }) {
            Icon(
                painter = painterResource(id = R.drawable.ic_options),
                contentDescription = stringResource(
                    R.string.ds_options_content_description,
                    track.title.displayTrackTitle(),
                ),
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
                    text = { Text(stringResource(option.textRes)) },
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

