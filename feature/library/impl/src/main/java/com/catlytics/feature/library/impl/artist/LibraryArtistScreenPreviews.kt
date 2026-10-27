package com.catlytics.feature.library.impl.artist

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.catlytics.core.designsystem.layout.FormFactorPreviews
import com.catlytics.core.designsystem.theme.CatlyticsTheme
import com.catlytics.core.model.Album
import com.catlytics.core.model.Artist
import com.catlytics.core.model.ArtistContent
import com.catlytics.core.model.ArtistSummary
import com.catlytics.core.model.Track

private class LibraryArtistUiStatePreviewProvider : PreviewParameterProvider<LibraryArtistUiState> {
    override val values = sequenceOf(
        LibraryArtistUiState.Success(previewArtistContent()),
        LibraryArtistUiState.NotFound,
    )
}

@FormFactorPreviews
@Composable
private fun LibraryArtistScreenPreview(
    @PreviewParameter(LibraryArtistUiStatePreviewProvider::class) uiState: LibraryArtistUiState,
) {
    CatlyticsTheme {
        LibraryArtistScreen(
            uiState = uiState,
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
