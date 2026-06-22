package com.catlytics.feature.library.impl.root

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.catlytics.core.designsystem.R
import com.catlytics.core.model.Album
import com.catlytics.core.model.SortDirection
import com.catlytics.feature.library.impl.sortedAlbumsByDirection
import kotlinx.coroutines.launch

@Composable
internal fun LibraryAlbumGrid(
    albums: List<Album>,
    modifier : Modifier = Modifier,
    state: LazyGridState = rememberLazyGridState(),
    sortDirection: SortDirection,
    onSortDirectionChange: (SortDirection) -> Unit,
    onAlbumSelected: (Album) -> Unit,
    onAddToPlaylist: (Album) -> Unit,
    bottomPadding: () -> Dp = { 0.dp },
) {
    // Sort inside the leaf component so that the search-filtered input list stays stable.
    val sortedAlbums: List<Album> = remember(albums, sortDirection) {
        albums.sortedAlbumsByDirection(sortDirection)
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
        LazyVerticalGrid(
            state = state,
            columns = GridCells.Adaptive(minSize = 160.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 20.dp,
                top = 56.dp,
                end = 20.dp,
                bottom = bottomPadding() + 20.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            items(
                items = sortedAlbums,
                key = { album -> "${sortDirection.name}:${album.id}" },
            ) { album ->
                AlbumCard(
                    album = album,
                    onClick = { onAlbumSelected(album) },
                    onAddToPlaylist = { onAddToPlaylist(album) },
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
private fun AlbumCard(
    album: Album,
    onClick: () -> Unit,
    onAddToPlaylist: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        AsyncImage(
            model = album.artworkUri,
            contentDescription = "Portada de ${album.title}",
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(24.dp)),
            placeholder = painterResource(R.drawable.placeholder_album),
            error = painterResource(R.drawable.placeholder_album),
            fallback = painterResource(R.drawable.placeholder_album),
            contentScale = ContentScale.Crop,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = album.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = album.artist.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = album.trackCount.trackCountLabel(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = onAddToPlaylist) {
                Icon(painterResource(R.drawable.ic_options), "Opciones de ${album.title}")
            }
        }
    }
}

private fun Int.trackCountLabel() = if (this == 1) "1 canción" else "$this canciones"
