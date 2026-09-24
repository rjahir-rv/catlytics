package com.catlytics.feature.home.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.catlytics.core.designsystem.R
import com.catlytics.core.domain.repository.HomePreferencesRepository
import com.catlytics.core.domain.usecase.library.ObserveRecentlyAddedTracksUseCase
import com.catlytics.core.domain.usecase.playback.ObservePlaybackStateUseCase
import com.catlytics.core.domain.usecase.playback.PlayShuffledQueueUseCase
import com.catlytics.core.domain.usecase.playback.PlayTrackUseCase
import com.catlytics.core.model.PlaybackStatus
import com.catlytics.core.designsystem.component.TrackSelectionHost
import com.catlytics.core.designsystem.component.rememberTrackSelectionState
import com.catlytics.core.model.RecentAddedWindow
import com.catlytics.core.model.Track
import com.catlytics.core.model.TrackSelectionAction
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal sealed interface RecentlyAddedUiState {
    data object Loading : RecentlyAddedUiState
    data class Empty(val windowDays: Int) : RecentlyAddedUiState
    data class Success(
        val tracks: List<Track>,
        val currentTrackId: String? = null,
        val isCurrentTrackPlaying: Boolean = false,
        val windowDays: Int = RecentAddedWindow.Days21.days,
        val showNewTrackBadge: Boolean = true,
    ) : RecentlyAddedUiState
}

@HiltViewModel
internal class RecentlyAddedViewModel @Inject constructor(
    observeRecentlyAddedTracksUseCase: ObserveRecentlyAddedTracksUseCase,
    observePlaybackStateUseCase: ObservePlaybackStateUseCase,
    homePreferencesRepository: HomePreferencesRepository,
    private val playTrackUseCase: PlayTrackUseCase,
    private val playShuffledQueueUseCase: PlayShuffledQueueUseCase,
) : ViewModel() {
    val uiState: StateFlow<RecentlyAddedUiState> = combine(
        observeRecentlyAddedTracksUseCase().catch { emit(emptyList()) },
        observePlaybackStateUseCase(),
        homePreferencesRepository.observeHomeRecommendationsSettings(),
    ) { recentlyAddedTracks, playbackState, homeRecommendationsSettings ->
        if (recentlyAddedTracks.isEmpty()) {
            RecentlyAddedUiState.Empty(homeRecommendationsSettings.recentAddedWindow.days)
        } else {
            RecentlyAddedUiState.Success(
                tracks = recentlyAddedTracks,
                currentTrackId = playbackState.currentTrack?.id,
                isCurrentTrackPlaying = playbackState.status == PlaybackStatus.Playing,
                windowDays = homeRecommendationsSettings.recentAddedWindow.days,
                showNewTrackBadge = homeRecommendationsSettings.showNewTrackBadge,
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = RecentlyAddedUiState.Loading,
    )

    fun onTrackSelected(track: Track, queue: List<Track>) {
        viewModelScope.launch {
            playTrackUseCase(track, queue)
        }
    }

    fun onPlayAll() {
        val tracks = (uiState.value as? RecentlyAddedUiState.Success)?.tracks.orEmpty()
        val firstTrack = tracks.firstOrNull() ?: return
        viewModelScope.launch {
            playTrackUseCase(firstTrack, tracks)
        }
    }

    fun onShuffle() {
        val tracks = (uiState.value as? RecentlyAddedUiState.Success)?.tracks.orEmpty()
        if (tracks.size < MIN_SHUFFLE_TRACK_COUNT) return
        viewModelScope.launch {
            playShuffledQueueUseCase(tracks)
        }
    }
}

internal const val MIN_SHUFFLE_TRACK_COUNT = 2

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
        RecentlyAddedUiState.Loading -> Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }

        is RecentlyAddedUiState.Empty -> Box(
            modifier = modifier
                .fillMaxSize()
                .padding(20.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "No hay canciones agregadas en los últimos ${uiState.windowDays} días.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        is RecentlyAddedUiState.Success -> {
            val selectionState = rememberTrackSelectionState()
            val selection = selectionState.value
            TrackSelectionHost(
                selection = selection,
                onSelectionChange = { selectionState.value = it },
                visibleIds = uiState.tracks.map(Track::id),
                selectedTracks = selection.selectedTracks(uiState.tracks),
                likedTrackIds = likedTrackIds,
                currentTrackId = uiState.currentTrackId,
                topInset = scaffoldContentPadding.calculateTopPadding(),
                bottomInset = bottomPadding(),
                onAction = onTrackSelectionAction,
                modifier = modifier.fillMaxSize(),
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        top = scaffoldContentPadding.calculateTopPadding() +
                            if (selection.active) 64.dp else 0.dp,
                        bottom = bottomPadding() + 20.dp +
                            if (selection.active) 72.dp else 0.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    item(key = "recently-added-header") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = if (uiState.tracks.size == 1) {
                                    "1 canción agregada en los últimos ${uiState.windowDays} días"
                                } else {
                                    "${uiState.tracks.size} canciones agregadas en los últimos " +
                                        "${uiState.windowDays} días"
                                },
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            FilledTonalIconButton(
                                onClick = onShuffle,
                                enabled = uiState.tracks.size >= MIN_SHUFFLE_TRACK_COUNT,
                                modifier = Modifier.size(48.dp),
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_shuffle),
                                    contentDescription = "Reproducir aleatoriamente",
                                    modifier = Modifier.size(24.dp),
                                )
                            }
                            FilledIconButton(
                                onClick = onPlayAll,
                                modifier = Modifier.size(48.dp),
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_play),
                                    contentDescription = "Reproducir canciones",
                                    modifier = Modifier.size(24.dp),
                                )
                            }
                        }
                    }
                    items(items = uiState.tracks, key = Track::id) { track ->
                        TrackRow(
                            track = track,
                            isCurrent = track.id == uiState.currentTrackId,
                            isPlaying = track.id == uiState.currentTrackId &&
                                uiState.isCurrentTrackPlaying,
                            isNew = uiState.showNewTrackBadge,
                            onTrackSelected = {
                                if (selection.active) {
                                    selectionState.value = selection.onTrackLongClick(track.id)
                                } else {
                                    onTrackSelected(track, uiState.tracks)
                                }
                            },
                            onTrackOptions = { onTrackOptions(track) },
                            modifier = Modifier.padding(horizontal = 20.dp),
                            selected = track.id in selection.selectedIds,
                            selectionActive = selection.active,
                            onLongClick = {
                                selectionState.value = selection.onTrackLongClick(track.id)
                            },
                        )
                    }
                }
            }
        }
    }
}
