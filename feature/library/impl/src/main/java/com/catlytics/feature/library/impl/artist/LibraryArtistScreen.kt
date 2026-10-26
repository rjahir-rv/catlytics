package com.catlytics.feature.library.impl.artist

import android.graphics.Bitmap
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ListItem
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.catlytics.core.designsystem.component.rememberArtworkGradient
import com.catlytics.core.designsystem.layout.CatlyticsLayoutTokens
import com.catlytics.core.designsystem.component.SearchCollapsibleHeader
import com.catlytics.core.designsystem.layout.ArtworkHeaderLayout
import com.catlytics.core.designsystem.layout.FormFactorPreviews
import com.catlytics.core.designsystem.R
import com.catlytics.core.designsystem.component.ArtworkGradientBackground
import com.catlytics.core.designsystem.component.CatlyticsArtwork
import com.catlytics.core.designsystem.component.TrackSelectionHost
import com.catlytics.core.designsystem.component.animateArtworkGradientColors
import com.catlytics.core.designsystem.component.rememberTrackSelectionState
import com.catlytics.core.designsystem.layout.deferredPadding
import com.catlytics.core.designsystem.modifier.pressScale
import com.catlytics.core.designsystem.text.asString
import com.catlytics.core.designsystem.text.displayAlbumTitle
import com.catlytics.core.designsystem.text.displayArtistName
import com.catlytics.core.designsystem.theme.CatlyticsTheme
import com.catlytics.core.model.Album
import com.catlytics.core.model.Artist
import com.catlytics.core.designsystem.component.CatlyticsMenuDivider
import com.catlytics.core.designsystem.component.CollapsibleHint
import com.catlytics.core.designsystem.component.InfoToggleButton
import com.catlytics.core.designsystem.component.CatlyticsMenuEntry
import com.catlytics.core.designsystem.component.CatlyticsMenuItem
import com.catlytics.core.model.ArtistAlbumSort
import com.catlytics.core.model.ArtistContent
import com.catlytics.core.model.ArtistTrackSort
import com.catlytics.feature.library.impl.collection.CollectionPlaybackRow
import com.catlytics.feature.library.impl.collection.LibraryMessage
import com.catlytics.feature.library.impl.collection.SortOption
import com.catlytics.feature.library.impl.collection.SortOptionsSheet
import com.catlytics.feature.library.impl.collection.collectionMenuEntries
import com.catlytics.core.model.ArtistSummary
import com.catlytics.core.model.ArtistAlias
import com.catlytics.core.model.PlaylistSource
import com.catlytics.core.model.Track
import com.catlytics.core.model.TrackSelectionAction
import com.catlytics.core.model.TrackSelectionSnapshot
import com.catlytics.feature.library.impl.LibraryDimens
import com.catlytics.feature.library.impl.R as LibraryR
import kotlinx.coroutines.launch

