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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.catlytics.core.designsystem.theme.CatlyticsTheme
import com.catlytics.core.model.Album
import com.catlytics.core.model.Artist
import com.catlytics.core.model.ArtistSummary
import com.catlytics.core.model.ArtistViewMode
import com.catlytics.core.model.LibraryFolder
import kotlinx.coroutines.launch

@Composable
internal fun LibraryScreen(
    uiState: LibraryUiState,
    hasAudioPermission: Boolean,
    onRequestPermission: () -> Unit,
    onAlbumSelected: (Album) -> Unit,
    onArtistSelected: (ArtistSummary) -> Unit,
    onArtistViewModeChange: (ArtistViewMode) -> Unit,
    onFolderVisibilityChange: (String, Boolean) -> Unit,
    onFolderSelected: (LibraryFolder) -> Unit,
    modifier: Modifier = Modifier,
) {
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
        is LibraryUiState.Success -> LibraryContent(
            uiState = uiState,
            onAlbumSelected = onAlbumSelected,
            onArtistSelected = onArtistSelected,
            onArtistViewModeChange = onArtistViewModeChange,
            onFolderVisibilityChange = onFolderVisibilityChange,
            onFolderSelected = onFolderSelected,
            modifier = modifier.fillMaxSize(),
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun LibraryContent(
    uiState: LibraryUiState.Success,
    onAlbumSelected: (Album) -> Unit,
    onArtistSelected: (ArtistSummary) -> Unit,
    onArtistViewModeChange: (ArtistViewMode) -> Unit,
    onFolderVisibilityChange: (String, Boolean) -> Unit,
    onFolderSelected: (LibraryFolder) -> Unit,
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
                    albums = uiState.albums,
                    onAlbumSelected = onAlbumSelected,
                )
                LibrarySection.Artists -> LibraryArtistCollection(
                    artists = uiState.artists,
                    viewMode = uiState.artistViewMode,
                    onViewModeChange = onArtistViewModeChange,
                    onArtistSelected = onArtistSelected,
                )
                LibrarySection.Folders -> LibraryFolderList(
                    folders = uiState.folders,
                    onFolderVisibilityChange = onFolderVisibilityChange,
                    onFolderSelected = onFolderSelected,
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
        )
    }
}
