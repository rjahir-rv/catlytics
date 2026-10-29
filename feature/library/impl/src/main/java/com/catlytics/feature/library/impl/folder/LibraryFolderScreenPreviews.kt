package com.catlytics.feature.library.impl.folder

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.catlytics.core.designsystem.layout.FormFactorPreviews
import com.catlytics.core.designsystem.theme.CatlyticsTheme
import com.catlytics.core.model.Artist
import com.catlytics.core.model.LibraryFolder
import com.catlytics.core.model.LibraryFolderContent
import com.catlytics.core.model.Track

private class LibraryFolderUiStatePreviewProvider : PreviewParameterProvider<LibraryFolderUiState> {
    override val values = sequenceOf(
        LibraryFolderUiState.Success(
            LibraryFolderContent(
                folder = LibraryFolder(
                    id = "external:Music",
                    name = "Music",
                    path = "Music",
                    trackCount = 3,
                    isVisible = true,
                ),
                subfolders = listOf(
                    LibraryFolder(
                        id = "external:Music/Live",
                        name = "Live",
                        path = "Music/Live",
                        trackCount = 12,
                        isVisible = true,
                    ),
                ),
                tracks = listOf(
                    Track(
                        id = "track-1",
                        title = "Midnight Signals",
                        artist = Artist("artist-1", "Catlytics"),
                        durationMillis = 215_000,
                        mediaUri = "content://media/track-1",
                    ),
                ),
            ),
        ),
        LibraryFolderUiState.NotFound,
    )
}

@FormFactorPreviews
@Composable
private fun LibraryFolderScreenPreview(
    @PreviewParameter(LibraryFolderUiStatePreviewProvider::class) uiState: LibraryFolderUiState,
) {
    CatlyticsTheme {
        LibraryFolderScreen(
            uiState = uiState,
            onFolderSelected = {},
            onTrackSelected = { _, _ -> },
            onAddToPlaylist = {},
            onTrackOptions = {},
            currentTrackId = "track-1",
        )
    }
}
