package com.catlytics.feature.library.impl.album

import com.catlytics.core.designsystem.component.CatlyticsOptionsButton
import com.catlytics.core.designsystem.component.rememberArtworkGradient
import com.catlytics.core.designsystem.layout.CatlyticsLayoutTokens
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.GridCells
import android.graphics.Bitmap
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.catlytics.core.designsystem.layout.ArtworkHeaderLayout
import com.catlytics.core.designsystem.R
import com.catlytics.core.designsystem.component.ArtworkGradientBackground
import com.catlytics.core.designsystem.component.CatlyticsArtwork
import com.catlytics.core.designsystem.component.CatlyticsPlayingBarsOverlay
import com.catlytics.core.designsystem.component.animateArtworkGradientColors
import com.catlytics.core.designsystem.component.TrackSelectionHost
import com.catlytics.core.designsystem.component.rememberTrackSelectionState
import com.catlytics.core.designsystem.format.TrackDurationFormat
import com.catlytics.core.designsystem.layout.deferredPadding
import com.catlytics.core.designsystem.text.asString
import com.catlytics.core.designsystem.text.displayArtistName
import com.catlytics.core.designsystem.text.displayTrackTitle
import com.catlytics.core.designsystem.text.displayAlbumTitle
import com.catlytics.core.designsystem.component.CatlyticsMenuDivider
import com.catlytics.core.designsystem.component.CatlyticsMenuEntry
import com.catlytics.core.designsystem.component.CatlyticsMenuItem
import com.catlytics.core.model.Album
import com.catlytics.core.model.AlbumTrackSort
import com.catlytics.core.model.Artist
import com.catlytics.core.model.PlaylistSource
import com.catlytics.feature.library.impl.collection.CollectionPlaybackRow
import com.catlytics.feature.library.impl.collection.LibraryMessage
import com.catlytics.feature.library.impl.collection.SortOption
import com.catlytics.feature.library.impl.collection.SortOptionsSheet
import com.catlytics.feature.library.impl.collection.collectionMenuEntries
import com.catlytics.core.model.TrackSelectionAction
import com.catlytics.core.model.Track
import com.catlytics.feature.library.impl.R as LibraryR

