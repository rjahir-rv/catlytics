package com.catlytics.feature.library.impl.root

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.catlytics.core.designsystem.R
import com.catlytics.core.designsystem.component.CatlyticsLetterFastScroller
import com.catlytics.core.designsystem.component.TrackBadge
import com.catlytics.core.designsystem.component.sectionLetter
import com.catlytics.core.designsystem.modifier.pressScale
import com.catlytics.core.designsystem.modifier.staggeredEntrance
import com.catlytics.core.model.LibraryFolder
import com.catlytics.core.model.SortDirection
import com.catlytics.feature.library.impl.LibraryDimens
import com.catlytics.feature.library.impl.R as LibraryR
import com.catlytics.feature.library.impl.sortedFoldersByDirection

@Composable
internal fun LibraryFolderList(
    folders: List<LibraryFolder>,
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    sortDirection: SortDirection,
    onFolderVisibilityChange: (String, Boolean) -> Unit,
    onFolderSelected: (LibraryFolder) -> Unit,
    onAddToPlaylist: (LibraryFolder) -> Unit,
    bottomPadding: () -> Dp = { 0.dp },
    topPadding: Dp = 0.dp,
    animateEntrance: Boolean = false,
) {
    val sortedFolders: List<LibraryFolder> = remember(folders, sortDirection) {
        folders.sortedFoldersByDirection(sortDirection)
    }
    val summary = remember(folders) { folders.visibilitySummary() }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            state = state,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = LibraryDimens.ScreenPadding,
                top = topPadding + LibraryDimens.ContentTopSpacing,
                end = LibraryDimens.ScreenPadding,
                bottom = bottomPadding() + LibraryDimens.ContentBottomSpacing,
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(key = "folders-header") {
                FoldersHeaderCard(
                    summary = summary,
                    modifier = Modifier
                        .padding(bottom = 6.dp)
                        .staggeredEntrance(index = 0, animate = animateEntrance),
                )
            }
            itemsIndexed(
                items = sortedFolders,
                key = { _, folder -> "${sortDirection.name}:${folder.id}" },
            ) { index, folder ->
                FolderRow(
                    folder = folder,
                    onVisibilityChange = { visible ->
                        onFolderVisibilityChange(folder.id, visible)
                    },
                    onClick = { onFolderSelected(folder) },
                    onAddToPlaylist = { onAddToPlaylist(folder) },
                    modifier = Modifier.staggeredEntrance(
                        index = index + 1,
                        animate = animateEntrance && index < LibraryDimens.EntranceMaxStaggeredItems,
                    ),
                )
            }
        }
        CatlyticsLetterFastScroller(
            listState = state,
            itemCount = sortedFolders.size,
            headerItemCount = FOLDERS_HEADER_ITEM_COUNT,
            letterForVisibleTrackIndex = { index -> sortedFolders[index].name.sectionLetter() },
            contentPadding = PaddingValues(top = topPadding, bottom = bottomPadding()),
        )
    }
}

private const val FOLDERS_HEADER_ITEM_COUNT = 1

