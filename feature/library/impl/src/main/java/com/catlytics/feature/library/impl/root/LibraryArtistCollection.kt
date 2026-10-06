package com.catlytics.feature.library.impl.root

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.catlytics.core.designsystem.R
import com.catlytics.core.designsystem.component.CatlyticsLetterFastScroller
import com.catlytics.core.designsystem.component.sectionLetter
import com.catlytics.core.designsystem.modifier.pressScale
import com.catlytics.core.designsystem.modifier.staggeredEntrance
import com.catlytics.core.model.ArtistSummary
import com.catlytics.core.model.ArtistViewMode
import com.catlytics.core.model.SortDirection
import com.catlytics.feature.library.impl.LibraryDimens
import com.catlytics.feature.library.impl.R as LibraryR
import com.catlytics.feature.library.impl.sortedArtistsByDirection

@Composable
internal fun LibraryArtistCollection(
    artists: List<ArtistSummary>,
    modifier: Modifier = Modifier,
    viewMode: ArtistViewMode,
    sortDirection: SortDirection,
    listState: LazyListState = rememberLazyListState(),
    gridState: LazyGridState = rememberLazyGridState(),
    onArtistSelected: (ArtistSummary) -> Unit,
    onAddToPlaylist: (ArtistSummary) -> Unit,
    bottomPadding: () -> Dp = { 0.dp },
    topPadding: Dp = 0.dp,
    animateEntrance: Boolean = false,
) {
    val sortedArtists: List<ArtistSummary> = remember(artists, sortDirection) {
        artists.sortedArtistsByDirection(sortDirection)
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
                topPadding = topPadding,
                animateEntrance = animateEntrance,
            )
            ArtistViewMode.Grid -> ArtistGrid(
                artists = sortedArtists,
                sortDirection = sortDirection,
                state = gridState,
                onArtistSelected = onArtistSelected,
                onAddToPlaylist = onAddToPlaylist,
                bottomPadding = bottomPadding,
                topPadding = topPadding,
                animateEntrance = animateEntrance,
            )
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
    topPadding: Dp = 0.dp,
    animateEntrance: Boolean = false,
) {
    val contentPadding = PaddingValues(
        start = LibraryDimens.ScreenPadding,
        top = topPadding + LibraryDimens.ContentTopSpacing,
        end = LibraryDimens.ScreenPadding,
        bottom = bottomPadding() + LibraryDimens.ContentBottomSpacing,
    )
    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            state = state,
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            itemsIndexed(
                items = artists,
                key = { _, artist -> "${sortDirection.name}:${artist.artist.id}" },
            ) { index, artist ->
                ArtistListRow(
                    artist = artist,
                    onClick = { onArtistSelected(artist) },
                    onOptions = { onAddToPlaylist(artist) },
                    modifier = Modifier.staggeredEntrance(
                        index = index,
                        animate = animateEntrance && index < LibraryDimens.EntranceMaxStaggeredItems,
                    ),
                )
            }
        }
        CatlyticsLetterFastScroller(
            listState = state,
            itemCount = artists.size,
            letterForVisibleTrackIndex = { index -> artists[index].artist.name.sectionLetter() },
            contentPadding = PaddingValues(top = topPadding, bottom = bottomPadding()),
        )
    }
}

@Composable
private fun ArtistListRow(
    artist: ArtistSummary,
    onClick: () -> Unit,
    onOptions: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .pressScale(interactionSource)
            .clip(LibraryDimens.RowShape)
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(start = 4.dp, top = 6.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ArtistImage(
            artist = artist,
            modifier = Modifier.size(56.dp),
        )
        ArtistText(
            artist = artist,
            modifier = Modifier.weight(1f),
        )
        ArtistOptionsButton(artist = artist, onClick = onOptions)
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
    topPadding: Dp = 0.dp,
    animateEntrance: Boolean = false,
) {
    LazyVerticalGrid(
        state = state,
        columns = GridCells.Adaptive(minSize = 144.dp),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = LibraryDimens.ScreenPadding,
            top = topPadding + LibraryDimens.ContentTopSpacing,
            end = LibraryDimens.ScreenPadding,
            bottom = bottomPadding() + LibraryDimens.ContentBottomSpacing,
        ),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        itemsIndexed(
            items = artists,
            key = { _, artist -> "${sortDirection.name}:${artist.artist.id}" },
        ) { index, artist ->
            ArtistGridCard(
                artist = artist,
                onClick = { onArtistSelected(artist) },
                onOptions = { onAddToPlaylist(artist) },
                modifier = Modifier.staggeredEntrance(
                    index = index,
                    animate = animateEntrance && index < LibraryDimens.EntranceMaxStaggeredItems,
                ),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ArtistGridCard(
    artist: ArtistSummary,
    onClick: () -> Unit,
    onOptions: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .pressScale(interactionSource)
            .clip(LibraryDimens.CardShape)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                role = Role.Button,
                onLongClick = onOptions,
                onClick = onClick,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box {
            ArtistImage(
                artist = artist,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
            )
            ArtistOptionsButton(
                artist = artist,
                onClick = onOptions,
                modifier = Modifier.align(Alignment.BottomEnd),
                tonal = true,
            )
        }
        ArtistText(
            artist = artist,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        )
    }
}

@Composable
private fun ArtistOptionsButton(
    artist: ArtistSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tonal: Boolean = false,
) {
    val icon: @Composable () -> Unit = {
        Icon(
            painter = painterResource(R.drawable.ic_options),
            contentDescription = stringResource(
                LibraryR.string.library_artist_options_content_description,
                artist.artist.name,
            ),
            modifier = if (tonal) Modifier.size(18.dp) else Modifier,
        )
    }
    if (tonal) {
        FilledTonalIconButton(
            onClick = onClick,
            modifier = modifier.size(36.dp),
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.9f),
                contentColor = MaterialTheme.colorScheme.onSurface,
            ),
            content = icon,
        )
    } else {
        IconButton(onClick = onClick, modifier = modifier, content = icon)
    }
}

@Composable
private fun ArtistImage(
    artist: ArtistSummary,
    modifier: Modifier = Modifier,
) {
    AsyncImage(
        model = artist.artworkUri,
        contentDescription = stringResource(
            LibraryR.string.library_artist_image_content_description,
            artist.artist.name,
        ),
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
    val textAlign = if (horizontalAlignment == Alignment.CenterHorizontally) {
        TextAlign.Center
    } else {
        TextAlign.Start
    }
    Column(
        modifier = modifier,
        horizontalAlignment = horizontalAlignment,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = artist.artist.name,
            style = MaterialTheme.typography.titleSmall,
            textAlign = textAlign,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = artist.metadataLabel(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = textAlign,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ArtistSummary.metadataLabel(): String =
    stringResource(
        LibraryR.string.library_artist_metadata,
        pluralStringResource(
            LibraryR.plurals.library_album_count,
            albumCount,
            albumCount,
        ),
        pluralStringResource(
            LibraryR.plurals.library_track_count,
            trackCount,
            trackCount,
        ),
    )
