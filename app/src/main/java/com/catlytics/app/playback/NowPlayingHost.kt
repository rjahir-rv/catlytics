package com.catlytics.app.playback

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.catlytics.app.R
import com.catlytics.core.model.LIKED_PLAYLIST_ID
import com.catlytics.core.model.PlaybackState
import com.catlytics.core.model.Track

@Composable
internal fun NowPlayingHost(
    isOpen: Boolean,
    playbackViewModel: PlaybackViewModel,
    playbackState: PlaybackState,
    progress: () -> PlaybackProgress,
    onClose: () -> Unit,
    canAddTrackToQueue: (Track) -> Boolean,
    onTrackOptions: (Track) -> Unit,
    onAddToPlaylist: (Track) -> Unit,
    onGoToAlbum: (Track) -> Unit,
    onGoToArtist: (Track) -> Unit,
    onEditMetadata: (Track) -> Unit,
) {
    val context = LocalContext.current
    val isCurrentTrackLiked by playbackViewModel.isCurrentTrackLiked.collectAsStateWithLifecycle()
    val queueSourcePlaylist by playbackViewModel.queueSourcePlaylist.collectAsStateWithLifecycle()
    val sleepTimerState by playbackViewModel.sleepTimerState.collectAsStateWithLifecycle()
    val currentTrack = playbackState.currentTrack

    NowPlayingOverlay(
        isOpen = isOpen,
        onClose = onClose,
    ) {
        NowPlayingScreen(
            playbackState = playbackState,
            isOpen = isOpen,
            progress = progress,
            onBack = onClose,
            onTogglePlayback = playbackViewModel::togglePlayback,
            onSkipPrevious = playbackViewModel::skipPrevious,
            onSkipNext = playbackViewModel::skipNext,
            onSeekTo = playbackViewModel::seekTo,
            onSeekBackward10Seconds = playbackViewModel::seekBackward10Seconds,
            onSeekForward10Seconds = playbackViewModel::seekForward10Seconds,
            onToggleShuffle = playbackViewModel::toggleShuffle,
            onCycleRepeatMode = playbackViewModel::cycleRepeatMode,
            onShareTrack = context::shareTrack,
            onPlayQueueItem = playbackViewModel::playQueueItem,
            onMoveQueueItem = playbackViewModel::moveQueueItem,
            onRemoveQueueItem = playbackViewModel::removeQueueItem,
            onTrackOptions = onTrackOptions,
            canAddCurrentTrackToQueue = currentTrack?.let(canAddTrackToQueue) ?: false,
            onAddCurrentTrackToPlaylist = { currentTrack?.let(onAddToPlaylist) },
            onToggleCurrentTrackLikedFromOptions = {
                currentTrack?.let { track -> playbackViewModel.toggleTrackLiked(track.id) }
            },
            onPlayNextCurrentTrack = { currentTrack?.let(playbackViewModel::playNext) },
            onAddCurrentTrackToQueue = { currentTrack?.let(playbackViewModel::addQueueItem) },
            onGoToCurrentTrackAlbum = { currentTrack?.let(onGoToAlbum) },
            onGoToCurrentTrackArtist = { currentTrack?.let(onGoToArtist) },
            onEditCurrentTrackMetadata = { currentTrack?.let(onEditMetadata) },
            isCurrentTrackLiked = isCurrentTrackLiked,
            queueSourceTitle = queueSourcePlaylist?.let { (id, name) ->
                if (id == LIKED_PLAYLIST_ID) {
                    stringResource(R.string.app_playlist_liked_name)
                } else {
                    name
                }
            } ?: stringResource(R.string.app_now_playing_from_library),
            sleepTimerState = sleepTimerState,
            onStartSleepTimer = playbackViewModel::startSleepTimer,
            onCancelSleepTimer = playbackViewModel::cancelSleepTimer,
            onAddCurrentTrackToLiked = playbackViewModel::toggleCurrentTrackLiked,
        )
    }
}
