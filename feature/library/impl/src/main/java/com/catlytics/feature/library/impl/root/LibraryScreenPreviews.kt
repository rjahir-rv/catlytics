package com.catlytics.feature.library.impl.root

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.catlytics.core.designsystem.layout.FormFactorPreviews
import com.catlytics.core.designsystem.theme.CatlyticsTheme
import com.catlytics.core.model.Album
import com.catlytics.core.model.Artist
import com.catlytics.core.model.ArtistSummary
import com.catlytics.core.model.ArtistViewMode
import com.catlytics.core.model.LibraryFolder
import com.catlytics.core.model.SortDirection

private class LibraryUiStatePreviewProvider : PreviewParameterProvider<LibraryUiState> {
    override val values = sequenceOf(
        LibraryUiState.Success(
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
        LibraryUiState.Empty,
    )
}

@FormFactorPreviews
@Composable
private fun LibraryScreenPreview(
    @PreviewParameter(LibraryUiStatePreviewProvider::class) uiState: LibraryUiState,
) {
    CatlyticsTheme {
        LibraryScreen(
            uiState = uiState,
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
