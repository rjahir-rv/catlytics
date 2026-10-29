package com.catlytics.feature.playlists.impl

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.catlytics.core.designsystem.layout.FormFactorPreviews
import com.catlytics.core.designsystem.theme.CatlyticsTheme
import com.catlytics.core.model.Playlist
import com.catlytics.core.model.PlaylistSummary
import com.catlytics.core.model.PlaylistViewMode

private fun previewSummary(
    id: String,
    name: String,
    trackCount: Int,
    artworks: List<String> = emptyList(),
) = PlaylistSummary(
    playlist = Playlist(id = id, name = name, trackIds = List(trackCount) { "$id-$it" }),
    trackCount = trackCount,
    totalDurationMillis = trackCount * 210_000L,
    coverArtworkUris = artworks,
)

private val previewContent = PlaylistsUiState.Content(
    listOf(
        previewSummary("p1", "Focus", 12),
        previewSummary("p2", "Gym", 1),
        previewSummary("p3", "Road trip 2025", 0),
    ),
)

@FormFactorPreviews
@Composable
private fun PlaylistsScreenListPreview() {
    CatlyticsTheme {
        PlaylistsScreen(
            uiState = previewContent,
            viewMode = PlaylistViewMode.List,
            activePlaylist = ActivePlaylist(id = "p1", isPlaying = true),
            onViewModeChange = {},
            onPlaylistSelected = {},
            onCreateClick = {},
            onRename = { _, _ -> },
            onDelete = {},
            onSetCover = { _, _ -> },
        )
    }
}

@FormFactorPreviews
@Composable
private fun PlaylistsScreenMosaicPreview() {
    CatlyticsTheme {
        PlaylistsScreen(
            uiState = previewContent,
            viewMode = PlaylistViewMode.Mosaic,
            onViewModeChange = {},
            onPlaylistSelected = {},
            onCreateClick = {},
            onRename = { _, _ -> },
            onDelete = {},
            onSetCover = { _, _ -> },
        )
    }
}

private class PlaylistsUiStatePreviewProvider : PreviewParameterProvider<PlaylistsUiState> {
    override val values = sequenceOf(
        PlaylistsUiState.Content(emptyList()),
        PlaylistsUiState.Loading,
    )
}

@Preview(showBackground = true)
@Composable
private fun PlaylistsScreenStatePreview(
    @PreviewParameter(PlaylistsUiStatePreviewProvider::class) uiState: PlaylistsUiState,
) {
    CatlyticsTheme {
        PlaylistsScreen(
            uiState = uiState,
            viewMode = PlaylistViewMode.List,
            onViewModeChange = {},
            onPlaylistSelected = {},
            onCreateClick = {},
            onRename = { _, _ -> },
            onDelete = {},
            onSetCover = { _, _ -> },
        )
    }
}