@Composable
internal fun LibraryAlbumScreen(
    uiState: LibraryAlbumUiState,
    modifier: Modifier = Modifier,
    onTrackSelected: (Track, List<Track>) -> Unit,
    onPlayShuffled: (List<Track>) -> Unit = {},
    onTrackOptions: (Track) -> Unit,
    onEditAlbum: (List<Track>) -> Unit = {},
    onAddToPlaylist: (PlaylistSource) -> Unit = {},
    onArtistSelected: (Artist) -> Unit = {},
    onTrackSortChange: (AlbumTrackSort) -> Unit = {},
    likedTrackIds: Set<String> = emptySet(),
    currentTrackId: String? = null,
    isCurrentTrackPlaying: Boolean = false,
    onTrackSelectionAction: (TrackSelectionAction) -> Unit = {},
    onTopBarColorChange: (Color) -> Unit,
    bottomPadding: () -> Dp = { 0.dp },
    scaffoldContentPadding: PaddingValues = PaddingValues(0.dp),
) {
    when (uiState) {
        LibraryAlbumUiState.Loading -> Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
        LibraryAlbumUiState.NotFound -> LibraryMessage(
            message = stringResource(LibraryR.string.library_album_not_found_message),
            modifier = modifier,
        )
        is LibraryAlbumUiState.Error -> LibraryMessage(uiState.message.asString(), modifier)
        is LibraryAlbumUiState.Success -> {
            val content = uiState.content
            val artworkGradient = rememberArtworkGradient(content.album.artworkUri)
            val listState = remember(content.album.id) { LazyGridState() }
            val animatedGradientColors = animateArtworkGradientColors(
                target = artworkGradient.colors,
                labelPrefix = "LibraryAlbumGradient",
            )

            LaunchedEffect(artworkGradient.colors.start) {
                onTopBarColorChange(artworkGradient.colors.start)
            }

            var showSortSheet by rememberSaveable { mutableStateOf(false) }
            if (showSortSheet) {
                AlbumSortSheet(
                    selected = uiState.sort,
                    onSelect = onTrackSortChange,
                    onDismiss = { showSortSheet = false },
                )
            }
            val selectionState = rememberTrackSelectionState()
            val selection = selectionState.value
            ArtworkGradientBackground(
                colors = animatedGradientColors,
                modifier = modifier,
            ) {
                TrackSelectionHost(
                    selection = selection,
                    onSelectionChange = { selectionState.value = it },
                    visibleIds = remember(content.tracks) { content.tracks.map(Track::id) },
                    selectedTracks = remember(selection, content.tracks) {
                        selection.selectedTracks(content.tracks)
                    },
                    likedTrackIds = likedTrackIds,
                    currentTrackId = currentTrackId,
                    topInset = { scaffoldContentPadding.calculateTopPadding() },
                    bottomInset = bottomPadding,
                    onAction = onTrackSelectionAction,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(CatlyticsLayoutTokens.ListItemMinColumnWidth),
                        state = listState,
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = deferredPadding(
                            start = 20.dp,
                            top = {
                                scaffoldContentPadding.calculateTopPadding() + 20.dp +
                                    if (selection.active) 64.dp else 0.dp
                            },
                            end = 20.dp,
                            bottom = {
                                bottomPadding() + 20.dp +
                                    if (selection.active) 72.dp else 0.dp
                            },
                        ),
                    ) {
                        item(key = "header", span = { GridItemSpan(maxLineSpan) }) {
                            AlbumHeader(
                                album = content.album,
                                tracks = content.tracks,
                                artworkModel = artworkGradient.artworkRequest,
                                onArtworkLoaded = artworkGradient::onArtworkLoaded,
                                onPlayShuffled = onPlayShuffled,
                                onPlayAll = { tracks ->
                                    tracks.firstOrNull()?.let { onTrackSelected(it, tracks) }
                                },
                                sort = uiState.sort,
                                menuEntries = albumMenuEntries(
                                    album = content.album,
                                    tracks = content.tracks,
                                    likedTrackIds = likedTrackIds,
                                    onTrackSelectionAction = onTrackSelectionAction,
                                    onAddToPlaylist = onAddToPlaylist,
                                    onSortTracks = { showSortSheet = true },
                                    onEditAlbum = onEditAlbum,
                                    onArtistSelected = onArtistSelected,
                                ),
                            )
                        }
                        itemsIndexed(
                            items = content.tracks,
                            key = { _, track -> track.id },
                            contentType = { _, _ -> "track" },
                        ) { index, track ->
                            Column {
                                AlbumTrackRow(
                                    position = index + 1,
                                    track = track,
                                    isCurrent = track.id == currentTrackId,
                                    isPlaying = track.id == currentTrackId && isCurrentTrackPlaying,
                                    selected = track.id in selection.selectedIds,
                                    selectionActive = selection.active,
                                    onClick = {
                                        if (selection.active) {
                                            selectionState.value = selection.onTrackLongClick(track.id)
                                        } else {
                                            onTrackSelected(track, content.tracks)
                                        }
                                    },
                                    onLongClick = {
                                        selectionState.value = selection.onTrackLongClick(track.id)
                                    },
                                    onTrackOptions = { onTrackOptions(track) },
                                )
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AlbumHeader(
    album: Album,
    tracks: List<Track>,
    artworkModel: Any?,
    onArtworkLoaded: (Bitmap) -> Unit,
    onPlayShuffled: (List<Track>) -> Unit,
    onPlayAll: (List<Track>) -> Unit,
    sort: AlbumTrackSort,
    menuEntries: List<CatlyticsMenuEntry>,
    modifier: Modifier = Modifier,
) {
    ArtworkHeaderLayout(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        artwork = { isWide ->
            CatlyticsArtwork(
                model = artworkModel,
                seed = album.title,
                placeholderIcon = R.drawable.ic_album,
                contentDescription = stringResource(
                    LibraryR.string.library_album_cover_content_description,
                    album.title.displayAlbumTitle(),
                ),
                modifier = Modifier
                    .then(
                        if (isWide) {
                            Modifier.size(AlbumArtworkWideSize)
                        } else {
                            Modifier
                                .fillMaxWidth(0.72f)
                                .widthIn(max = AlbumArtworkMaxSize)
                        },
                    )
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(24.dp)),
                onSuccess = onArtworkLoaded,
                contentScale = ContentScale.Crop,
            )
        },
        details = {
            Text(
                text = album.title.displayAlbumTitle(),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = album.artist.name.displayArtistName(),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = pluralStringResource(
                    R.plurals.ds_track_count,
                    album.trackCount,
                    album.trackCount,
                ),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (sort != AlbumTrackSort.TrackNumber) {
                Text(
                    text = stringResource(LibraryR.string.library_collection_sorted_by, sort.label()),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            CollectionPlaybackRow(
                tracks = tracks,
                onPlayShuffled = { onPlayShuffled(tracks) },
                onPlayAll = { onPlayAll(tracks) },
                menuEntries = menuEntries,
                menuContentDescription = stringResource(
                    R.string.ds_options_content_description,
                    album.title.displayAlbumTitle(),
                ),
            )
        },
    )
}

private val AlbumArtworkMaxSize = 280.dp
private val AlbumArtworkWideSize = 220.dp

@Composable
private fun albumMenuEntries(
    album: Album,
    tracks: List<Track>,
    likedTrackIds: Set<String>,
    onTrackSelectionAction: (TrackSelectionAction) -> Unit,
    onAddToPlaylist: (PlaylistSource) -> Unit,
    onSortTracks: () -> Unit,
    onEditAlbum: (List<Track>) -> Unit,
    onArtistSelected: (Artist) -> Unit,
): List<CatlyticsMenuEntry> = collectionMenuEntries(
    tracks = tracks,
    likedTrackIds = likedTrackIds,
    onTrackSelectionAction = onTrackSelectionAction,
    onAddToPlaylist = { onAddToPlaylist(PlaylistSource.AlbumSource(album.id)) },
) + listOf(
    CatlyticsMenuItem(
        text = stringResource(LibraryR.string.library_collection_sort_tracks),
        icon = R.drawable.ic_filter,
        onClick = onSortTracks,
    ),
    CatlyticsMenuDivider,
    CatlyticsMenuItem(
        text = stringResource(LibraryR.string.library_album_menu_edit),
        icon = R.drawable.ic_edit,
        onClick = { onEditAlbum(tracks) },
    ),
    CatlyticsMenuItem(
        text = stringResource(LibraryR.string.library_album_menu_go_to_artist),
        icon = R.drawable.ic_artist,
        onClick = { onArtistSelected(album.artist) },
    ),
)

@Composable
private fun AlbumSortSheet(
    selected: AlbumTrackSort,
    onSelect: (AlbumTrackSort) -> Unit,
    onDismiss: () -> Unit,
) {
    SortOptionsSheet(
        title = stringResource(LibraryR.string.library_collection_sort_tracks),
        options = AlbumTrackSort.entries.map { SortOption(it, it.label()) },
        selected = selected,
        onSelect = onSelect,
        onDismiss = onDismiss,
    )
}

@Composable
private fun AlbumTrackSort.label(): String = stringResource(
    when (this) {
        AlbumTrackSort.TrackNumber -> LibraryR.string.library_sort_track_number
        AlbumTrackSort.Title -> LibraryR.string.library_sort_title
        AlbumTrackSort.Duration -> LibraryR.string.library_sort_duration
        AlbumTrackSort.MostPlayed -> LibraryR.string.library_sort_most_played
    },
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AlbumTrackRow(
    position: Int,
    track: Track,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onTrackOptions: () -> Unit,
    selected: Boolean = false,
    selectionActive: Boolean = false,
    onLongClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(28.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (selectionActive && selected) {
                Icon(
                    painter = painterResource(R.drawable.ic_check_list),
                    contentDescription = stringResource(
                        LibraryR.string.library_track_selected_content_description,
                    ),
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .size(22.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                        .padding(3.dp),
                )
            } else if (isCurrent && isPlaying && !selectionActive) {
                CatlyticsPlayingBarsOverlay(
                    contentDescription = stringResource(
                        R.string.ds_track_row_playing_content_description,
                        track.title.displayTrackTitle(),
                    ),
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                )
            } else {
                Text(
                    text = position.toString(),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isCurrent && !selectionActive) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title.displayTrackTitle(),
                style = MaterialTheme.typography.titleMedium,
                color = if (isCurrent && !selectionActive) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onBackground
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = track.artist.name.displayArtistName(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = TrackDurationFormat.format(track.durationMillis),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (!selectionActive) {
            CatlyticsOptionsButton(label = track.title.displayTrackTitle(), onClick = onTrackOptions)
        }
    }
}
