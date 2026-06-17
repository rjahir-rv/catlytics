package com.catlytics.feature.library.impl.album

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.catlytics.feature.library.api.LibraryAlbumRoute
import com.catlytics.core.model.Track

@Composable
internal fun LibraryAlbumRoute(
    route: LibraryAlbumRoute,
    onTrackOptions: (Track) -> Unit,
    viewModel: LibraryAlbumViewModel = hiltViewModel(key = route.albumId),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(route.albumId) {
        viewModel.openAlbum(route.albumId)
    }

    LibraryAlbumScreen(
        uiState = uiState,
        onTrackSelected = viewModel::playTrack,
        onTrackOptions = onTrackOptions,
    )
}
