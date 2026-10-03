package com.catlytics.feature.playlists.impl

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.catlytics.core.designsystem.R
import com.catlytics.core.designsystem.theme.CatlyticsCorners
import com.catlytics.core.model.PlaylistViewMode
import com.catlytics.core.model.SortDirection
import com.catlytics.feature.playlists.impl.R as PlaylistsR

internal val PlaylistsControlsHeight = 64.dp

@Composable
internal fun MetaPill(text: String) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}

@Composable
internal fun PlaylistsControlsRow(
    sortDirection: SortDirection,
    viewMode: PlaylistViewMode,
    onSortSelected: (SortDirection) -> Unit,
    onViewModeChange: (PlaylistViewMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .height(PlaylistsControlsHeight),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SortChip(sortDirection = sortDirection, onSortSelected = onSortSelected)
        ViewModeSelector(viewMode = viewMode, onViewModeChange = onViewModeChange)
    }
}

@Composable
private fun SortChip(
    sortDirection: SortDirection,
    onSortSelected: (SortDirection) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        AssistChip(
            onClick = { expanded = true },
            label = { Text(sortDirection.label()) },
            leadingIcon = {
                Icon(
                    painter = painterResource(R.drawable.ic_filter),
                    contentDescription = stringResource(
                        PlaylistsR.string.playlists_sort_content_description,
                    ),
                    modifier = Modifier.size(18.dp),
                )
            },
            shape = CatlyticsCorners.Medium,
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            SortDirection.entries.forEach { direction ->
                DropdownMenuItem(
                    text = { Text(direction.label()) },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_down),
                            contentDescription = null,
                            modifier = if (direction == SortDirection.Ascending) {
                                Modifier.graphicsLayer { rotationZ = 180f }
                            } else {
                                Modifier
                            },
                        )
                    },
                    onClick = {
                        expanded = false
                        onSortSelected(direction)
                    },
                )
            }
        }
    }
}

@Composable
private fun SortDirection.label(): String = stringResource(
    if (this == SortDirection.Ascending) {
        PlaylistsR.string.playlists_sort_ascending
    } else {
        PlaylistsR.string.playlists_sort_descending
    },
)

@Composable
private fun ViewModeSelector(
    viewMode: PlaylistViewMode,
    onViewModeChange: (PlaylistViewMode) -> Unit,
) {
    val options = listOf(
        Triple(
            PlaylistViewMode.List,
            R.drawable.ic_list_shadow,
            PlaylistsR.string.playlists_show_list_content_description,
        ),
        Triple(
            PlaylistViewMode.Mosaic,
            R.drawable.ic_grid,
            PlaylistsR.string.playlists_show_grid_content_description,
        ),
    )
    SingleChoiceSegmentedButtonRow {
        options.forEachIndexed { index, (mode, iconRes, descriptionRes) ->
            SegmentedButton(
                selected = viewMode == mode,
                onClick = { onViewModeChange(mode) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
                icon = {},
                modifier = Modifier.width(56.dp),
                label = {
                    Icon(
                        painter = painterResource(iconRes),
                        contentDescription = stringResource(descriptionRes),
                        modifier = Modifier.size(20.dp),
                    )
                },
            )
        }
    }
}

@Composable
internal fun PlaylistsSkeleton(
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    rows: Int = SKELETON_ROWS,
) {
    val transition = rememberInfiniteTransition(label = "playlistsSkeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(SKELETON_PULSE_MILLIS), RepeatMode.Reverse),
        label = "playlistsSkeletonAlpha",
    )
    val block = MaterialTheme.colorScheme.surfaceContainerHigh
    Column(
        modifier = modifier
            .padding(contentPadding)
            .graphicsLayer { this.alpha = alpha },
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        repeat(rows) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CatlyticsCorners.Large)
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(64.dp)
                        .clip(CatlyticsCorners.Medium)
                        .background(block),
                )
                Column(
                    modifier = Modifier.padding(start = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        Modifier
                            .width(150.dp)
                            .height(16.dp)
                            .clip(CircleShape)
                            .background(block),
                    )
                    Box(
                        Modifier
                            .width(96.dp)
                            .height(12.dp)
                            .clip(CircleShape)
                            .background(block),
                    )
                }
            }
        }
    }
}

private const val SKELETON_ROWS = 6
private const val SKELETON_PULSE_MILLIS = 800
