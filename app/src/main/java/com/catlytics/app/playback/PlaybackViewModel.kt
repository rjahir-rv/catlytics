package com.catlytics.app.playback

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.catlytics.app.R
import com.catlytics.core.designsystem.text.UiText
import com.catlytics.core.domain.repository.SleepTimerController
import com.catlytics.core.domain.usecase.playback.AddQueueItemUseCase
import com.catlytics.core.domain.usecase.playback.CycleRepeatModeUseCase
import com.catlytics.core.domain.usecase.playback.MoveQueueItemUseCase
import com.catlytics.core.domain.usecase.playback.ObservePlaybackStateUseCase
import com.catlytics.core.domain.usecase.playback.PlayNextUseCase
import com.catlytics.core.domain.usecase.playback.PlayQueueItemUseCase
import com.catlytics.core.domain.usecase.playback.RemoveQueueItemUseCase
import com.catlytics.core.domain.usecase.playback.RestorePlaybackSessionUseCase
import com.catlytics.core.domain.usecase.playback.SeekPlaybackUseCase
import com.catlytics.core.domain.usecase.playback.SkipPlaybackUseCase
import com.catlytics.core.domain.usecase.playback.TogglePlaybackUseCase
import com.catlytics.core.domain.usecase.playback.ToggleShuffleUseCase
import com.catlytics.core.domain.usecase.playlist.AddTracksToLikedUseCase
import com.catlytics.core.domain.usecase.playlist.ObserveIsTrackLikedUseCase
import com.catlytics.core.domain.usecase.playlist.ObservePlaylistsUseCase
import com.catlytics.core.domain.usecase.playlist.RemoveTracksFromLikedUseCase
import com.catlytics.core.domain.usecase.playlist.ToggleLikedTrackResult
import com.catlytics.core.domain.usecase.playlist.ToggleLikedTrackUseCase
import com.catlytics.core.model.LIKED_PLAYLIST_ID
import com.catlytics.core.model.PlaybackQueueSource
import com.catlytics.core.model.PlaybackState
import com.catlytics.core.model.SleepTimerState
import com.catlytics.core.model.Track
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PlaybackViewModel @Inject constructor(
    observePlaybackStateUseCase: ObservePlaybackStateUseCase,
    observeIsTrackLikedUseCase: ObserveIsTrackLikedUseCase,
    observePlaylistsUseCase: ObservePlaylistsUseCase,
    private val togglePlaybackUseCase: TogglePlaybackUseCase,
    private val skipPlaybackUseCase: SkipPlaybackUseCase,
    private val seekPlaybackUseCase: SeekPlaybackUseCase,
    private val toggleShuffleUseCase: ToggleShuffleUseCase,
    private val cycleRepeatModeUseCase: CycleRepeatModeUseCase,
    private val playQueueItemUseCase: PlayQueueItemUseCase,
    private val addQueueItemUseCase: AddQueueItemUseCase,
    private val playNextUseCase: PlayNextUseCase,
    private val moveQueueItemUseCase: MoveQueueItemUseCase,
    private val removeQueueItemUseCase: RemoveQueueItemUseCase,
    private val restorePlaybackSessionUseCase: RestorePlaybackSessionUseCase,
    private val toggleLikedTrackUseCase: ToggleLikedTrackUseCase,
    private val addTracksToLikedUseCase: AddTracksToLikedUseCase,
    private val removeTracksFromLikedUseCase: RemoveTracksFromLikedUseCase,
    private val sleepTimerController: SleepTimerController,
) : ViewModel() {
    private val _messages = MutableSharedFlow<UiText>(extraBufferCapacity = 4)

    val messages: SharedFlow<UiText> = _messages.asSharedFlow()

    private val fullPlaybackState: StateFlow<PlaybackState> = observePlaybackStateUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = PlaybackState(),
        )

    val playbackState: StateFlow<PlaybackState> = fullPlaybackState
        .map { it.copy(positionMillis = 0L, bufferedPositionMillis = 0L) }
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = PlaybackState(),
        )

    val progress: StateFlow<PlaybackProgress> = fullPlaybackState
        .map(PlaybackState::toProgress)
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = PlaybackProgress(),
        )

    val isCurrentTrackLiked: StateFlow<Boolean> = playbackState
        .map { it.currentTrack?.id }
        .distinctUntilChanged()
        .flatMapLatest(observeIsTrackLikedUseCase::invoke)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = false,
        )

    private val playlists = observePlaylistsUseCase()
        .shareIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            replay = 1,
        )

    val queueSourcePlaylist: StateFlow<Pair<String, String>?> = combine(
        playbackState.map { it.queueSource }.distinctUntilChanged(),
        playlists,
    ) { source, playlists ->
        (source as? PlaybackQueueSource.Playlist)?.let { playlistSource ->
            playlists
                .firstOrNull { it.id == playlistSource.playlistId }
                ?.let { it.id to it.name }
        }
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )

    val likedTrackIds: StateFlow<Set<String>> = playlists
        .map { playlists ->
            playlists
                .firstOrNull { it.id == LIKED_PLAYLIST_ID }
                ?.trackIds
                .orEmpty()
                .toSet()
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptySet(),
        )

    val sleepTimerState: StateFlow<SleepTimerState> = sleepTimerController.state

    init {
        viewModelScope.launch {
            restorePlaybackSessionUseCase()
        }
    }

    fun startSleepTimer(durationMinutes: Int) {
        sleepTimerController.start(durationMinutes)
    }

    fun cancelSleepTimer() {
        sleepTimerController.cancel()
    }

    fun togglePlayback() {
        viewModelScope.launch {
            togglePlaybackUseCase()
        }
    }

    fun skipNext() {
        viewModelScope.launch {
            skipPlaybackUseCase.next()
        }
    }

    fun skipPrevious() {
        viewModelScope.launch {
            skipPlaybackUseCase.previous()
        }
    }

    fun seekTo(positionMillis: Long) {
        viewModelScope.launch {
            seekPlaybackUseCase(positionMillis)
        }
    }

    fun seekBackward10Seconds() {
        seekBy(-SEEK_INTERVAL_MILLIS)
    }

    fun seekForward10Seconds() {
        seekBy(SEEK_INTERVAL_MILLIS)
    }

    fun toggleShuffle() {
        viewModelScope.launch {
            toggleShuffleUseCase(!playbackState.value.isShuffleEnabled)
        }
    }

    fun cycleRepeatMode() {
        viewModelScope.launch {
            cycleRepeatModeUseCase(playbackState.value.repeatMode)
        }
    }

    fun playQueueItem(index: Int) {
        viewModelScope.launch {
            playQueueItemUseCase(index)
        }
    }

    fun addQueueItem(track: Track) {
        enqueueTrack(track, UiText.Resource(R.string.app_toast_added_to_queue)) {
            addQueueItemUseCase(it)
        }
    }

    fun playNext(track: Track) {
        enqueueTrack(track, UiText.Resource(R.string.app_toast_will_play_next)) {
            playNextUseCase(it)
        }
    }

    fun addQueueItems(tracks: List<Track>) {
        viewModelScope.launch {
            val added = addQueueItemUseCase.invokeAll(tracks, playbackState.value.currentTrack?.id)
            _messages.emit(UiText.Plural(R.plurals.app_toast_tracks_added_to_queue, added))
        }
    }

    fun playNextTracks(tracks: List<Track>) {
        viewModelScope.launch {
            val queued = playNextUseCase.invokeAll(tracks, playbackState.value.currentTrack?.id)
            _messages.emit(UiText.Plural(R.plurals.app_toast_tracks_will_play_next, queued))
        }
    }

    fun likeTracks(trackIds: Collection<String>) {
        viewModelScope.launch {
            val added = addTracksToLikedUseCase(trackIds)
            _messages.emit(
                UiText.Plural(
                    id = R.plurals.app_toast_tracks_added_to_liked,
                    count = added,
                    args = listOf(added, LikedPlaylistName),
                ),
            )
        }
    }

    fun unlikeTracks(trackIds: Collection<String>) {
        viewModelScope.launch {
            val removed = removeTracksFromLikedUseCase(trackIds)
            _messages.emit(
                UiText.Plural(
                    id = R.plurals.app_toast_tracks_removed_from_liked,
                    count = removed,
                    args = listOf(removed, LikedPlaylistName),
                ),
            )
        }
    }

    private fun enqueueTrack(
        track: Track,
        message: UiText,
        action: suspend (Track) -> Unit,
    ) {
        val currentTrackId = playbackState.value.currentTrack?.id ?: return
        if (currentTrackId == track.id) return
        viewModelScope.launch {
            action(track)
            _messages.emit(message)
        }
    }

    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        viewModelScope.launch {
            moveQueueItemUseCase(fromIndex, toIndex)
        }
    }

    fun removeQueueItem(index: Int) {
        viewModelScope.launch {
            removeQueueItemUseCase(index)
        }
    }

    fun toggleCurrentTrackLiked() {
        val trackId = playbackState.value.currentTrack?.id ?: return
        toggleTrackLiked(trackId)
    }

    fun toggleTrackLiked(trackId: String) {
        viewModelScope.launch {
            val messageRes = when (toggleLikedTrackUseCase(trackId)) {
                ToggleLikedTrackResult.Added -> R.string.app_toast_track_added_to_liked
                ToggleLikedTrackResult.Removed -> R.string.app_toast_track_removed_from_liked
            }
            _messages.emit(UiText.Resource(messageRes, listOf(LikedPlaylistName)))
        }
    }

    private fun seekBy(offsetMillis: Long) {
        val state = fullPlaybackState.value
        if (state.currentTrack == null) return

        viewModelScope.launch {
            seekPlaybackUseCase(
                calculateRelativeSeekPosition(
                    positionMillis = state.positionMillis,
                    durationMillis = state.durationMillis,
                    offsetMillis = offsetMillis,
                ),
            )
        }
    }

    private companion object {
        const val SEEK_INTERVAL_MILLIS = 10_000L
        val LikedPlaylistName = UiText.Resource(R.string.app_playlist_liked_name)
    }
}

internal fun calculateRelativeSeekPosition(
    positionMillis: Long,
    durationMillis: Long,
    offsetMillis: Long,
): Long {
    val targetPosition = (positionMillis + offsetMillis).coerceAtLeast(0L)
    return if (durationMillis > 0L) {
        targetPosition.coerceAtMost(durationMillis)
    } else {
        targetPosition
    }
}
