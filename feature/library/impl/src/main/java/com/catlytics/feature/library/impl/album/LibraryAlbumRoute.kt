package com.catlytics.feature.library.impl.album

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.catlytics.core.model.Artist
import com.catlytics.core.model.PlaylistSource
import com.catlytics.core.model.Track
import com.catlytics.core.model.TrackSelectionAction
import com.catlytics.feature.library.api.LibraryAlbumRoute
import com.catlytics.feature.library.impl.metadata.BulkEditMode
import com.catlytics.feature.library.impl.metadata.EditTracksMetadataSheet

@Composable
internal fun LibraryAlbumRoute(
    route: LibraryAlbumRoute,
    onTrackOptions: (Track) -> Unit,
    onAddToPlaylist: (PlaylistSource) -> Unit = {},
    onArtistSelected: (Artist) -> Unit = {},
    likedTrackIds: Set<String> = emptySet(),
    currentTrackId: String? = null,
    isCurrentTrackPlaying: Boolean = false,
    onTrackSelectionAction: (TrackSelectionAction) -> Unit = {},
    onTopBarColorChange: (Color) -> Unit,
    onTitleChange: (String) -> Unit = {},
    bottomPadding: () -> Dp = { 0.dp },
    scaffoldContentPadding: PaddingValues = PaddingValues(0.dp),
    viewModel: LibraryAlbumViewModel = hiltViewModel(key = route.albumId),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var editingTrackIds by remember { mutableStateOf<List<String>?>(null) }

    LaunchedEffect(route.albumId) {
        viewModel.openAlbum(route.albumId)
    }

    val albumTitle = (uiState as? LibraryAlbumUiState.Success)?.content?.album?.title
    LaunchedEffect(albumTitle) {
        albumTitle?.let(onTitleChange)
    }

    editingTrackIds?.let { trackIds ->
        EditTracksMetadataSheet(
            trackIds = trackIds,
            onDismiss = { editingTrackIds = null },
            mode = BulkEditMode.Album,
        )
    }

    LibraryAlbumScreen(
        uiState = uiState,
        onTrackSelected = viewModel::playTrack,
        onPlayShuffled = viewModel::playShuffled,
        onTrackOptions = onTrackOptions,
        onEditAlbum = { tracks -> editingTrackIds = tracks.map(Track::id) },
        onAddToPlaylist = onAddToPlaylist,
        onArtistSelected = onArtistSelected,
        onTrackSortChange = viewModel::setTrackSort,
        likedTrackIds = likedTrackIds,
        currentTrackId = currentTrackId,
        isCurrentTrackPlaying = isCurrentTrackPlaying,
        onTrackSelectionAction = onTrackSelectionAction,
        onTopBarColorChange = onTopBarColorChange,
        bottomPadding = bottomPadding,
        scaffoldContentPadding = scaffoldContentPadding,
    )
}
