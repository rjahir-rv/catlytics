package com.catlytics.feature.library.impl.artist

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.catlytics.core.designsystem.component.rememberArtworkGradient
import com.catlytics.core.designsystem.component.SearchCollapsibleHeader
import com.catlytics.core.designsystem.component.ArtworkGradientBackground
import com.catlytics.core.designsystem.component.TrackSelectionHost
import com.catlytics.core.designsystem.component.animateArtworkGradientColors
import com.catlytics.core.designsystem.component.rememberTrackSelectionState
import com.catlytics.core.designsystem.layout.deferredPadding
import com.catlytics.core.designsystem.text.asString
import com.catlytics.core.model.Album
import com.catlytics.core.model.Artist
import com.catlytics.core.model.ArtistAlbumSort
import com.catlytics.core.model.ArtistContent
import com.catlytics.core.model.ArtistTrackSort
import com.catlytics.feature.library.impl.collection.LibraryMessage
import com.catlytics.feature.library.impl.collection.SortOption
import com.catlytics.feature.library.impl.collection.SortOptionsSheet
import com.catlytics.core.model.ArtistAlias
import com.catlytics.core.model.PlaylistSource
import com.catlytics.core.model.Track
import com.catlytics.core.model.TrackSelectionAction
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
internal fun ArtistContent(
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
internal fun ArtistTrackSort.label(): String = stringResource(
    when (this) {
        ArtistTrackSort.Album -> LibraryR.string.library_sort_album
        ArtistTrackSort.Title -> LibraryR.string.library_sort_title
        ArtistTrackSort.Duration -> LibraryR.string.library_sort_duration
        ArtistTrackSort.MostPlayed -> LibraryR.string.library_sort_most_played
    },
)

@Composable
internal fun ArtistAlbumSort.label(): String = stringResource(
    when (this) {
        ArtistAlbumSort.Title -> LibraryR.string.library_sort_title
        ArtistAlbumSort.TrackCount -> LibraryR.string.library_sort_track_count
    },
)
