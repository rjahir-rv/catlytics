package com.catlytics.feature.library.impl.root

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.catlytics.core.designsystem.R
import com.catlytics.core.designsystem.modifier.pressScale
import com.catlytics.core.designsystem.modifier.staggeredEntrance
import com.catlytics.core.model.Album
import com.catlytics.core.model.SortDirection
import com.catlytics.feature.library.impl.LibraryDimens
import com.catlytics.feature.library.impl.R as LibraryR
import com.catlytics.feature.library.impl.sortedAlbumsByDirection

@Composable
internal fun LibraryAlbumGrid(
    albums: List<Album>,
    modifier : Modifier = Modifier,
    state: LazyGridState = rememberLazyGridState(),
    sortDirection: SortDirection,
    onAlbumSelected: (Album) -> Unit,
    onAddToPlaylist: (Album) -> Unit,
    bottomPadding: () -> Dp = { 0.dp },
    topPadding: Dp = 0.dp,
    animateEntrance: Boolean = false,
) {
    val sortedAlbums: List<Album> = remember(albums, sortDirection) {
        albums.sortedAlbumsByDirection(sortDirection)
    }

    LazyVerticalGrid(
        state = state,
        columns = GridCells.Adaptive(minSize = 160.dp),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 20.dp,
            top = topPadding + LibraryDimens.ContentTopSpacing,
            end = 20.dp,
            bottom = bottomPadding() + LibraryDimens.ContentBottomSpacing,
        ),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        itemsIndexed(
            items = sortedAlbums,
            key = { _, album -> "${sortDirection.name}:${album.id}" },
        ) { index, album ->
            AlbumCard(
                album = album,
                onClick = { onAlbumSelected(album) },
                onAddToPlaylist = { onAddToPlaylist(album) },
                modifier = Modifier.staggeredEntrance(
                    index = index,
                    animate = animateEntrance && index < LibraryDimens.EntranceMaxStaggeredItems,
                ),
            )
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
    val interactionSource = remember { MutableInteractionSource() }
    Column(
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
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        AsyncImage(
            model = album.artworkUri,
            contentDescription = stringResource(
                LibraryR.string.library_album_cover_content_description,
                album.title,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(LibraryDimens.ArtworkShape),
            placeholder = painterResource(R.drawable.placeholder_album),
            error = painterResource(R.drawable.placeholder_album),
            fallback = painterResource(R.drawable.placeholder_album),
            contentScale = ContentScale.Crop,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = album.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(
                        LibraryR.string.library_track_metadata,
                        album.artist.name,
                        pluralStringResource(
                            LibraryR.plurals.library_track_count,
                            album.trackCount,
                            album.trackCount,
                        ),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = onAddToPlaylist) {
                Icon(
                    painterResource(R.drawable.ic_options),
                    stringResource(
                        LibraryR.string.library_album_options_content_description,
                        album.title,
                    ),
                )
            }
        }
    }
}
