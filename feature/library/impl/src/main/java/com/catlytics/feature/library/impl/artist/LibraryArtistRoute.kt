package com.catlytics.feature.library.impl.artist

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.catlytics.core.designsystem.component.isSearching
import com.catlytics.core.designsystem.text.resolve
import com.catlytics.core.model.Album
import com.catlytics.core.model.PlaylistSource
import com.catlytics.core.model.Track
import com.catlytics.core.model.TrackSelectionAction
import com.catlytics.feature.library.api.LibraryArtistRoute

@Composable
internal fun LibraryArtistRoute(
    route: LibraryArtistRoute,
    onAlbumSelected: (Album) -> Unit,
    onAddToPlaylist: (PlaylistSource) -> Unit,
    onTrackOptions: (Track) -> Unit,
    likedTrackIds: Set<String> = emptySet(),
    currentTrackId: String? = null,
    isCurrentTrackPlaying: Boolean = false,
    onTrackSelectionAction: (TrackSelectionAction) -> Unit = {},
    onTopBarColorChange: (Color) -> Unit,
    searchQuery: String,
    isSearchActive: Boolean = false,
    bottomPadding: () -> Dp = { 0.dp },
    scaffoldContentPadding: PaddingValues = PaddingValues(0.dp),
    viewModel: LibraryArtistViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val resources = LocalResources.current

    val artworkPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri: Uri? ->
        uri?.let { viewModel.changeArtwork(it.toString()) }
    }

    LaunchedEffect(route.artistId) {
        viewModel.openArtist(route.artistId)
    }

    LaunchedEffect(searchQuery) {
        viewModel.onSearchQueryChanged(searchQuery)
    }

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { message ->
            Toast.makeText(context, message.resolve(resources), Toast.LENGTH_SHORT).show()
        }
    }

    LibraryArtistScreen(
        uiState = uiState,
        onAlbumSelected = onAlbumSelected,
        onTrackSelected = viewModel::playTrack,
        onAddToPlaylist = onAddToPlaylist,
        onTrackOptions = onTrackOptions,
        likedTrackIds = likedTrackIds,
        currentTrackId = currentTrackId,
        isCurrentTrackPlaying = isCurrentTrackPlaying,
        onTrackSelectionAction = onTrackSelectionAction,
        onTopBarColorChange = onTopBarColorChange,
        isSearching = isSearching(isSearchActive, searchQuery),
        bottomPadding = bottomPadding,
        scaffoldContentPadding = scaffoldContentPadding,
        onShowMergePicker = viewModel::showMergePicker,
        onMergeQueryChange = viewModel::updateMergeQuery,
        onMergeTargetSelected = viewModel::selectMergeTarget,
        onShowAliasManager = viewModel::showAliasManager,
        onUnmergeRequested = viewModel::requestUnmerge,
        onDismissMergeDialog = viewModel::dismissMergeDialog,
        onConfirmMerge = viewModel::confirmMerge,
        onConfirmUnmerge = viewModel::confirmUnmerge,
        onShowRename = viewModel::showRename,
        onChangeArtwork = {
            artworkPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        },
        onRestoreArtwork = viewModel::restoreArtwork,
        onRenameNameChange = viewModel::updateRenameName,
        onConfirmRename = viewModel::confirmRename,
        onPlayShuffled = viewModel::playShuffled,
        onTrackSortChange = viewModel::setTrackSort,
        onAlbumSortChange = viewModel::setAlbumSort,
    )
}
