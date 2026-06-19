package com.catlytics.feature.library.impl.root

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.catlytics.core.model.Album
import com.catlytics.core.model.ArtistSummary
import com.catlytics.core.model.LibraryFolder
import com.catlytics.core.model.PlaylistSource
import com.catlytics.core.model.SortDirection
import com.catlytics.feature.library.impl.root.LibraryUiState

@Composable
internal fun LibraryRoute(
    searchQuery: String = "",
    onAlbumSelected: (Album) -> Unit,
    onArtistSelected: (ArtistSummary) -> Unit,
    onFolderSelected: (LibraryFolder) -> Unit,
    onAddToPlaylist: (PlaylistSource) -> Unit,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val permission = requiredAudioPermission()
    var hasAudioPermission by remember(permission) {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                permission,
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED,
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        hasAudioPermission = isGranted
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(hasAudioPermission) {
        if (hasAudioPermission) {
            viewModel.refreshLibraryOnce()
        }
    }

    LibraryScreen(
        uiState = uiState,
        hasAudioPermission = hasAudioPermission,
        onRequestPermission = { permissionLauncher.launch(permission) },
        onAlbumSelected = onAlbumSelected,
        onArtistSelected = onArtistSelected,
        onArtistViewModeChange = viewModel::setArtistViewMode,
        onFolderVisibilityChange = viewModel::setFolderVisible,
        onFolderSelected = onFolderSelected,
        onAddToPlaylist = onAddToPlaylist,
        searchQuery = searchQuery,
        sortDirection = (uiState as? LibraryUiState.Success)?.sortDirection ?: SortDirection.Ascending,
        onSortDirectionChange = viewModel::setSortDirection,
    )
}

private fun requiredAudioPermission(): String =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
