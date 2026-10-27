package com.catlytics.feature.home.impl

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.catlytics.core.designsystem.layout.FormFactorPreviews
import com.catlytics.core.designsystem.theme.CatlyticsTheme
import com.catlytics.core.model.Artist
import com.catlytics.core.model.Track

private class HomeUiStatePreviewProvider : PreviewParameterProvider<HomeUiState> {
    override val values = sequenceOf(
        HomeUiState.Success(
            tracks = listOf(
                Track(
                    id = "track-current",
                    title = "Electric Feel",
                    artist = Artist(id = "artist-preview", name = "MGMT"),
                    durationMillis = 186_000,
                    mediaUri = "content://media/external/audio/media/1",
                ),
                Track(
                    id = "track-preview",
                    title = "Canción local con un título bastante largo",
                    artist = Artist(id = "artist-local", name = "Artista local"),
                    durationMillis = 242_000,
                    mediaUri = "content://media/external/audio/media/2",
                ),
            ),
            currentTrackId = "track-current",
        ),
        HomeUiState.Empty,
    )
}

@FormFactorPreviews
@Composable
private fun HomeScreenPreview(
    @PreviewParameter(HomeUiStatePreviewProvider::class) uiState: HomeUiState,
) {
    CatlyticsTheme {
        HomeScreen(
            uiState = uiState,
            searchQuery = "",
            hasAudioPermission = true,
            onRequestPermission = {},
            onTrackSelected = { _, _ -> },
            onTrackOptions = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPermissionRequiredPreview() {
    CatlyticsTheme {
        HomeScreen(
            uiState = HomeUiState.Empty,
            searchQuery = "",
            hasAudioPermission = false,
            onRequestPermission = {},
            onTrackSelected = { _, _ -> },
            onTrackOptions = {},
        )
    }
}
