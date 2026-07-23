package com.catlytics.feature.playlists.impl

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.catlytics.core.model.Track

@Composable
internal fun PlaylistDetailRoute(
    playlistId: String,
    onTrackOptions: (track: Track, onRemoveFromPlaylist: () -> Unit) -> Unit,
    onTopBarColorChange: (Color) -> Unit,
    onDeleted: () -> Unit,
    bottomPadding: () -> Dp = { 0.dp },
    viewModel: PlaylistDetailViewModel = hiltViewModel(key = playlistId),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
    val allTracks by viewModel.allTracks.collectAsStateWithLifecycle()

    LaunchedEffect(playlistId) { viewModel.open(playlistId) }
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                PlaylistDetailEffect.Deleted -> onDeleted()
                is PlaylistDetailEffect.Message -> Toast.makeText(
                    context,
                    effect.text,
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }
    }

    PlaylistDetailScreen(
        uiState = uiState,
        playbackState = playbackState,
        allTracks = allTracks,
        onPlay = viewModel::play,
        onPlayShuffled = viewModel::playShuffled,
        onTrackOptions = { track ->
            onTrackOptions(track) {
                viewModel.remove(track.id)
                Toast.makeText(
                    context,
                    "${track.title} eliminada de la playlist",
                    Toast.LENGTH_SHORT,
                ).show()
            }
        },
        onTogglePlayback = viewModel::togglePlayback,
        onSaveDetails = viewModel::saveDetails,
        onSaveOrder = viewModel::saveOrder,
        onAddTracks = viewModel::addTracks,
        onDelete = viewModel::delete,
        onTopBarColorChange = onTopBarColorChange,
        bottomPadding = bottomPadding,
    )
}
