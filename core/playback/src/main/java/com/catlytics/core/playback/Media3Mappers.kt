package com.catlytics.core.playback

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import com.catlytics.core.model.PlaybackRepeatMode
import com.catlytics.core.model.PlaybackQueueSource
import com.catlytics.core.model.PlaybackState
import com.catlytics.core.model.PlaybackStatus
import com.catlytics.core.model.Track

fun Track.toMediaItem(): MediaItem = MediaItem.Builder()
    .setMediaId(id)
    .setUri(mediaUri)
    .setMediaMetadata(
        MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist.name)
            .setDurationMs(durationMillis)
            .setArtworkUri(artworkUri?.let(Uri::parse))
            .build(),
    )
    .build()

internal fun Player.toPlaybackState(
    queue: List<Track>,
    queueSource: PlaybackQueueSource,
): PlaybackState {
    val currentIndex = currentMediaItemIndex.takeUnless { it < 0 } ?: 0
    val currentTrack = queue.getOrNull(currentIndex)
    return PlaybackState(
        status = toPlaybackStatus(),
        currentTrack = currentTrack,
        queue = queue,
        queueSource = queueSource,
        currentIndex = currentIndex,
        positionMillis = currentPosition.coerceAtLeast(0L),
        durationMillis = duration.takeIf { it > 0L } ?: currentTrack?.durationMillis ?: 0L,
        bufferedPositionMillis = bufferedPosition.coerceAtLeast(0L),
        isShuffleEnabled = shuffleModeEnabled,
        repeatMode = repeatMode.toPlaybackRepeatMode(),
    )
}

private fun Player.toPlaybackStatus(): PlaybackStatus = when {
    playbackState == Player.STATE_BUFFERING -> PlaybackStatus.Buffering
    playbackState == Player.STATE_ENDED -> PlaybackStatus.Ended
    playbackState == Player.STATE_IDLE -> PlaybackStatus.Idle
    isPlaying -> PlaybackStatus.Playing
    else -> PlaybackStatus.Paused
}

internal fun Int.toPlaybackRepeatMode(): PlaybackRepeatMode = when (this) {
    Player.REPEAT_MODE_ONE -> PlaybackRepeatMode.One
    Player.REPEAT_MODE_ALL -> PlaybackRepeatMode.All
    else -> PlaybackRepeatMode.Off
}

internal fun PlaybackRepeatMode.toMedia3RepeatMode(): Int = when (this) {
    PlaybackRepeatMode.Off -> Player.REPEAT_MODE_OFF
    PlaybackRepeatMode.One -> Player.REPEAT_MODE_ONE
    PlaybackRepeatMode.All -> Player.REPEAT_MODE_ALL
}
