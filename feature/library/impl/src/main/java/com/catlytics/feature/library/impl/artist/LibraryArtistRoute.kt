package com.catlytics.feature.library.impl.artist

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.catlytics.core.model.Album
import com.catlytics.feature.library.api.LibraryArtistRoute
import com.catlytics.core.model.PlaylistSource

@Composable
internal fun LibraryArtistRoute(
    route: LibraryArtistRoute,
    onAlbumSelected: (Album) -> Unit,
    onAddToPlaylist: (PlaylistSource) -> Unit,
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
    )
}
