package com.catlytics.feature.playlists.impl

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.catlytics.core.designsystem.text.resolve
import com.catlytics.core.model.Track
import com.catlytics.core.model.TrackSelectionAction

@Composable
internal fun PlaylistDetailRoute(
    playlistId: String,
    onTrackOptions: (track: Track, onRemoveFromPlaylist: () -> Unit) -> Unit,
    likedTrackIds: Set<String> = emptySet(),
    onTrackSelectionAction: (TrackSelectionAction) -> Unit = {},
    onTopBarColorChange: (Color) -> Unit,
    onDeleted: () -> Unit,
    bottomPadding: () -> Dp = { 0.dp },
    scaffoldContentPadding: PaddingValues = PaddingValues(0.dp),
    viewModel: PlaylistDetailViewModel = hiltViewModel(key = playlistId),
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
    val allTracks by viewModel.allTracks.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val undoLabel = stringResource(R.string.playlist_detail_undo)

    LaunchedEffect(playlistId) { viewModel.open(playlistId) }
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                PlaylistDetailEffect.Deleted -> onDeleted()
                is PlaylistDetailEffect.Message -> Toast.makeText(
                    context,
                    effect.text.resolve(resources),
                    Toast.LENGTH_SHORT,
                ).show()
                is PlaylistDetailEffect.TracksRemoved -> {
                    snackbarHostState.currentSnackbarData?.dismiss()
                    launch {
                        val result = snackbarHostState.showSnackbar(
                            message = effect.text.resolve(resources),
                            actionLabel = undoLabel,
                            duration = SnackbarDuration.Long,
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            viewModel.undoRemove(effect.previousTrackIds)
                        }
                    }
                }
            }
        }
    }

    val exportM3uLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("audio/x-mpegurl"),
    ) { uri ->
        if (uri != null) {
            viewModel.exportM3u(uri.toString())
        }
    }

    PlaylistDetailScreen(
        uiState = uiState,
        playbackState = playbackState,
        allTracks = allTracks,
        onPlay = viewModel::play,
        onPlayShuffled = viewModel::playShuffled,
        onTrackOptions = { track ->
            onTrackOptions(track) { viewModel.remove(track.id) }
        },
        likedTrackIds = likedTrackIds,
        onTrackSelectionAction = onTrackSelectionAction,
        onRemoveSelected = viewModel::remove,
        onTogglePlayback = viewModel::togglePlayback,
        onSaveDetails = viewModel::saveDetails,
        onSaveOrder = viewModel::saveOrder,
        onAddTracks = { trackIds, onAdded ->
            viewModel.addTracks(
                trackIds = trackIds,
                title = resources.getString(R.string.playlist_detail_selection_title),
                onAdded = onAdded,
            )
        },
        onDelete = viewModel::delete,
        onExportM3u = {
            val name = (uiState as? PlaylistDetailUiState.Success)?.content?.playlist?.name ?: "playlist"
            exportM3uLauncher.launch("$name.m3u8")
        },
        onTopBarColorChange = onTopBarColorChange,
        snackbarHostState = snackbarHostState,
        bottomPadding = bottomPadding,
        scaffoldContentPadding = scaffoldContentPadding,
    )
}
