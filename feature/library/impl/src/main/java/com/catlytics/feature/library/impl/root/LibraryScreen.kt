package com.catlytics.feature.library.impl.root

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.catlytics.core.designsystem.R
import com.catlytics.core.designsystem.theme.CatlyticsTheme
import com.catlytics.core.model.Album
import com.catlytics.core.model.Artist
import com.catlytics.core.model.ArtistSummary
import com.catlytics.core.model.ArtistViewMode
import com.catlytics.core.model.LibraryFolder
import com.catlytics.core.model.PlaylistSource
import com.catlytics.core.model.SortDirection
import com.catlytics.feature.library.impl.filterAlbumsByQuery
import com.catlytics.feature.library.impl.filterArtistsByQuery
import com.catlytics.feature.library.impl.filterFoldersByQuery
import kotlinx.coroutines.launch

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
        LibraryUiState.Empty -> EmptyContent(modifier)
        is LibraryUiState.Error -> MessageContent(uiState.message, modifier)
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
                NoSearchResultsContent(modifier)
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
@OptIn(ExperimentalMaterial3Api::class)
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

    val pageTopPadding = scaffoldContentPadding.calculateTopPadding() + 48.dp
    val scaffoldTopPadding = scaffoldContentPadding.calculateTopPadding()
    val currentPage = pagerState.currentPage

    // El header (tabs + controles) se mueve con la MISMA señal del topbar
    // (collapsedFraction). En reposo no se aplica NINGUNA transformación: es una
    // columna estática y por construcción imposible de ocultar o atorar.
    var headerHeightPx by remember { mutableIntStateOf(0) }
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
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(top = scaffoldTopPadding),
        ) {
            Column(
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
            ) {
                SecondaryTabRow(
                    selectedTabIndex = pagerState.currentPage,
                    containerColor = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.primary,
                    divider = {},
                    indicator = {
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier
                                .tabIndicatorOffset(pagerState.currentPage)
                                .padding(horizontal = 20.dp)
                                .clip(MaterialTheme.shapes.extraLarge),
                            color = MaterialTheme.colorScheme.primary,
                        )
                    },
                ) {
                    LibrarySection.entries.forEachIndexed { index, section ->
                        Tab(
                            selected = index == pagerState.currentPage,
                            onClick = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            },
                            text = { Text(section.label) },
                        )
                    }
                }
                when (LibrarySection.entries[currentPage]) {
                    LibrarySection.Albums, LibrarySection.Folders -> LibraryHeaderSortRow(
                        onSelectSort = ::selectSortDirection,
                    )
                    LibrarySection.Artists -> LibraryArtistHeaderControls(
                        viewMode = artistViewMode,
                        onViewModeChange = onArtistViewModeChange,
                        onSelectSort = ::selectSortDirection,
                    )
                }
            }
        }

    }
}

@Composable
private fun LibraryHeaderSortRow(
    onSelectSort: (SortDirection) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.End,
    ) {
        LibrarySortMenuButton(onSelectSort = onSelectSort)
    }
}

@Composable
private fun LibraryArtistHeaderControls(
    viewMode: ArtistViewMode,
    onViewModeChange: (ArtistViewMode) -> Unit,
    onSelectSort: (SortDirection) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Spacer(modifier = Modifier.weight(1f))
        LibrarySortMenuButton(onSelectSort = onSelectSort)
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

@Composable
private fun LibrarySortMenuButton(
    onSelectSort: (SortDirection) -> Unit,
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
                    onSelectSort(SortDirection.Ascending)
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
                    onSelectSort(SortDirection.Descending)
                    expanded = false
                }
            )
        }
    }
}

private enum class LibrarySection(val label: String) {
    Albums("Álbumes"),
    Artists("Artistas"),
    Folders("Carpetas"),
}

@Composable
private fun PermissionRequiredContent(
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Catlytics necesita permiso para encontrar tu biblioteca musical.",
            style = MaterialTheme.typography.bodyLarge,
        )
        Button(onClick = onRequestPermission) {
            Text("Permitir acceso a música")
        }
    }
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

@Composable
private fun EmptyContent(modifier: Modifier = Modifier) {
    MessageContent(
        message = "No encontramos música en este dispositivo.",
        modifier = modifier,
    )
}

@Composable
private fun MessageContent(
    message: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun NoSearchResultsContent(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "No encontramos álbumes ni artistas que coincidan con tu búsqueda.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

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
