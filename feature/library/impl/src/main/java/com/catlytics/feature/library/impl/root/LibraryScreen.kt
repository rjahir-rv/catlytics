package com.catlytics.feature.library.impl.root

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.catlytics.core.designsystem.R
import com.catlytics.core.designsystem.component.CatlyticsEmptyState
import com.catlytics.core.designsystem.modifier.pressScale
import com.catlytics.core.designsystem.text.asString
import com.catlytics.core.designsystem.theme.CatlyticsTheme
import com.catlytics.core.model.Album
import com.catlytics.core.model.Artist
import com.catlytics.core.model.ArtistSummary
import com.catlytics.core.model.ArtistViewMode
import com.catlytics.core.model.LibraryFolder
import com.catlytics.core.model.PlaylistSource
import com.catlytics.core.model.SortDirection
import com.catlytics.feature.library.impl.R as LibraryR
import com.catlytics.feature.library.impl.filterAlbumsByQuery
import com.catlytics.feature.library.impl.filterArtistsByQuery
import com.catlytics.feature.library.impl.filterFoldersByQuery
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@Composable
internal fun LibraryScreen(
    uiState: LibraryUiState,
    modifier: Modifier = Modifier,
    hasAudioPermission: Boolean,
    onRequestPermission: () -> Unit,
    onAlbumSelected: (Album) -> Unit,
    onArtistSelected: (ArtistSummary) -> Unit,
    onArtistViewModeChange: (ArtistViewMode) -> Unit,
    onFolderVisibilityChange: (String, Boolean) -> Unit,
    onFolderSelected: (LibraryFolder) -> Unit,
    onAddToPlaylist: (PlaylistSource) -> Unit,
    searchQuery: String = "",
    sortDirection: SortDirection = SortDirection.Ascending,
    onSortDirectionChange: (SortDirection) -> Unit = {},
    bottomPadding: () -> Dp = { 0.dp },
    scaffoldContentPadding: PaddingValues = PaddingValues(0.dp),
    collapseFraction: () -> Float = { 0f },
) {
    val albumsGridState = rememberSaveable(saver = LazyGridState.Saver) { LazyGridState() }
    val artistsListState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    val artistsGridState = rememberSaveable(saver = LazyGridState.Saver) { LazyGridState() }
    val foldersListState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }

    if (!hasAudioPermission) {
        PermissionRequiredContent(
            onRequestPermission = onRequestPermission,
            modifier = modifier,
        )
        return
    }

    when (uiState) {
        LibraryUiState.Loading -> LoadingContent(modifier)
        LibraryUiState.Empty -> CatlyticsEmptyState(
            message = stringResource(LibraryR.string.library_empty_message),
            modifier = modifier,
        )
        is LibraryUiState.Error -> CatlyticsEmptyState(
            message = uiState.message.asString(),
            modifier = modifier,
            messageColor = MaterialTheme.colorScheme.error,
        )
        is LibraryUiState.Success -> {
            val filteredAlbums = remember(uiState.albums, searchQuery) {
                uiState.albums.filterAlbumsByQuery(searchQuery)
            }
            val filteredArtists = remember(uiState.artists, searchQuery) {
                uiState.artists.filterArtistsByQuery(searchQuery)
            }
            val filteredFolders = remember(uiState.folders, searchQuery) {
                uiState.folders.filterFoldersByQuery(searchQuery)
            }

            if (searchQuery.isNotBlank() &&
                filteredAlbums.isEmpty() &&
                filteredArtists.isEmpty() &&
                filteredFolders.isEmpty()
            ) {
                CatlyticsEmptyState(
                    message = stringResource(LibraryR.string.library_no_search_results),
                    modifier = modifier,
                    messageColor = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LibraryContent(
                    albums = filteredAlbums,
                    artists = filteredArtists,
                    artistViewMode = uiState.artistViewMode,
                    folders = filteredFolders,
                    sortDirection = sortDirection,
                    onSortDirectionChange = onSortDirectionChange,
                    albumsGridState = albumsGridState,
                    artistsListState = artistsListState,
                    artistsGridState = artistsGridState,
                    foldersListState = foldersListState,
                    onAlbumSelected = onAlbumSelected,
                    onArtistSelected = onArtistSelected,
                    onArtistViewModeChange = onArtistViewModeChange,
                    onFolderVisibilityChange = onFolderVisibilityChange,
                    onFolderSelected = onFolderSelected,
                    onAddToPlaylist = onAddToPlaylist,
                    bottomPadding = bottomPadding,
                    scaffoldContentPadding = scaffoldContentPadding,
                    modifier = modifier.fillMaxSize(),
                    collapseFraction = collapseFraction,
                )
            }
        }
    }
}

