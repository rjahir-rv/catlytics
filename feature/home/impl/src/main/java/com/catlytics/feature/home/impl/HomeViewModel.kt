package com.catlytics.feature.home.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.catlytics.core.domain.repository.HomePreferencesRepository
import com.catlytics.core.domain.usecase.home.GenerateDailyPlaylistUseCase
import com.catlytics.core.domain.usecase.library.ObserveLibraryUseCase
import com.catlytics.core.domain.usecase.library.ObserveRecentlyAddedTracksUseCase
import com.catlytics.core.domain.usecase.playback.ObserveNowPlayingUseCase
import com.catlytics.core.domain.usecase.playback.PlayShuffledQueueUseCase
import com.catlytics.core.domain.usecase.playback.PlayTrackUseCase
import com.catlytics.core.domain.usecase.playlist.ObservePlaylistContentUseCase
import com.catlytics.core.domain.usecase.statistics.ObserveRecentlyPlayedTracksUseCase
import com.catlytics.core.domain.usecase.statistics.ObserveWeeklyStatsUseCase
import com.catlytics.core.model.LIKED_PLAYLIST_ID
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
    observeNowPlayingUseCase: ObserveNowPlayingUseCase,
    observeRecentlyPlayedTracksUseCase: ObserveRecentlyPlayedTracksUseCase,
    observeWeeklyStatsUseCase: ObserveWeeklyStatsUseCase,
    observePlaylistContentUseCase: ObservePlaylistContentUseCase,
    observeRecentlyAddedTracksUseCase: ObserveRecentlyAddedTracksUseCase,
    homePreferencesRepository: HomePreferencesRepository,
    private val generateDailyPlaylistUseCase: GenerateDailyPlaylistUseCase,
    private val playShuffledQueueUseCase: PlayShuffledQueueUseCase,
    private val playTrackUseCase: PlayTrackUseCase,
) : ViewModel() {
    private val libraryAndListening = combine(
        observeLibraryUseCase().catch { emit(emptyList()) },
        observeRecentlyPlayedTracksUseCase(RECENTLY_PLAYED_LIMIT),
        observeWeeklyStatsUseCase(),
        observeRecentlyAddedTracksUseCase().catch { emit(emptyList()) },
    ) { tracks, recentlyPlayed, weeklyStats, recentlyAdded ->
        val tracksById = tracks.associateBy(Track::id)
        HomeLibraryAndListening(
            tracks = tracks,
            dailyPlaylistTrackCount = generateDailyPlaylistUseCase(tracks).size,
            recentlyPlayedTracks = recentlyPlayed.mapNotNull { recentlyPlayedTrack ->
                tracksById[recentlyPlayedTrack.trackId]
            },
            recentlyAddedTracks = recentlyAdded.filter { recentlyAddedTrack ->
                recentlyAddedTrack.id in tracksById
            },
            topTracks = weeklyStats.topTracks
                .filter { topTrack -> topTrack.trackId in tracksById }
                .take(TOP_TRACKS_LIMIT),
        )
    }

    val uiState: StateFlow<HomeUiState> = combine(
        libraryAndListening,
        observeNowPlayingUseCase(),
        observePlaylistContentUseCase(LIKED_PLAYLIST_ID).catch { emit(null) },
        homePreferencesRepository.observeHomeRecommendationsSettings(),
    ) { libraryAndListening, nowPlaying, likedPlaylist, homeRecommendationsSettings ->
        when {
            libraryAndListening.tracks.isEmpty() -> HomeUiState.Empty
            else -> HomeUiState.Success(
                tracks = libraryAndListening.tracks,
                dailyPlaylistTrackCount = libraryAndListening.dailyPlaylistTrackCount,
                canShuffleAll = libraryAndListening.tracks.size >= MIN_SHUFFLE_TRACK_COUNT,
                favoriteTracks = likedPlaylist?.tracks.orEmpty(),
                recentlyPlayedTracks = libraryAndListening.recentlyPlayedTracks,
                recentlyAddedTracks = libraryAndListening.recentlyAddedTracks,
                topTracks = libraryAndListening.topTracks,
                currentTrackId = nowPlaying.trackId,
                isCurrentTrackPlaying = nowPlaying.isPlaying,
                showRecommendedPlaylists = homeRecommendationsSettings.showRecommendedPlaylists,
                showNewTrackBadge = homeRecommendationsSettings.showNewTrackBadge,
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

    fun onShuffleAll() {
        val tracks = (uiState.value as? HomeUiState.Success)?.tracks.orEmpty()
        if (tracks.size < MIN_SHUFFLE_TRACK_COUNT) return
        viewModelScope.launch {
            playShuffledQueueUseCase(tracks)
        }
    }

    private data class HomeLibraryAndListening(
        val tracks: List<Track>,
        val dailyPlaylistTrackCount: Int,
        val recentlyPlayedTracks: List<Track>,
        val recentlyAddedTracks: List<Track>,
        val topTracks: List<TopTrack>,
    )

    private companion object {
        const val RECENTLY_PLAYED_LIMIT = 10
        const val TOP_TRACKS_LIMIT = 3
        const val MIN_SHUFFLE_TRACK_COUNT = 2
    }

}
