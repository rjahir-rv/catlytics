package com.catlytics.feature.library.impl.root

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import com.catlytics.core.model.ArtistSummary
import com.catlytics.core.model.ArtistViewMode
import com.catlytics.core.model.SortDirection
import com.catlytics.feature.library.impl.sortedArtistsByDirection
import kotlinx.coroutines.launch

@Composable
internal fun LibraryArtistCollection(
    artists: List<ArtistSummary>,
    modifier: Modifier = Modifier,
    viewMode: ArtistViewMode,
    onViewModeChange: (ArtistViewMode) -> Unit,
    sortDirection: SortDirection,
    onSortDirectionChange: (SortDirection) -> Unit,
    listState: LazyListState = rememberLazyListState(),
    gridState: LazyGridState = rememberLazyGridState(),
    onArtistSelected: (ArtistSummary) -> Unit,
    onAddToPlaylist: (ArtistSummary) -> Unit,
    bottomPadding: () -> Dp = { 0.dp },
) {
    // Sort inside so the input list is stable on sort-only changes.
    val sortedArtists: List<ArtistSummary> = remember(artists, sortDirection) {
        artists.sortedArtistsByDirection(sortDirection)
    }
    val coroutineScope = rememberCoroutineScope()

    fun selectSortDirection(direction: SortDirection) {
        if (direction == sortDirection) {
            onSortDirectionChange(direction)
            return
        }
        coroutineScope.launch {
            when (viewMode) {
                ArtistViewMode.List -> listState.scrollToItem(0)
                ArtistViewMode.Grid -> gridState.scrollToItem(0)
            }
            onSortDirectionChange(direction)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        when (viewMode) {
            ArtistViewMode.List -> ArtistList(
                artists = sortedArtists,
                sortDirection = sortDirection,
                state = listState,
                onArtistSelected = onArtistSelected,
                onAddToPlaylist = onAddToPlaylist,
                bottomPadding = bottomPadding,
            )
            ArtistViewMode.Grid -> ArtistGrid(
                artists = sortedArtists,
                sortDirection = sortDirection,
                state = gridState,
                onArtistSelected = onArtistSelected,
                onAddToPlaylist = onAddToPlaylist,
                bottomPadding = bottomPadding,
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            // Sort button using ic_filter (same size as view toggle)
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
            IconButton(
                onClick = {
                    onViewModeChange(
                        if (viewMode == ArtistViewMode.List) ArtistViewMode.Grid
                        else ArtistViewMode.List,
                    )
                },
            ) {
                val isList = viewMode == ArtistViewMode.List
                Icon(
                    painter = painterResource(
                        if (isList) R.drawable.ic_grid else R.drawable.ic_list_shadow,
                    ),
                    contentDescription = if (isList) {
                        "Mostrar artistas en mosaico"
                    } else {
                        "Mostrar artistas en lista"
                    },
                )
            }
        }
    }
}

@Composable
private fun ArtistList(
    artists: List<ArtistSummary>,
    sortDirection: SortDirection,
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    onArtistSelected: (ArtistSummary) -> Unit,
    onAddToPlaylist: (ArtistSummary) -> Unit,
    bottomPadding: () -> Dp = { 0.dp },
) {
    LazyColumn(
        state = state,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 20.dp,
            top = 56.dp,
            end = 20.dp,
            bottom = bottomPadding() + 8.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(items = artists, key = { artist -> "${sortDirection.name}:${artist.artist.id}" }) { artist ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onArtistSelected(artist) }
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ArtistImage(
                    artist = artist,
                    modifier = Modifier.size(64.dp),
                )
                ArtistText(
                    artist = artist,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { onAddToPlaylist(artist) }) {
                    Icon(painterResource(R.drawable.ic_options), "Opciones de ${artist.artist.name}")
                }
            }
        }
    }
}

@Composable
private fun ArtistGrid(
    artists: List<ArtistSummary>,
    modifier: Modifier = Modifier,
    sortDirection: SortDirection,
    state: LazyGridState = rememberLazyGridState(),
    onArtistSelected: (ArtistSummary) -> Unit,
    onAddToPlaylist: (ArtistSummary) -> Unit,
    bottomPadding: () -> Dp = { 0.dp },
) {
    LazyVerticalGrid(
        state = state,
        columns = GridCells.Adaptive(minSize = 144.dp),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 20.dp,
            top = 56.dp,
            end = 20.dp,
            bottom = bottomPadding() + 8.dp,
        ),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        items(items = artists, key = { artist -> "${sortDirection.name}:${artist.artist.id}" }) { artist ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onArtistSelected(artist) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ArtistImage(
                    artist = artist,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f),
                )
                ArtistText(
                    artist = artist,
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                )
                IconButton(onClick = { onAddToPlaylist(artist) }) {
                    Icon(painterResource(R.drawable.ic_options), "Opciones de ${artist.artist.name}")
                }
            }
        }
    }
}

@Composable
private fun ArtistImage(
    artist: ArtistSummary,
    modifier: Modifier = Modifier,
) {
    AsyncImage(
        model = artist.artworkUri,
        contentDescription = "Imagen de ${artist.artist.name}",
        modifier = modifier.clip(CircleShape),
        placeholder = painterResource(R.drawable.placeholder_artist),
        error = painterResource(R.drawable.placeholder_artist),
        fallback = painterResource(R.drawable.placeholder_artist),
        contentScale = ContentScale.Crop,
    )
}

@Composable
private fun ArtistText(
    artist: ArtistSummary,
    modifier: Modifier = Modifier,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = horizontalAlignment,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = artist.artist.name,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = artist.metadataLabel(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun ArtistSummary.metadataLabel(): String =
    "${albumCount.countLabel("álbum", "álbumes")} · " +
        trackCount.countLabel("canción", "canciones")

private fun Int.countLabel(singular: String, plural: String) =
    if (this == 1) "1 $singular" else "$this $plural"
