package com.catlytics.feature.library.impl.collection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.catlytics.core.designsystem.R
import com.catlytics.core.designsystem.component.CatlyticsEmptyState
import com.catlytics.core.designsystem.component.CatlyticsMenuEntry
import com.catlytics.core.designsystem.component.CatlyticsMenuItem
import com.catlytics.core.designsystem.component.CatlyticsOptionsMenu
import com.catlytics.core.designsystem.component.CatlyticsShuffleButton
import com.catlytics.core.model.Track
import com.catlytics.core.model.TrackSelectionAction
import com.catlytics.feature.library.impl.R as LibraryR

@Composable
internal fun CollectionPlaybackRow(
    tracks: List<Track>,
    onPlayShuffled: () -> Unit,
    onPlayAll: () -> Unit,
    menuEntries: List<CatlyticsMenuEntry>,
    menuContentDescription: String,
    modifier: Modifier = Modifier,
) {
    val enabled = tracks.isNotEmpty()
    Row(
        modifier = modifier.padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CatlyticsShuffleButton(onClick = onPlayShuffled, enabled = enabled)
        Button(onClick = onPlayAll, enabled = enabled, modifier = Modifier.height(48.dp)) {
            Icon(painterResource(R.drawable.ic_play), contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.ds_action_play))
        }
        CatlyticsOptionsMenu(
            entries = menuEntries,
            contentDescription = menuContentDescription,
            enabled = enabled,
        )
    }
}

@Composable
internal fun collectionMenuEntries(
    tracks: List<Track>,
    likedTrackIds: Set<String>,
    onTrackSelectionAction: (TrackSelectionAction) -> Unit,
    onAddToPlaylist: () -> Unit,
): List<CatlyticsMenuEntry> {
    val allLiked = tracks.isNotEmpty() && tracks.all { it.id in likedTrackIds }
    return listOf(
        CatlyticsMenuItem(
            text = stringResource(LibraryR.string.library_collection_play_next),
            icon = R.drawable.ic_skip_next,
            onClick = { onTrackSelectionAction(TrackSelectionAction.PlayNext(tracks)) },
        ),
        CatlyticsMenuItem(
            text = stringResource(LibraryR.string.library_collection_add_to_queue),
            icon = R.drawable.ic_add,
            onClick = { onTrackSelectionAction(TrackSelectionAction.AddToQueue(tracks)) },
        ),
        CatlyticsMenuItem(
            text = stringResource(LibraryR.string.library_action_add_to_playlist),
            icon = R.drawable.ic_add_playlist,
            onClick = onAddToPlaylist,
        ),
        CatlyticsMenuItem(
            text = stringResource(
                if (allLiked) LibraryR.string.library_collection_unlike else LibraryR.string.library_collection_like,
            ),
            icon = if (allLiked) R.drawable.ic_favorite_fill else R.drawable.ic_favorite,
            onClick = {
                onTrackSelectionAction(
                    if (allLiked) TrackSelectionAction.Unlike(tracks) else TrackSelectionAction.Like(tracks),
                )
            },
        ),
    )
}

internal data class SortOption<T>(val value: T, val label: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun <T> SortOptionsSheet(
    title: String,
    options: List<SortOption<T>>,
    selected: T,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
        )
        LazyColumn(modifier = Modifier.padding(bottom = 32.dp)) {
            items(options, key = { it.value.toString() }) { option ->
                val isSelected = option.value == selected
                ListItem(
                    headlineContent = { Text(option.label) },
                    leadingContent = { RadioButton(selected = isSelected, onClick = null) },
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = isSelected,
                            role = Role.RadioButton,
                            onClick = {
                                onSelect(option.value)
                                onDismiss()
                            },
                        ),
                )
            }
        }
    }
}

@Composable
internal fun LibraryMessage(message: String, modifier: Modifier = Modifier) {
    CatlyticsEmptyState(
        message = message,
        modifier = modifier,
        messageColor = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
