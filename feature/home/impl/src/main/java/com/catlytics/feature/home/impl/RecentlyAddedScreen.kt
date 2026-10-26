package com.catlytics.feature.home.impl

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
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
internal fun RecentlyAddedRoute(
    modifier: Modifier = Modifier,
    onTrackOptions: (Track) -> Unit,
    likedTrackIds: Set<String> = emptySet(),
    onTrackSelectionAction: (TrackSelectionAction) -> Unit = {},
    bottomPadding: () -> Dp = { 0.dp },
    scaffoldContentPadding: PaddingValues = PaddingValues(0.dp),
    viewModel: RecentlyAddedViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    RecentlyAddedScreen(
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
internal fun RecentlyAddedScreen(
    modifier: Modifier = Modifier,
    uiState: RecentlyAddedUiState,
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
        RecentlyAddedUiState.Loading -> TrackCollectionLoading(modifier)

        is RecentlyAddedUiState.Empty -> TrackCollectionEmpty(
            message = stringResource(HomeR.string.recently_added_empty_message, uiState.windowDays),
            modifier = modifier,
        )

        is RecentlyAddedUiState.Success -> TrackCollectionContent(
            tracks = uiState.tracks,
            currentTrackId = uiState.currentTrackId,
            isCurrentTrackPlaying = uiState.isCurrentTrackPlaying,
            summary = pluralStringResource(
                HomeR.plurals.recently_added_tracks_count,
                uiState.tracks.size,
                uiState.tracks.size,
                uiState.windowDays,
            ),
            badgeLabel = if (uiState.showNewTrackBadge) {
                stringResource(HomeR.string.home_new_track_badge)
            } else {
                null
            },
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