@Composable
private fun FoldersHeaderCard(
    summary: FolderVisibilitySummary,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme
    val contentColor = colorScheme.onSecondaryContainer
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = LibraryDimens.CardShape,
        color = colorScheme.secondaryContainer,
        contentColor = contentColor,
        border = BorderStroke(0.5.dp, contentColor.copy(alpha = 0.12f)),
    ) {
        Row(
            modifier = Modifier
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            colorScheme.secondaryContainer,
                            lerp(colorScheme.secondaryContainer, colorScheme.primaryContainer, 0.22f),
                        ),
                    ),
                )
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(contentColor.copy(alpha = 0.14f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_folder),
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = stringResource(LibraryR.string.library_folders_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(LibraryR.string.library_folders_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor.copy(alpha = 0.8f),
                )
                Text(
                    text = stringResource(
                        LibraryR.string.library_folders_summary,
                        pluralStringResource(
                            LibraryR.plurals.library_folders_visible_count,
                            summary.visible,
                            summary.visible,
                        ),
                        pluralStringResource(
                            LibraryR.plurals.library_folders_hidden_count,
                            summary.hidden,
                            summary.hidden,
                        ),
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun FolderRow(
    folder: LibraryFolder,
    onVisibilityChange: (Boolean) -> Unit,
    onClick: () -> Unit,
    onAddToPlaylist: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuExpanded by rememberSaveable(folder.id) { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val contentAlpha by animateFloatAsState(
        targetValue = if (folder.isVisible) 1f else 0.56f,
        label = "folderContentAlpha",
    )
    val containerColor by animateColorAsState(
        targetValue = if (folder.isVisible) {
            MaterialTheme.colorScheme.surfaceContainerLow
        } else {
            MaterialTheme.colorScheme.surfaceContainerLowest
        },
        label = "folderContainerColor",
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .pressScale(interactionSource)
            .clip(LibraryDimens.CardShape)
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                role = Role.Button,
                onClick = onClick,
            ),
        shape = LibraryDimens.CardShape,
        color = containerColor,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FolderIcon(
                isVisible = folder.isVisible,
                modifier = Modifier.alpha(contentAlpha),
            )
            FolderDetails(
                folder = folder,
                modifier = Modifier
                    .weight(1f)
                    .alpha(contentAlpha),
            )
            FolderVisibilityToggle(
                folder = folder,
                onVisibilityChange = onVisibilityChange,
            )
            FolderOptionsMenu(
                folder = folder,
                expanded = menuExpanded,
                onExpandedChange = { menuExpanded = it },
                onVisibilityChange = onVisibilityChange,
                onAddToPlaylist = onAddToPlaylist,
            )
        }
    }
}

@Composable
internal fun FolderIcon(
    isVisible: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.size(44.dp),
        shape = LibraryDimens.IconContainerShape,
        color = if (isVisible) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHighest
        },
        contentColor = if (isVisible) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(R.drawable.ic_folder),
                contentDescription = null,
            )
        }
    }
}

@Composable
private fun FolderDetails(
    folder: LibraryFolder,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = folder.name,
                modifier = Modifier.weight(1f, fill = false),
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!folder.isVisible) {
                TrackBadge(label = stringResource(LibraryR.string.library_folder_hidden_badge))
            }
        }
        Text(
            text = folder.path,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = pluralStringResource(
                LibraryR.plurals.library_track_count,
                folder.trackCount,
                folder.trackCount,
            ),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun FolderVisibilityToggle(
    folder: LibraryFolder,
    onVisibilityChange: (Boolean) -> Unit,
) {
    IconButton(onClick = { onVisibilityChange(!folder.isVisible) }) {
        Crossfade(
            targetState = folder.isVisible,
            animationSpec = tween(durationMillis = 180),
            label = "folderVisibilityToggle",
        ) { visible ->
            Icon(
                painter = painterResource(if (visible) R.drawable.ic_show else R.drawable.ic_hide),
                contentDescription = stringResource(
                    if (visible) {
                        LibraryR.string.library_action_hide_folder
                    } else {
                        LibraryR.string.library_action_show_folder
                    },
                ),
                tint = if (visible) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}

@Composable
private fun FolderOptionsMenu(
    folder: LibraryFolder,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onVisibilityChange: (Boolean) -> Unit,
    onAddToPlaylist: () -> Unit,
) {
    Box {
        IconButton(onClick = { onExpandedChange(true) }) {
            Icon(
                painter = painterResource(R.drawable.ic_options),
                contentDescription = stringResource(
                    LibraryR.string.library_folder_options_content_description,
                    folder.name,
                ),
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
            shape = RoundedCornerShape(20.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(LibraryR.string.library_action_add_to_playlist)) },
                onClick = { onExpandedChange(false); onAddToPlaylist() },
                leadingIcon = { Icon(painterResource(R.drawable.ic_playlist), null) },
            )
            FolderVisibilityMenuItem(
                label = stringResource(LibraryR.string.library_action_show_folder),
                iconRes = R.drawable.ic_show,
                selected = folder.isVisible,
                onClick = {
                    onExpandedChange(false)
                    onVisibilityChange(true)
                },
            )
            FolderVisibilityMenuItem(
                label = stringResource(LibraryR.string.library_action_hide_folder),
                iconRes = R.drawable.ic_hide,
                selected = !folder.isVisible,
                onClick = {
                    onExpandedChange(false)
                    onVisibilityChange(false)
                },
            )
        }
    }
}

@Composable
private fun FolderVisibilityMenuItem(
    label: String,
    iconRes: Int,
    selected: Boolean,
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        text = {
            Text(
                text = label,
                color = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
        },
        leadingIcon = {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (selected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainer
                },
                contentColor = if (selected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            ) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    modifier = Modifier.padding(4.dp),
                )
            }
        },
        onClick = onClick,
    )
}