@Composable
internal fun LibraryArtistScreen(
    uiState: LibraryArtistUiState,
    onAlbumSelected: (Album) -> Unit,
    modifier: Modifier = Modifier,
    onTrackSelected: (Track, List<Track>) -> Unit,
    onAddToPlaylist: (PlaylistSource) -> Unit,
    onTrackOptions: (Track) -> Unit,
    likedTrackIds: Set<String> = emptySet(),
    currentTrackId: String? = null,
    isCurrentTrackPlaying: Boolean = false,
    onTrackSelectionAction: (TrackSelectionAction) -> Unit = {},
    onTopBarColorChange: (Color) -> Unit,
    isSearching: Boolean = false,
    bottomPadding: () -> Dp = { 0.dp },
    scaffoldContentPadding: PaddingValues = PaddingValues(0.dp),
    onShowMergePicker: () -> Unit = {},
    onMergeQueryChange: (String) -> Unit = {},
    onMergeTargetSelected: (Artist) -> Unit = {},
    onShowAliasManager: () -> Unit = {},
    onUnmergeRequested: (ArtistAlias) -> Unit = {},
    onDismissMergeDialog: () -> Unit = {},
    onConfirmMerge: () -> Unit = {},
    onConfirmUnmerge: () -> Unit = {},
    onShowRename: () -> Unit = {},
    onChangeArtwork: () -> Unit = {},
    onRestoreArtwork: () -> Unit = {},
    onRenameNameChange: (String) -> Unit = {},
    onConfirmRename: () -> Unit = {},
    onPlayShuffled: (List<Track>) -> Unit = {},
    onTrackSortChange: (ArtistTrackSort) -> Unit = {},
    onAlbumSortChange: (ArtistAlbumSort) -> Unit = {},
) {
    when (uiState) {
        LibraryArtistUiState.Loading -> Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
        LibraryArtistUiState.NotFound -> LibraryMessage(
            message = stringResource(LibraryR.string.library_artist_not_found_message),
            modifier = modifier,
        )
        is LibraryArtistUiState.Error -> LibraryMessage(uiState.message.asString(), modifier)
        is LibraryArtistUiState.Success -> ArtistContent(
            content = uiState.content,
            playbackQueue = uiState.playbackQueue,
            onAlbumSelected = onAlbumSelected,
            onTrackSelected = onTrackSelected,
            onAddToPlaylist = onAddToPlaylist,
            onTrackOptions = onTrackOptions,
            likedTrackIds = likedTrackIds,
            currentTrackId = currentTrackId,
            isCurrentTrackPlaying = isCurrentTrackPlaying,
            onTrackSelectionAction = onTrackSelectionAction,
            onTopBarColorChange = onTopBarColorChange,
            isSearching = isSearching,
            bottomPadding = bottomPadding,
            scaffoldContentPadding = scaffoldContentPadding,
            modifier = modifier,
            aliases = uiState.aliases,
            canRename = uiState.canRename,
            onShowMergePicker = onShowMergePicker,
            onShowAliasManager = onShowAliasManager,
            onShowRename = onShowRename,
            onChangeArtwork = onChangeArtwork,
            onRestoreArtwork = onRestoreArtwork,
            onPlayShuffled = onPlayShuffled,
            trackSort = uiState.trackSort,
            albumSort = uiState.albumSort,
            onTrackSortChange = onTrackSortChange,
            onAlbumSortChange = onAlbumSortChange,
        )
    }
    if (uiState is LibraryArtistUiState.Success) {
        ArtistMergeDialogs(
            state = uiState,
            onQueryChange = onMergeQueryChange,
            onTargetSelected = onMergeTargetSelected,
            onUnmergeRequested = onUnmergeRequested,
            onDismiss = onDismissMergeDialog,
            onConfirmMerge = onConfirmMerge,
            onConfirmUnmerge = onConfirmUnmerge,
            onRenameNameChange = onRenameNameChange,
            onConfirmRename = onConfirmRename,
        )
    }
}

