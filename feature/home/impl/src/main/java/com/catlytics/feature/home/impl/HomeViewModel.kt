package com.catlytics.feature.home.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.catlytics.core.domain.usecase.home.GenerateDailyPlaylistUseCase
import com.catlytics.core.domain.usecase.library.ObserveLibraryUseCase
import com.catlytics.core.domain.usecase.playback.ObservePlaybackStateUseCase
import com.catlytics.core.domain.usecase.playback.PlayShuffledQueueUseCase
import com.catlytics.core.domain.usecase.playback.PlayTrackUseCase
import com.catlytics.core.domain.usecase.playlist.ObservePlaylistContentUseCase
import com.catlytics.core.domain.usecase.statistics.ObserveRecentlyPlayedTracksUseCase
import com.catlytics.core.domain.usecase.statistics.ObserveWeeklyStatsUseCase
import com.catlytics.core.model.LIKED_PLAYLIST_ID
import com.catlytics.core.model.PlaybackStatus
import com.catlytics.core.model.Track
import com.catlytics.core.model.TopTrack
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
internal class HomeViewModel @Inject constructor(
    observeLibraryUseCase: ObserveLibraryUseCase,
    observePlaybackStateUseCase: ObservePlaybackStateUseCase,
    observeRecentlyPlayedTracksUseCase: ObserveRecentlyPlayedTracksUseCase,
    observeWeeklyStatsUseCase: ObserveWeeklyStatsUseCase,
    observePlaylistContentUseCase: ObservePlaylistContentUseCase,
    private val generateDailyPlaylistUseCase: GenerateDailyPlaylistUseCase,
    private val playShuffledQueueUseCase: PlayShuffledQueueUseCase,
    private val playTrackUseCase: PlayTrackUseCase,
) : ViewModel() {
    private val libraryAndListening = combine(
        observeLibraryUseCase().catch { emit(emptyList()) },
        observeRecentlyPlayedTracksUseCase(RECENTLY_PLAYED_LIMIT),
        observeWeeklyStatsUseCase(),
    ) { tracks, recentlyPlayed, weeklyStats ->
        val tracksById = tracks.associateBy(Track::id)
        HomeLibraryAndListening(
            tracks = tracks,
            recentlyPlayedTracks = recentlyPlayed.mapNotNull { recentlyPlayedTrack ->
                tracksById[recentlyPlayedTrack.trackId]
            },
            topTracks = weeklyStats.topTracks
                .filter { topTrack -> topTrack.trackId in tracksById }
                .take(TOP_TRACKS_LIMIT),
        )
    }

    val uiState: StateFlow<HomeUiState> = combine(
        libraryAndListening,
        observePlaybackStateUseCase(),
        observePlaylistContentUseCase(LIKED_PLAYLIST_ID).catch { emit(null) },
    ) { libraryAndListening, playbackState, likedPlaylist ->
        when {
            libraryAndListening.tracks.isEmpty() -> HomeUiState.Empty
            else -> HomeUiState.Success(
                tracks = libraryAndListening.tracks,
                dailyPlaylistTrackCount = generateDailyPlaylistUseCase(
                    libraryAndListening.tracks,
                ).size,
                canShuffleAll = libraryAndListening.tracks.size >= MIN_SHUFFLE_TRACK_COUNT,
                favoriteTracks = likedPlaylist?.tracks.orEmpty(),
                recentlyPlayedTracks = libraryAndListening.recentlyPlayedTracks,
                topTracks = libraryAndListening.topTracks,
                currentTrackId = playbackState.currentTrack?.id,
                isCurrentTrackPlaying = playbackState.status == PlaybackStatus.Playing,
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState.Loading,
    )

    fun onTrackSelected(track: Track, queue: List<Track>) {
        viewModelScope.launch {
            playTrackUseCase(track, queue)
        }
    }

    fun onTopTrackSelected(trackId: String) {
        val state = uiState.value as? HomeUiState.Success ?: return
        val tracksById = state.tracks.associateBy(Track::id)
        val topTracksQueue = state.topTracks.mapNotNull { topTrack ->
            tracksById[topTrack.trackId]
        }
        val selectedTrack = tracksById[trackId] ?: return
        if (topTracksQueue.none { it.id == selectedTrack.id }) return

        viewModelScope.launch {
            playTrackUseCase(selectedTrack, topTracksQueue)
        }
    }

    fun onPlayDailyPlaylist() {
        val tracks = (uiState.value as? HomeUiState.Success)?.tracks.orEmpty()
        val dailyPlaylist = generateDailyPlaylistUseCase(tracks)
        val firstTrack = dailyPlaylist.firstOrNull() ?: return
        viewModelScope.launch {
            playTrackUseCase(firstTrack, dailyPlaylist)
        }
    }

    fun onShuffleAll() {
        val tracks = (uiState.value as? HomeUiState.Success)?.tracks.orEmpty()
        if (tracks.size < MIN_SHUFFLE_TRACK_COUNT) return
        viewModelScope.launch {
            playShuffledQueueUseCase(tracks)
        }
    }

    private data class HomeLibraryAndListening(
        val tracks: List<Track>,
        val recentlyPlayedTracks: List<Track>,
        val topTracks: List<TopTrack>,
    )

    private companion object {
        const val RECENTLY_PLAYED_LIMIT = 10
        const val TOP_TRACKS_LIMIT = 3
        const val MIN_SHUFFLE_TRACK_COUNT = 2
    }

}
