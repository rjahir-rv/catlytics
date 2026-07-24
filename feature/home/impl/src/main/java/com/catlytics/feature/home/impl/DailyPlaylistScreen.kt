package com.catlytics.feature.home.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.catlytics.core.domain.usecase.home.GenerateDailyPlaylistUseCase
import com.catlytics.core.domain.usecase.library.ObserveLibraryUseCase
import com.catlytics.core.domain.usecase.playback.ObservePlaybackStateUseCase
import com.catlytics.core.domain.usecase.playback.PlayTrackUseCase
import com.catlytics.core.model.PlaybackStatus
import com.catlytics.core.model.Track
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal sealed interface DailyPlaylistUiState {
    data object Loading : DailyPlaylistUiState
    data object Empty : DailyPlaylistUiState
    data class Success(
        val tracks: List<Track>,
        val currentTrackId: String? = null,
        val isCurrentTrackPlaying: Boolean = false,
    ) : DailyPlaylistUiState
}

@HiltViewModel
internal class DailyPlaylistViewModel @Inject constructor(
    observeLibraryUseCase: ObserveLibraryUseCase,
    observePlaybackStateUseCase: ObservePlaybackStateUseCase,
    generateDailyPlaylistUseCase: GenerateDailyPlaylistUseCase,
    private val playTrackUseCase: PlayTrackUseCase,
) : ViewModel() {
    val uiState: StateFlow<DailyPlaylistUiState> = combine(
        observeLibraryUseCase().catch { emit(emptyList()) },
        observePlaybackStateUseCase(),
    ) { library, playbackState ->
        val dailyTracks = generateDailyPlaylistUseCase(library)
        if (dailyTracks.isEmpty()) {
            DailyPlaylistUiState.Empty
        } else {
            DailyPlaylistUiState.Success(
                tracks = dailyTracks,
                currentTrackId = playbackState.currentTrack?.id,
                isCurrentTrackPlaying = playbackState.status == PlaybackStatus.Playing,
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DailyPlaylistUiState.Loading,
    )

    fun onTrackSelected(track: Track, queue: List<Track>) {
        viewModelScope.launch {
            playTrackUseCase(track, queue)
        }
    }
}

@Composable
internal fun DailyPlaylistRoute(
    onTrackOptions: (Track) -> Unit,
    modifier: Modifier = Modifier,
    bottomPadding: () -> Dp = { 0.dp },
    scaffoldContentPadding: PaddingValues = PaddingValues(0.dp),
    viewModel: DailyPlaylistViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    DailyPlaylistScreen(
        uiState = uiState,
        onTrackSelected = viewModel::onTrackSelected,
        onTrackOptions = onTrackOptions,
        bottomPadding = bottomPadding,
        scaffoldContentPadding = scaffoldContentPadding,
        modifier = modifier,
    )
}

@Composable
internal fun DailyPlaylistScreen(
    uiState: DailyPlaylistUiState,
    onTrackSelected: (Track, List<Track>) -> Unit,
    onTrackOptions: (Track) -> Unit,
    modifier: Modifier = Modifier,
    bottomPadding: () -> Dp = { 0.dp },
    scaffoldContentPadding: PaddingValues = PaddingValues(0.dp),
) {
    when (uiState) {
        DailyPlaylistUiState.Loading -> Box(
            modifier = modifier
                .fillMaxSize()
                .padding(top = scaffoldContentPadding.calculateTopPadding()),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }

        DailyPlaylistUiState.Empty -> Box(
            modifier = modifier
                .fillMaxSize()
                .padding(
                    start = 20.dp,
                    top = scaffoldContentPadding.calculateTopPadding() + 20.dp,
                    end = 20.dp,
                    bottom = 20.dp,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Necesitas al menos 5 canciones para crear tu Playlist diaria.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        is DailyPlaylistUiState.Success -> LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = bottomPadding() + 20.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            item(key = "daily-playlist-header") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = 20.dp,
                            top = scaffoldContentPadding.calculateTopPadding() + 20.dp,
                            end = 20.dp,
                            bottom = 12.dp,
                        ),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                    text = "Tu selección para hoy",
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Text(
                        text = "${uiState.tracks.size} canciones elegidas para ti",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(items = uiState.tracks, key = Track::id) { track ->
                TrackRow(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    track = track,
                    isCurrent = track.id == uiState.currentTrackId,
                    isPlaying = track.id == uiState.currentTrackId &&
                        uiState.isCurrentTrackPlaying,
                    onTrackSelected = { onTrackSelected(track, uiState.tracks) },
                    onTrackOptions = { onTrackOptions(track) },
                )
            }
        }
    }
}