@Composable
private fun ArtistContent(
    content: ArtistContent,
    playbackQueue: List<Track>,
    onAlbumSelected: (Album) -> Unit,
    onTrackSelected: (Track, List<Track>) -> Unit,
    onAddToPlaylist: (PlaylistSource) -> Unit,
    onTrackOptions: (Track) -> Unit,
    likedTrackIds: Set<String>,
    currentTrackId: String?,
    isCurrentTrackPlaying: Boolean,
    onTrackSelectionAction: (TrackSelectionAction) -> Unit,
    onTopBarColorChange: (Color) -> Unit,
    isSearching: Boolean,
    bottomPadding: () -> Dp,
    scaffoldContentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    aliases: List<ArtistAlias>,
    canRename: Boolean,
    onShowMergePicker: () -> Unit,
    onShowAliasManager: () -> Unit,
    onShowRename: () -> Unit,
    onChangeArtwork: () -> Unit,
    onRestoreArtwork: () -> Unit,
    onPlayShuffled: (List<Track>) -> Unit,
    trackSort: ArtistTrackSort,
    albumSort: ArtistAlbumSort,
    onTrackSortChange: (ArtistTrackSort) -> Unit,
    onAlbumSortChange: (ArtistAlbumSort) -> Unit,
) {
    var sortSheet by rememberSaveable { mutableStateOf<ArtistDetailSection?>(null) }
    when (sortSheet) {
        ArtistDetailSection.Songs -> SortOptionsSheet(
            title = stringResource(LibraryR.string.library_collection_sort_tracks),
            options = ArtistTrackSort.entries.map { SortOption(it, it.label()) },
            selected = trackSort,
            onSelect = onTrackSortChange,
            onDismiss = { sortSheet = null },
        )
        ArtistDetailSection.Albums -> SortOptionsSheet(
            title = stringResource(LibraryR.string.library_collection_sort_albums),
            options = ArtistAlbumSort.entries.map { SortOption(it, it.label()) },
            selected = albumSort,
            onSelect = onAlbumSortChange,
            onDismiss = { sortSheet = null },
        )
        null -> Unit
    }
    val selectedSectionIndex = rememberSaveable(content.summary.artist.id) { mutableIntStateOf(0) }
    val pagerState = rememberPagerState(
        initialPage = selectedSectionIndex.intValue,
        pageCount = { ArtistDetailSection.entries.size },
    )
    val coroutineScope = rememberCoroutineScope()
    val songsListState = remember(content.summary.artist.id) {
        LazyGridState()
    }
    val albumsGridState = remember(content.summary.artist.id) {
        LazyGridState()
    }
    val artworkGradient = rememberArtworkGradient(content.summary.artworkUri)
    val animatedGradientColors = animateArtworkGradientColors(
        target = artworkGradient.colors,
        labelPrefix = "LibraryArtistGradient",
    )

    LaunchedEffect(artworkGradient.colors.start) {
        onTopBarColorChange(artworkGradient.colors.start)
    }

    LaunchedEffect(pagerState.currentPage) {
        selectedSectionIndex.intValue = pagerState.currentPage
    }

    // Al buscar, los resultados empiezan arriba, justo bajo las pestañas.
    LaunchedEffect(isSearching) {
        if (isSearching) {
            songsListState.scrollToItem(0)
            albumsGridState.scrollToItem(0)
        }
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    deferredPadding(
                        top = { scaffoldContentPadding.calculateTopPadding() },
                    ),
                ),
        ) {
            SearchCollapsibleHeader(isSearching = isSearching) {
                ArtistHeader(
                    content = content,
                    artworkModel = artworkGradient.artworkRequest,
                    onArtworkLoaded = artworkGradient::onArtworkLoaded,
                    sortLabel = when (ArtistDetailSection.entries[pagerState.currentPage]) {
                        ArtistDetailSection.Songs -> trackSort.takeIf { it != ArtistTrackSort.Album }?.label()
                        ArtistDetailSection.Albums -> albumSort.takeIf { it != ArtistAlbumSort.Title }?.label()
                    },
                    playbackQueue = playbackQueue,
                    onPlayAll = { playbackQueue.firstOrNull()?.let { onTrackSelected(it, playbackQueue) } },
                    onPlayShuffled = { onPlayShuffled(playbackQueue) },
                    menuEntries = artistMenuEntries(
                        content = content,
                        tracks = playbackQueue,
                        likedTrackIds = likedTrackIds,
                        section = ArtistDetailSection.entries[pagerState.currentPage],
                        aliasCount = aliases.size,
                        canRename = canRename,
                        onTrackSelectionAction = onTrackSelectionAction,
                        onAddToPlaylist = onAddToPlaylist,
                        onSort = { section -> sortSheet = section },
                        onShowRename = onShowRename,
                        onChangeArtwork = onChangeArtwork,
                        onRestoreArtwork = onRestoreArtwork,
                        onShowMergePicker = onShowMergePicker,
                        onShowAliasManager = onShowAliasManager,
                    ),
                    modifier = Modifier.padding(
                        start = 20.dp,
                        top = 20.dp,
                        end = 20.dp,
                        bottom = 8.dp,
                    ),
                )
            }
            ArtistSectionTabs(
                selectedIndex = pagerState.currentPage,
                onSectionSelected = { index ->
                    selectedSectionIndex.intValue = index
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(index)
                    }
                },
            )
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
            ) { page ->
                when (ArtistDetailSection.entries[page]) {
                    ArtistDetailSection.Songs -> ArtistSongsPage(
                        tracks = content.tracks,
                        state = songsListState,
                        onTrackSelected = { track ->
                            if (selection.active) {
                                selectionState.value = selection.onTrackLongClick(track.id)
                            } else {
                                onTrackSelected(track, playbackQueue)
                            }
                        },
                        onTrackOptions = onTrackOptions,
                        bottomPadding = bottomPadding,
                        currentTrackId = currentTrackId,
                        isCurrentTrackPlaying = isCurrentTrackPlaying,
                        selection = selection,
                        onTrackLongClick = { track ->
                            selectionState.value = selection.onTrackLongClick(track.id)
                        },
                    )
                    ArtistDetailSection.Albums -> ArtistAlbumsPage(
                        albums = content.albums,
                        state = albumsGridState,
                        onAlbumSelected = onAlbumSelected,
                        onAddToPlaylist = { album ->
                            onAddToPlaylist(PlaylistSource.AlbumSource(album.id))
                        },
                        bottomPadding = bottomPadding,
                    )
                }
            }
        }
        }
    }
}

