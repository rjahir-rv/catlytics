package com.catlytics.feature.library.impl.root

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.catlytics.core.designsystem.R
import com.catlytics.core.model.LibraryFolder
import com.catlytics.core.model.SortDirection
import com.catlytics.feature.library.impl.sortedFoldersByDirection
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.launch

@Composable
internal fun LibraryFolderList(
    folders: List<LibraryFolder>,
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    sortDirection: SortDirection,
    onSortDirectionChange: (SortDirection) -> Unit,
    onFolderVisibilityChange: (String, Boolean) -> Unit,
    onFolderSelected: (LibraryFolder) -> Unit,
    onAddToPlaylist: (LibraryFolder) -> Unit,
    bottomPadding: () -> Dp = { 0.dp },
) {
    // Sort inside the leaf so the passed list (search filtered) is stable when only sort changes.
    val sortedFolders: List<LibraryFolder> = remember(folders, sortDirection) {
        folders.sortedFoldersByDirection(sortDirection)
    }
    val coroutineScope = rememberCoroutineScope()

    fun selectSortDirection(direction: SortDirection) {
        if (direction == sortDirection) {
            onSortDirectionChange(direction)
            return
        }
        coroutineScope.launch {
            state.scrollToItem(0)
            onSortDirectionChange(direction)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            state = state,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 20.dp,
                top = 56.dp,
                end = 20.dp,
                bottom = bottomPadding() + 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Carpetas musicales",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = "Elige qué carpetas forman parte de tu biblioteca.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(
            items = sortedFolders,
            key = { folder -> "${sortDirection.name}:${folder.id}" },
        ) { folder ->
            FolderRow(
                folder = folder,
                onVisibilityChange = { visible ->
                    onFolderVisibilityChange(folder.id, visible)
                },
                onClick = { onFolderSelected(folder) },
                onAddToPlaylist = { onAddToPlaylist(folder) },
            )
        }
    }

        // Sort button using ic_filter, same size as view toggle
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.End,
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
    val contentAlpha = if (folder.isVisible) 1f else 0.56f
    val shape = RoundedCornerShape(24.dp)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .then(
                if (folder.isVisible) {
                    Modifier
                } else {
                    Modifier.shadow(
                        elevation = 10.dp,
                        shape = shape,
                        ambientColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.28f),
                        spotColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.38f),
                    )
                },
            ),
        shape = shape,
        color = if (folder.isVisible) {
            MaterialTheme.colorScheme.surfaceContainerLow
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FolderIcon(isVisible = folder.isVisible)
            FolderDetails(
                folder = folder,
                modifier = Modifier
                    .weight(1f)
                    .alpha(contentAlpha),
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
private fun FolderIcon(
    isVisible: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.size(44.dp),
        shape = RoundedCornerShape(16.dp),
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
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = folder.path,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = folder.trackCount.trackCountLabel(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
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
                contentDescription = "Opciones de ${folder.name}",
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
            shape = RoundedCornerShape(20.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            DropdownMenuItem(
                text = { Text("Agregar a playlist") },
                onClick = { onExpandedChange(false); onAddToPlaylist() },
                leadingIcon = { Icon(painterResource(R.drawable.ic_playlist), null) },
            )
            FolderVisibilityMenuItem(
                label = "Mostrar carpeta",
                iconRes = R.drawable.ic_show,
                selected = folder.isVisible,
                onClick = {
                    onExpandedChange(false)
                    onVisibilityChange(true)
                },
            )
            FolderVisibilityMenuItem(
                label = "Ocultar carpeta",
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
                    MaterialTheme.colorScheme.onSecondaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
        },
        leadingIcon = {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (selected) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainer
                },
                contentColor = if (selected) {
                    MaterialTheme.colorScheme.onSecondaryContainer
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

private fun Int.trackCountLabel() = if (this == 1) "1 canción" else "$this canciones"