@Composable
private fun LibraryContent(
    albums: List<Album>,
    artists: List<ArtistSummary>,
    artistViewMode: ArtistViewMode,
    folders: List<LibraryFolder>,
    sortDirection: SortDirection,
    onSortDirectionChange: (SortDirection) -> Unit,
    albumsGridState: LazyGridState,
    artistsListState: LazyListState,
    artistsGridState: LazyGridState,
    foldersListState: LazyListState,
    onAlbumSelected: (Album) -> Unit,
    onArtistSelected: (ArtistSummary) -> Unit,
    onArtistViewModeChange: (ArtistViewMode) -> Unit,
    onFolderVisibilityChange: (String, Boolean) -> Unit,
    onFolderSelected: (LibraryFolder) -> Unit,
    onAddToPlaylist: (PlaylistSource) -> Unit,
    bottomPadding: () -> Dp,
    scaffoldContentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    collapseFraction: () -> Float = { 0f },
) {
    val pagerState = rememberPagerState(pageCount = { LibrarySection.entries.size })
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    var entranceDone by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(ENTRANCE_SETTLE_MILLIS.milliseconds)
        entranceDone = true
    }

    var headerHeightPx by remember { mutableIntStateOf(0) }
    val headerHeight = if (headerHeightPx > 0) {
        with(density) { headerHeightPx.toDp() }
    } else {
        HeaderHeightEstimate
    }
    val scaffoldTopPadding = scaffoldContentPadding.calculateTopPadding()
    val pageTopPadding = scaffoldTopPadding + headerHeight
    val currentPage = pagerState.currentPage
    val currentSection = LibrarySection.entries[currentPage]

    val headerAtTop by remember(currentPage, artistViewMode) {
        derivedStateOf {
            when (LibrarySection.entries[currentPage]) {
                LibrarySection.Albums ->
                    albumsGridState.firstVisibleItemIndex == 0 &&
                        albumsGridState.firstVisibleItemScrollOffset == 0
                LibrarySection.Artists ->
                    if (artistViewMode == ArtistViewMode.List) {
                        artistsListState.firstVisibleItemIndex == 0 &&
                            artistsListState.firstVisibleItemScrollOffset == 0
                    } else {
                        artistsGridState.firstVisibleItemIndex == 0 &&
                            artistsGridState.firstVisibleItemScrollOffset == 0
                    }
                LibrarySection.Folders ->
                    foldersListState.firstVisibleItemIndex == 0 &&
                        foldersListState.firstVisibleItemScrollOffset == 0
            }
        }
    }

    fun selectSortDirection(direction: SortDirection) {
        if (direction == sortDirection) {
            onSortDirectionChange(direction)
            return
        }
        coroutineScope.launch {
            when (LibrarySection.entries[currentPage]) {
                LibrarySection.Albums -> albumsGridState.scrollToItem(0)
                LibrarySection.Artists ->
                    if (artistViewMode == ArtistViewMode.List) {
                        artistsListState.scrollToItem(0)
                    } else {
                        artistsGridState.scrollToItem(0)
                    }
                LibrarySection.Folders -> foldersListState.scrollToItem(0)
            }
            onSortDirectionChange(direction)
        }
    }

    Box(modifier = modifier) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            when (LibrarySection.entries[page]) {
                LibrarySection.Albums -> LibraryAlbumGrid(
                    albums = albums,
                    state = albumsGridState,
                    sortDirection = sortDirection,
                    onAlbumSelected = onAlbumSelected,
                    onAddToPlaylist = { onAddToPlaylist(PlaylistSource.AlbumSource(it.id)) },
                    bottomPadding = bottomPadding,
                    topPadding = pageTopPadding,
                    animateEntrance = !entranceDone,
                )
                LibrarySection.Artists -> LibraryArtistCollection(
                    artists = artists,
                    viewMode = artistViewMode,
                    sortDirection = sortDirection,
                    listState = artistsListState,
                    gridState = artistsGridState,
                    onArtistSelected = onArtistSelected,
                    onAddToPlaylist = {
                        onAddToPlaylist(PlaylistSource.ArtistSource(it.artist.id))
                    },
                    bottomPadding = bottomPadding,
                    topPadding = pageTopPadding,
                    animateEntrance = !entranceDone,
                )
                LibrarySection.Folders -> LibraryFolderList(
                    folders = folders,
                    state = foldersListState,
                    sortDirection = sortDirection,
                    onFolderVisibilityChange = onFolderVisibilityChange,
                    onFolderSelected = onFolderSelected,
                    onAddToPlaylist = { onAddToPlaylist(PlaylistSource.FolderSource(it.id)) },
                    bottomPadding = bottomPadding,
                    topPadding = pageTopPadding,
                    animateEntrance = !entranceDone,
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(top = scaffoldTopPadding),
        ) {
            LibraryHeader(
                currentSection = currentSection,
                onSectionSelected = { section ->
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(section.ordinal)
                    }
                },
                sortDirection = sortDirection,
                onSelectSort = ::selectSortDirection,
                artistViewMode = artistViewMode,
                onArtistViewModeChange = onArtistViewModeChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .onSizeChanged { headerHeightPx = it.height }
                    .then(
                        if (headerAtTop) {
                            Modifier
                        } else {
                            Modifier.graphicsLayer {
                                val f = collapseFraction().coerceIn(0f, 1f)
                                translationY = -f * headerHeightPx
                                alpha = 1f - f
                            }
                        }
                    )
                    .background(MaterialTheme.colorScheme.background),
            )
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun LibraryHeader(
    currentSection: LibrarySection,
    onSectionSelected: (LibrarySection) -> Unit,
    sortDirection: SortDirection,
    onSelectSort: (SortDirection) -> Unit,
    artistViewMode: ArtistViewMode,
    onArtistViewModeChange: (ArtistViewMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        SecondaryTabRow(
            selectedTabIndex = currentSection.ordinal,
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.primary,
            divider = {},
            indicator = {
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier
                        .tabIndicatorOffset(currentSection.ordinal)
                        .padding(horizontal = 20.dp)
                        .clip(MaterialTheme.shapes.extraLarge),
                    color = MaterialTheme.colorScheme.primary,
                )
            },
        ) {
            LibrarySection.entries.forEach { section ->
                Tab(
                    selected = section == currentSection,
                    onClick = { onSectionSelected(section) },
                    text = { Text(stringResource(section.labelRes)) },
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, top = 4.dp, end = 12.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LibrarySortMenuButton(
                sortDirection = sortDirection,
                onSelectSort = onSelectSort,
            )
            AnimatedVisibility(
                visible = currentSection == LibrarySection.Artists,
                enter = fadeIn() + expandHorizontally(expandFrom = Alignment.Start),
                exit = fadeOut() + shrinkHorizontally(shrinkTowards = Alignment.Start),
            ) {
                ArtistViewModeToggle(
                    viewMode = artistViewMode,
                    onViewModeChange = onArtistViewModeChange,
                )
            }
        }
    }
}

@Composable
private fun LibrarySortMenuButton(
    sortDirection: SortDirection,
    onSelectSort: (SortDirection) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val currentLabel = stringResource(sortDirection.labelRes())
    val sortDescription = stringResource(
        LibraryR.string.library_sort_current_content_description,
        currentLabel,
    )
    val arrowRotation by animateFloatAsState(
        targetValue = sortDirection.arrowRotation(),
        label = "librarySortArrow",
    )
    Box {
        Surface(
            onClick = { expanded = true },
            modifier = Modifier
                .pressScale(interactionSource)
                .semantics { contentDescription = sortDescription },
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface,
            interactionSource = interactionSource,
        ) {
            Row(
                modifier = Modifier
                    .height(ControlHeight)
                    .padding(start = 10.dp, end = 14.dp)
                    .clearAndSetSemantics {},
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_down),
                    contentDescription = null,
                    modifier = Modifier
                        .size(18.dp)
                        .graphicsLayer { rotationZ = arrowRotation },
                )
                Text(
                    text = currentLabel,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = RoundedCornerShape(20.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            SortDirection.entries.forEach { direction ->
                val selected = direction == sortDirection
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(direction.labelRes()),
                            color = if (selected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                        )
                    },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_down),
                            contentDescription = null,
                            tint = if (selected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.graphicsLayer {
                                rotationZ = direction.arrowRotation()
                            },
                        )
                    },
                    onClick = {
                        onSelectSort(direction)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun ArtistViewModeToggle(
    viewMode: ArtistViewMode,
    onViewModeChange: (ArtistViewMode) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isList = viewMode == ArtistViewMode.List
    val toggleDescription = if (isList) {
        stringResource(LibraryR.string.library_show_artists_grid_content_description)
    } else {
        stringResource(LibraryR.string.library_show_artists_list_content_description)
    }
    Surface(
        onClick = {
            onViewModeChange(if (isList) ArtistViewMode.Grid else ArtistViewMode.List)
        },
        modifier = Modifier
            .pressScale(interactionSource)
            .semantics { contentDescription = toggleDescription },
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
        interactionSource = interactionSource,
    ) {
        Box(
            modifier = Modifier.size(ControlHeight),
            contentAlignment = Alignment.Center,
        ) {
            Crossfade(
                targetState = isList,
                animationSpec = tween(durationMillis = 180),
                label = "artistViewModeToggle",
            ) { showingList ->
                Icon(
                    painter = painterResource(
                        if (showingList) R.drawable.ic_grid else R.drawable.ic_list_shadow,
                    ),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

private fun SortDirection.labelRes(): Int = when (this) {
    SortDirection.Ascending -> LibraryR.string.library_sort_ascending
    SortDirection.Descending -> LibraryR.string.library_sort_descending
}

private fun SortDirection.arrowRotation(): Float = when (this) {
    SortDirection.Ascending -> 180f
    SortDirection.Descending -> 0f
}

@Composable
private fun PermissionRequiredContent(
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CatlyticsEmptyState(
        message = stringResource(LibraryR.string.library_permission_message),
        modifier = modifier,
        action = {
            Button(onClick = onRequestPermission) {
                Text(stringResource(LibraryR.string.library_permission_request))
            }
        },
    )
}

@Composable
private fun LoadingContent(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

private val ControlHeight = 36.dp

/** Altura aproximada del header antes de la primera medición (tabs 48dp + controles 48dp + paddings). */
private val HeaderHeightEstimate = 108.dp
private const val ENTRANCE_SETTLE_MILLIS = 900L

@Preview(name = "Phone", widthDp = 390, heightDp = 844, showBackground = true)
@Preview(name = "Tablet", widthDp = 800, heightDp = 1280, showBackground = true)
@Composable
private fun LibraryScreenPreview() {
    CatlyticsTheme {
        LibraryScreen(
            uiState = LibraryUiState.Success(
                albums = listOf(
                    Album(
                        id = "album-1",
                        title = "Midnight Signals",
                        artist = Artist("artist-1", "Catlytics"),
                        trackCount = 10,
                    ),
                ),
                artists = listOf(
                    ArtistSummary(
                        artist = Artist("artist-1", "Catlytics"),
                        albumCount = 2,
                        trackCount = 10,
                    ),
                ),
                artistViewMode = ArtistViewMode.List,
                sortDirection = SortDirection.Ascending,
                folders = listOf(
                    LibraryFolder(
                        id = "external:Music",
                        name = "Music",
                        path = "Music",
                        trackCount = 24,
                        isVisible = true,
                    ),
                    LibraryFolder(
                        id = "external:Android",
                        name = "Android",
                        path = "Android",
                        trackCount = 128,
                        isVisible = false,
                    ),
                ),
            ),
            hasAudioPermission = true,
            onRequestPermission = {},
            onAlbumSelected = {},
            onArtistSelected = {},
            onArtistViewModeChange = {},
            onFolderVisibilityChange = { _, _ -> },
            onFolderSelected = {},
            onAddToPlaylist = {},
            searchQuery = "",
            sortDirection = SortDirection.Ascending,
            onSortDirectionChange = {},
        )
    }
}
