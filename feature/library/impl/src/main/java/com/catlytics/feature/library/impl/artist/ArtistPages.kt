package com.catlytics.feature.library.impl.artist

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.catlytics.core.designsystem.component.TrackListRow
import com.catlytics.core.designsystem.layout.CatlyticsLayoutTokens
import com.catlytics.core.designsystem.R
import com.catlytics.core.designsystem.component.CatlyticsArtwork
import com.catlytics.core.designsystem.layout.deferredPadding
import com.catlytics.core.designsystem.modifier.pressScale
import com.catlytics.core.designsystem.text.displayAlbumTitle
import com.catlytics.core.model.Album
import com.catlytics.core.model.Track
import com.catlytics.core.model.TrackSelectionSnapshot
import com.catlytics.feature.library.impl.LibraryDimens
import com.catlytics.feature.library.impl.R as LibraryR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ArtistSectionTabs(
    selectedIndex: Int,
    onSectionSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    SecondaryTabRow(
        selectedTabIndex = selectedIndex,
        modifier = modifier,
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.primary,
        divider = {},
        indicator = {
            TabRowDefaults.SecondaryIndicator(
                modifier = Modifier
                    .tabIndicatorOffset(selectedIndex)
                    .padding(horizontal = 20.dp)
                    .clip(MaterialTheme.shapes.extraLarge),
                color = MaterialTheme.colorScheme.primary,
            )
        },
    ) {
        ArtistDetailSection.entries.forEachIndexed { index, section ->
            Tab(
                selected = index == selectedIndex,
                onClick = { onSectionSelected(index) },
                text = { Text(stringResource(section.labelRes)) },
            )
        }
    }
}

@Composable
internal fun ArtistSongsPage(
    tracks: List<Track>,
    state: LazyGridState,
    onTrackSelected: (Track) -> Unit,
    onTrackOptions: (Track) -> Unit,
    bottomPadding: () -> Dp,
    currentTrackId: String?,
    isCurrentTrackPlaying: Boolean,
    selection: TrackSelectionSnapshot = TrackSelectionSnapshot(),
    onTrackLongClick: (Track) -> Unit = {},
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(CatlyticsLayoutTokens.ListItemMinColumnWidth),
        state = state,
        modifier = Modifier.fillMaxSize(),
        contentPadding = deferredPadding(
            start = 20.dp,
            top = { 8.dp },
            end = 20.dp,
            bottom = { bottomPadding() + 20.dp + if (selection.active) 72.dp else 0.dp },
        ),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        gridItems(items = tracks, key = Track::id) { track ->
            TrackListRow(
                track = track,
                modifier = Modifier.animateItem(),
                isCurrent = track.id == currentTrackId,
                isPlaying = track.id == currentTrackId && isCurrentTrackPlaying,
                onClick = { onTrackSelected(track) },
                onOptions = { onTrackOptions(track) },
                selected = track.id in selection.selectedIds,
                selectionActive = selection.active,
                onLongClick = { onTrackLongClick(track) },
            )
        }
    }
}

@Composable
internal fun ArtistAlbumsPage(
    albums: List<Album>,
    state: LazyGridState,
    onAlbumSelected: (Album) -> Unit,
    onAddToPlaylist: (Album) -> Unit,
    bottomPadding: () -> Dp,
) {
    LazyVerticalGrid(
        state = state,
        columns = GridCells.Adaptive(minSize = 160.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = deferredPadding(
            start = 20.dp,
            top = { 16.dp },
            end = 20.dp,
            bottom = { bottomPadding() + 20.dp },
        ),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        gridItems(items = albums, key = Album::id) { album ->
            ArtistAlbumCard(
                album = album,
                onClick = { onAlbumSelected(album) },
                onAddToPlaylist = { onAddToPlaylist(album) },
                modifier = Modifier.animateItem(),
            )
        }
    }
}

@Composable
private fun ArtistAlbumCard(
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
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        CatlyticsArtwork(
            model = album.artworkUri,
            seed = album.title,
            placeholderIcon = R.drawable.ic_album,
            contentDescription = stringResource(
                LibraryR.string.library_album_cover_content_description,
                album.title.displayAlbumTitle(),
            ),
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(LibraryDimens.ArtworkShape),
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
                    text = album.title.displayAlbumTitle(),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = pluralStringResource(
                        R.plurals.ds_track_count,
                        album.trackCount,
                        album.trackCount,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = onAddToPlaylist) {
                Icon(
                    painterResource(R.drawable.ic_options),
                    stringResource(
                        R.string.ds_add_to_playlist_content_description,
                        album.title.displayAlbumTitle(),
                    ),
                )
            }
        }
    }
}

internal enum class ArtistDetailSection(@param:StringRes val labelRes: Int) {
    Songs(LibraryR.string.library_artist_tab_songs),
    Albums(LibraryR.string.library_artist_tab_albums),
}