@Composable
private fun ArtistHeader(
    content: ArtistContent,
    artworkModel: Any?,
    onArtworkLoaded: (Bitmap) -> Unit,
    sortLabel: String?,
    playbackQueue: List<Track>,
    onPlayAll: () -> Unit,
    onPlayShuffled: () -> Unit,
    menuEntries: List<CatlyticsMenuEntry>,
    modifier: Modifier = Modifier,
) {
    ArtworkHeaderLayout(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        artwork = { isWide ->
            CatlyticsArtwork(
                model = artworkModel,
                seed = content.summary.artist.name,
                placeholderIcon = R.drawable.ic_artist,
                contentDescription = stringResource(
                    LibraryR.string.library_artist_image_content_description,
                    content.summary.artist.name.displayArtistName(),
                ),
                modifier = Modifier
                    .then(
                        if (isWide) {
                            Modifier.size(ArtistArtworkWideSize)
                        } else {
                            Modifier
                                .fillMaxWidth(0.52f)
                                .widthIn(max = ArtistArtworkMaxSize)
                        },
                    )
                    .aspectRatio(1f)
                    .clip(CircleShape),
                onSuccess = onArtworkLoaded,
                contentScale = ContentScale.Crop,
            )
        },
        details = {
            Text(
                text = content.summary.artist.name.displayArtistName(),
                style = MaterialTheme.typography.headlineSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            val albumCountText = pluralStringResource(
                R.plurals.ds_album_count,
                content.summary.albumCount,
                content.summary.albumCount,
            )
            val trackCountText = pluralStringResource(
                R.plurals.ds_track_count,
                content.summary.trackCount,
                content.summary.trackCount,
            )
            Text(
                text = stringResource(
                    LibraryR.string.library_artist_metadata,
                    albumCountText,
                    trackCountText,
                ),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            sortLabel?.let { label ->
                Text(
                    text = stringResource(LibraryR.string.library_collection_sorted_by, label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            CollectionPlaybackRow(
                tracks = playbackQueue,
                onPlayShuffled = onPlayShuffled,
                onPlayAll = onPlayAll,
                menuEntries = menuEntries,
                menuContentDescription = stringResource(
                    R.string.ds_options_content_description,
                    content.summary.artist.name.displayArtistName(),
                ),
            )
        },
    )
}

private val ArtistArtworkMaxSize = 220.dp
private val ArtistArtworkWideSize = 180.dp

@Composable
private fun artistMenuEntries(
    content: ArtistContent,
    tracks: List<Track>,
    likedTrackIds: Set<String>,
    section: ArtistDetailSection,
    aliasCount: Int,
    canRename: Boolean,
    onTrackSelectionAction: (TrackSelectionAction) -> Unit,
    onAddToPlaylist: (PlaylistSource) -> Unit,
    onSort: (ArtistDetailSection) -> Unit,
    onShowRename: () -> Unit,
    onChangeArtwork: () -> Unit,
    onRestoreArtwork: () -> Unit,
    onShowMergePicker: () -> Unit,
    onShowAliasManager: () -> Unit,
): List<CatlyticsMenuEntry> = buildList {
    addAll(
        collectionMenuEntries(
            tracks = tracks,
            likedTrackIds = likedTrackIds,
            onTrackSelectionAction = onTrackSelectionAction,
            onAddToPlaylist = { onAddToPlaylist(PlaylistSource.ArtistSource(content.summary.artist.id)) },
        ),
    )
    // Sorting follows the visible tab, so the menu offers the list the user is looking at.
    add(
        CatlyticsMenuItem(
            text = stringResource(
                when (section) {
                    ArtistDetailSection.Songs -> LibraryR.string.library_collection_sort_tracks
                    ArtistDetailSection.Albums -> LibraryR.string.library_collection_sort_albums
                },
            ),
            icon = R.drawable.ic_filter,
            onClick = { onSort(section) },
        ),
    )
    add(CatlyticsMenuDivider)
    // "Artista desconocido" agrupa canciones sin artista: no se renombra ni lleva imagen propia.
    if (canRename) {
        add(
            CatlyticsMenuItem(
                text = stringResource(LibraryR.string.library_artist_rename_open),
                icon = R.drawable.ic_edit,
                onClick = onShowRename,
            ),
        )
        add(
            CatlyticsMenuItem(
                text = stringResource(LibraryR.string.library_artist_image_change),
                icon = R.drawable.ic_image,
                onClick = onChangeArtwork,
            ),
        )
        if (content.summary.hasCustomArtwork) {
            add(
                CatlyticsMenuItem(
                    text = stringResource(LibraryR.string.library_artist_image_restore),
                    icon = R.drawable.ic_close,
                    onClick = onRestoreArtwork,
                ),
            )
        }
    }
    add(
        CatlyticsMenuItem(
            text = stringResource(LibraryR.string.library_artist_merge_open),
            icon = R.drawable.ic_artist,
            onClick = onShowMergePicker,
        ),
    )
    if (aliasCount > 0) {
        add(
            CatlyticsMenuItem(
                text = stringResource(LibraryR.string.library_artist_merge_manage, aliasCount),
                icon = R.drawable.ic_list,
                onClick = onShowAliasManager,
            ),
        )
    }
}

@Composable
private fun ArtistTrackSort.label(): String = stringResource(
    when (this) {
        ArtistTrackSort.Album -> LibraryR.string.library_sort_album
        ArtistTrackSort.Title -> LibraryR.string.library_sort_title
        ArtistTrackSort.Duration -> LibraryR.string.library_sort_duration
        ArtistTrackSort.MostPlayed -> LibraryR.string.library_sort_most_played
    },
)

@Composable
private fun ArtistAlbumSort.label(): String = stringResource(
    when (this) {
        ArtistAlbumSort.Title -> LibraryR.string.library_sort_title
        ArtistAlbumSort.TrackCount -> LibraryR.string.library_sort_track_count
    },
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ArtistMergeDialogs(
    state: LibraryArtistUiState.Success,
    onQueryChange: (String) -> Unit,
    onTargetSelected: (Artist) -> Unit,
    onUnmergeRequested: (ArtistAlias) -> Unit,
    onDismiss: () -> Unit,
    onConfirmMerge: () -> Unit,
    onConfirmUnmerge: () -> Unit,
    onRenameNameChange: (String) -> Unit,
    onConfirmRename: () -> Unit,
) {
    when (val dialog = state.mergeDialog) {
        ArtistMergeDialog.Hidden -> Unit
        is ArtistMergeDialog.SelectTarget -> {
            val candidates = remember(state.mergeCandidates, dialog.query) {
                val query = dialog.query.trim()
                if (query.isEmpty()) state.mergeCandidates else state.mergeCandidates.filter {
                    it.name.contains(query, ignoreCase = true)
                }
            }
            ModalBottomSheet(onDismissRequest = onDismiss) {
                Text(
                    text = stringResource(LibraryR.string.library_artist_merge_picker_title),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
                Text(
                    text = stringResource(LibraryR.string.library_artist_merge_picker_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                )
                OutlinedTextField(
                    value = dialog.query,
                    onValueChange = onQueryChange,
                    label = {
                        Text(stringResource(LibraryR.string.library_artist_merge_search_hint))
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                )
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(bottom = 32.dp),
                ) {
                    items(candidates, key = Artist::id) { artist ->
                        ListItem(
                            headlineContent = { Text(artist.name.displayArtistName()) },
                            modifier = Modifier.clickable { onTargetSelected(artist) },
                        )
                    }
                }
            }
        }
        is ArtistMergeDialog.ConfirmMerge -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(LibraryR.string.library_artist_merge_dialog_title)) },
            text = {
                Text(
                    stringResource(
                        LibraryR.string.library_artist_merge_confirm_message,
                        state.content.summary.artist.name.displayArtistName(),
                        dialog.target.name.displayArtistName(),
                    ),
                )
            },
            confirmButton = {
                Button(onClick = onConfirmMerge, enabled = !state.isMergeBusy) {
                    Text(
                        if (state.isMergeBusy) {
                            stringResource(LibraryR.string.library_action_merging)
                        } else {
                            stringResource(LibraryR.string.library_action_merge)
                        },
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss, enabled = !state.isMergeBusy) {
                    Text(stringResource(LibraryR.string.library_action_cancel))
                }
            },
        )
        ArtistMergeDialog.ManageAliases -> ModalBottomSheet(onDismissRequest = onDismiss) {
            Text(
                text = stringResource(LibraryR.string.library_artist_aliases_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
            state.aliases.forEachIndexed { index, alias ->
                ListItem(
                    headlineContent = { Text(alias.source.name) },
                    supportingContent = {
                        Text(
                            if (alias.isDisplayRename) {
                                stringResource(LibraryR.string.library_artist_alias_original_name)
                            } else {
                                stringResource(LibraryR.string.library_artist_alias_target, alias.target.name)
                            },
                        )
                    },
                    trailingContent = {
                        TextButton(onClick = { onUnmergeRequested(alias) }) {
                            Text(
                                stringResource(
                                    if (alias.isDisplayRename) {
                                        LibraryR.string.library_action_restore_name
                                    } else {
                                        LibraryR.string.library_action_unmerge
                                    },
                                ),
                            )
                        }
                    },
                )
                if (index < state.aliases.lastIndex) HorizontalDivider()
            }
        }
        is ArtistMergeDialog.Rename -> {
            var showDetails by rememberSaveable { mutableStateOf(false) }
            AlertDialog(
                onDismissRequest = onDismiss,
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(LibraryR.string.library_artist_rename_title),
                            modifier = Modifier.weight(1f),
                        )
                        InfoToggleButton(expanded = showDetails, onExpandedChange = { showDetails = it })
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(stringResource(LibraryR.string.library_artist_rename_description))
                        CollapsibleHint(
                            visible = showDetails,
                            text = stringResource(LibraryR.string.library_artist_rename_details),
                        )
                        OutlinedTextField(
                            value = dialog.name,
                            onValueChange = onRenameNameChange,
                            label = { Text(stringResource(LibraryR.string.library_artist_rename_field)) },
                            singleLine = true,
                            supportingText = state.renameMergeTarget?.let { target ->
                                { Text(stringResource(LibraryR.string.library_artist_rename_existing, target.name)) }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = onConfirmRename,
                        enabled = dialog.name.isNotBlank() && !state.isMergeBusy,
                    ) {
                        Text(
                            if (state.isMergeBusy) {
                                stringResource(LibraryR.string.library_action_renaming)
                            } else {
                                stringResource(LibraryR.string.library_action_rename)
                            },
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss, enabled = !state.isMergeBusy) {
                        Text(stringResource(LibraryR.string.library_action_cancel))
                    }
                },
            )
        }
        is ArtistMergeDialog.ConfirmUnmerge -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(LibraryR.string.library_artist_unmerge_dialog_title)) },
            text = {
                Text(
                    stringResource(
                        LibraryR.string.library_artist_unmerge_confirm_message,
                        dialog.alias.source.name,
                    ),
                )
            },
            confirmButton = {
                Button(onClick = onConfirmUnmerge, enabled = !state.isMergeBusy) {
                    Text(
                        if (state.isMergeBusy) {
                            stringResource(LibraryR.string.library_action_unmerging)
                        } else {
                            stringResource(LibraryR.string.library_action_unmerge)
                        },
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss, enabled = !state.isMergeBusy) {
                    Text(stringResource(LibraryR.string.library_action_cancel))
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ArtistSectionTabs(
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
private fun ArtistSongsPage(
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
private fun ArtistAlbumsPage(
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

private enum class ArtistDetailSection(@param:StringRes val labelRes: Int) {
    Songs(LibraryR.string.library_artist_tab_songs),
    Albums(LibraryR.string.library_artist_tab_albums),
}

@FormFactorPreviews
@Composable
internal fun LibraryArtistScreenPreview() {
    LibraryArtistScreenPreviewContent()
}

@Composable
private fun LibraryArtistScreenPreviewContent() {
    CatlyticsTheme {
        LibraryArtistScreen(
            uiState = LibraryArtistUiState.Success(previewArtistContent()),
            onAlbumSelected = {},
            onTrackSelected = { _, _ -> },
            onAddToPlaylist = {},
            onTrackOptions = {},
            onTopBarColorChange = {},
        )
    }
}

private fun previewArtistContent(): ArtistContent {
    val artist = Artist("artist-preview", "Mitski")
    val albums = listOf(
        Album(
            id = "album-1",
            title = "The Land Is Inhospitable and So Are We",
            artist = artist,
            trackCount = 11,
        ),
        Album(
            id = "album-2",
            title = "Laurel Hell",
            artist = artist,
            trackCount = 11,
        ),
        Album(
            id = "album-3",
            title = "Be the Cowboy",
            artist = artist,
            trackCount = 14,
        ),
        Album(
            id = "album-4",
            title = "Puberty 2",
            artist = artist,
            trackCount = 11,
        ),
    )
    val tracks = listOf(
        Track(
            id = "track-1",
            title = "Bug Like an Angel",
            artist = artist,
            durationMillis = 212_000,
            mediaUri = "content://preview/track-1",
            albumTitle = albums[0].title,
        ),
        Track(
            id = "track-2",
            title = "Heaven",
            artist = artist,
            durationMillis = 224_000,
            mediaUri = "content://preview/track-2",
            albumTitle = albums[0].title,
        ),
        Track(
            id = "track-3",
            title = "Working for the Knife",
            artist = artist,
            durationMillis = 159_000,
            mediaUri = "content://preview/track-3",
            albumTitle = albums[1].title,
        ),
        Track(
            id = "track-4",
            title = "Nobody",
            artist = artist,
            durationMillis = 193_000,
            mediaUri = "content://preview/track-4",
            albumTitle = albums[2].title,
        ),
    )
    return ArtistContent(
        summary = ArtistSummary(
            artist = artist,
            albumCount = albums.size,
            trackCount = tracks.size,
        ),
        albums = albums,
        tracks = tracks,
    )
}
