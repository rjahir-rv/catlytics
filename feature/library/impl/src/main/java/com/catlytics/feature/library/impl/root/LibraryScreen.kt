package com.catlytics.feature.library.impl.root

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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
) {
    // Hoist scroll states (using Saver for better stability across recompositions and sort changes)
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
            // Stabilize the base (search-filtered) lists so that only actual search changes cause new list refs.
            // Sorting will be done inside the leaf list components.
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
                    modifier = modifier.fillMaxSize(),
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
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberPagerState(pageCount = { LibrarySection.entries.size })
    val coroutineScope = rememberCoroutineScope()

    Column(modifier = modifier) {
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

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
        ) { page ->
            when (LibrarySection.entries[page]) {
                LibrarySection.Albums -> LibraryAlbumGrid(
                    albums = albums,
                    state = albumsGridState,
                    sortDirection = sortDirection,
                    onSortDirectionChange = onSortDirectionChange,
                    onAlbumSelected = onAlbumSelected,
                    onAddToPlaylist = { onAddToPlaylist(PlaylistSource.AlbumSource(it.id)) },
                    bottomPadding = bottomPadding,
                )
                LibrarySection.Artists -> LibraryArtistCollection(
                    artists = artists,
                    viewMode = artistViewMode,
                    onViewModeChange = onArtistViewModeChange,
                    sortDirection = sortDirection,
                    onSortDirectionChange = onSortDirectionChange,
                    listState = artistsListState,
                    gridState = artistsGridState,
                    onArtistSelected = onArtistSelected,
                    onAddToPlaylist = {
                        onAddToPlaylist(PlaylistSource.ArtistSource(it.artist.id))
                    },
                    bottomPadding = bottomPadding,
                )
                LibrarySection.Folders -> LibraryFolderList(
                    folders = folders,
                    state = foldersListState,
                    sortDirection = sortDirection,
                    onSortDirectionChange = onSortDirectionChange,
                    onFolderVisibilityChange = onFolderVisibilityChange,
                    onFolderSelected = onFolderSelected,
                    onAddToPlaylist = { onAddToPlaylist(PlaylistSource.FolderSource(it.id)) },
                    bottomPadding = bottomPadding,
                )
            }
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
