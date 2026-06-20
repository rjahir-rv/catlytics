package com.catlytics.feature.library.impl.artist

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.catlytics.core.model.Album
import com.catlytics.core.model.PlaylistSource
import com.catlytics.core.model.Track
import com.catlytics.feature.library.api.LibraryArtistRoute

@Composable
internal fun LibraryArtistRoute(
    route: LibraryArtistRoute,
    onAlbumSelected: (Album) -> Unit,
    onAddToPlaylist: (PlaylistSource) -> Unit,
    onTrackOptions: (Track) -> Unit,
    onTopBarColorChange: (Color) -> Unit,
    bottomPadding: () -> Dp = { 0.dp },
    viewModel: LibraryArtistViewModel = hiltViewModel(key = route.artistId),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(route.artistId) {
        viewModel.openArtist(route.artistId)
    }

    LibraryArtistScreen(
        uiState = uiState,
        onAlbumSelected = onAlbumSelected,
        onTrackSelected = viewModel::playTrack,
        onAddToPlaylist = onAddToPlaylist,
        onTrackOptions = onTrackOptions,
        onTopBarColorChange = onTopBarColorChange,
        bottomPadding = bottomPadding,
    )
}
