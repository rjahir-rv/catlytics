package com.catlytics.feature.library.impl.folder

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.catlytics.core.model.LibraryFolder
import com.catlytics.feature.library.api.LibraryFolderRoute
import com.catlytics.core.model.PlaylistSource

@Composable
internal fun LibraryFolderRoute(
    route: LibraryFolderRoute,
    onFolderSelected: (LibraryFolder) -> Unit,
    onAddToPlaylist: (PlaylistSource) -> Unit,
    viewModel: LibraryFolderViewModel = hiltViewModel(key = route.folderId),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(route.folderId) {
        viewModel.openFolder(route.folderId)
    }

    LibraryFolderScreen(
        uiState = uiState,
        onFolderSelected = onFolderSelected,
        onTrackSelected = viewModel::playTrack,
        onAddToPlaylist = onAddToPlaylist,
    )
}
