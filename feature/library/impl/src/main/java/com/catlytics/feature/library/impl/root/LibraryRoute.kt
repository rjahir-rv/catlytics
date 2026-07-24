package com.catlytics.feature.library.impl.root

import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.catlytics.core.model.Album
import com.catlytics.core.model.ArtistSummary
import com.catlytics.core.model.LibraryFolder
import com.catlytics.core.model.PlaylistSource
import com.catlytics.core.model.SortDirection

@Composable
internal fun LibraryRoute(
    searchQuery: String = "",
    onAlbumSelected: (Album) -> Unit,
    onArtistSelected: (ArtistSummary) -> Unit,
    onFolderSelected: (LibraryFolder) -> Unit,
    onAddToPlaylist: (PlaylistSource) -> Unit,
    hasAudioPermission: Boolean,
    onRequestPermission: () -> Unit,
    bottomPadding: () -> Dp = { 0.dp },
    scaffoldContentPadding: PaddingValues = PaddingValues(0.dp),
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LibraryScreen(
        uiState = uiState,
        hasAudioPermission = hasAudioPermission,
        onRequestPermission = onRequestPermission,
        onAlbumSelected = onAlbumSelected,
        onArtistSelected = onArtistSelected,
        onArtistViewModeChange = viewModel::setArtistViewMode,
        onFolderVisibilityChange = viewModel::setFolderVisible,
        onFolderSelected = onFolderSelected,
        onAddToPlaylist = onAddToPlaylist,
        searchQuery = searchQuery,
        sortDirection = (uiState as? LibraryUiState.Success)?.sortDirection ?: SortDirection.Ascending,
        onSortDirectionChange = viewModel::setSortDirection,
        bottomPadding = bottomPadding,
        scaffoldContentPadding = scaffoldContentPadding,
    )
}
