package com.catlytics.feature.home.impl

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.catlytics.core.model.Track
import com.catlytics.core.model.TrackSelectionAction
import com.catlytics.feature.home.impl.R as HomeR

@Composable
internal fun DailyPlaylistRoute(
    modifier: Modifier = Modifier,
    onTrackOptions: (Track) -> Unit,
    likedTrackIds: Set<String> = emptySet(),
    onTrackSelectionAction: (TrackSelectionAction) -> Unit = {},
    bottomPadding: () -> Dp = { 0.dp },
    scaffoldContentPadding: PaddingValues = PaddingValues(0.dp),
    viewModel: DailyPlaylistViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    DailyPlaylistScreen(
        uiState = uiState,
        onTrackSelected = viewModel::onTrackSelected,
        onPlayAll = viewModel::onPlayAll,
        onShuffle = viewModel::onShuffle,
        onTrackOptions = onTrackOptions,
        likedTrackIds = likedTrackIds,
        onTrackSelectionAction = onTrackSelectionAction,
        bottomPadding = bottomPadding,
        scaffoldContentPadding = scaffoldContentPadding,
        modifier = modifier,
    )
}

@Composable
internal fun DailyPlaylistScreen(
    modifier: Modifier = Modifier,
    uiState: DailyPlaylistUiState,
    onTrackSelected: (Track, List<Track>) -> Unit,
    onTrackOptions: (Track) -> Unit,
    likedTrackIds: Set<String> = emptySet(),
    onTrackSelectionAction: (TrackSelectionAction) -> Unit = {},
    onPlayAll: () -> Unit = {},
    onShuffle: () -> Unit = {},
    bottomPadding: () -> Dp = { 0.dp },
    scaffoldContentPadding: PaddingValues = PaddingValues(0.dp),
) {
    when (uiState) {
        DailyPlaylistUiState.Loading -> TrackCollectionLoading(modifier)

        DailyPlaylistUiState.Empty -> TrackCollectionEmpty(
            message = stringResource(HomeR.string.daily_playlist_empty_message),
            modifier = modifier,
        )

        is DailyPlaylistUiState.Success -> TrackCollectionContent(
            tracks = uiState.tracks,
            currentTrackId = uiState.currentTrackId,
            isCurrentTrackPlaying = uiState.isCurrentTrackPlaying,
            title = stringResource(HomeR.string.daily_playlist_title),
            summary = pluralStringResource(
                HomeR.plurals.daily_playlist_tracks_count,
                uiState.tracks.size,
                uiState.tracks.size,
            ),
            onTrackSelected = onTrackSelected,
            onTrackOptions = onTrackOptions,
            onPlayAll = onPlayAll,
            onShuffle = onShuffle,
            likedTrackIds = likedTrackIds,
            onTrackSelectionAction = onTrackSelectionAction,
            bottomPadding = bottomPadding,
            scaffoldContentPadding = scaffoldContentPadding,
            modifier = modifier,
        )
    }
}
