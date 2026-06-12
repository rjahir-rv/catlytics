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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.catlytics.core.designsystem.R
import com.catlytics.core.model.ArtistSummary
import com.catlytics.core.model.ArtistViewMode

@Composable
internal fun LibraryArtistCollection(
    artists: List<ArtistSummary>,
    viewMode: ArtistViewMode,
    onViewModeChange: (ArtistViewMode) -> Unit,
    onArtistSelected: (ArtistSummary) -> Unit,
    onAddToPlaylist: (ArtistSummary) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        when (viewMode) {
            ArtistViewMode.List -> ArtistList(
                artists = artists,
                onArtistSelected = onArtistSelected,
                onAddToPlaylist = onAddToPlaylist,
            )
            ArtistViewMode.Grid -> ArtistGrid(
                artists = artists,
                onArtistSelected = onArtistSelected,
                onAddToPlaylist = onAddToPlaylist,
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.End,
        ) {
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
    onArtistSelected: (ArtistSummary) -> Unit,
    onAddToPlaylist: (ArtistSummary) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 20.dp,
            top = 56.dp,
            end = 20.dp,
            bottom = 8.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(items = artists, key = { it.artist.id }) { artist ->
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
    onArtistSelected: (ArtistSummary) -> Unit,
    onAddToPlaylist: (ArtistSummary) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 144.dp),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 20.dp,
            top = 56.dp,
            end = 20.dp,
            bottom = 8.dp,
        ),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        items(items = artists, key = { it.artist.id }) { artist ->
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
