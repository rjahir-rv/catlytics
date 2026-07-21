package com.catlytics.core.playback

import android.net.Uri
import androidx.media3.common.C
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
    val playbackQueue = queue.inPlaybackOrder(shuffledMediaItemIndices())
    val currentTrack = currentMediaItem?.mediaId?.let { mediaId ->
        playbackQueue.firstOrNull { it.id == mediaId }
    }
    val currentIndex = playbackQueue.indexOf(currentTrack).takeUnless { it < 0 } ?: 0
    return PlaybackState(
        status = toPlaybackStatus(),
        currentTrack = currentTrack,
        queue = playbackQueue,
        queueSource = queueSource,
        currentIndex = currentIndex,
        positionMillis = currentPosition.coerceAtLeast(0L),
        durationMillis = duration.takeIf { it > 0L } ?: currentTrack?.durationMillis ?: 0L,
        bufferedPositionMillis = bufferedPosition.coerceAtLeast(0L),
        isShuffleEnabled = shuffleModeEnabled,
        repeatMode = repeatMode.toPlaybackRepeatMode(),
    )
}

private fun Player.shuffledMediaItemIndices(): List<Int>? {
    if (!shuffleModeEnabled || mediaItemCount == 0) return null

    val currentIndex = currentMediaItemIndex
    if (currentIndex !in 0 until mediaItemCount) return null

    val visitedIndices = mutableSetOf(currentIndex)
    val previousIndices = mutableListOf<Int>()
    var index = currentIndex
    while (true) {
        index = currentTimeline.getPreviousWindowIndex(index, effectiveRepeatMode(), true)
        if (index == C.INDEX_UNSET || !visitedIndices.add(index)) break
        previousIndices += index
    }

    val nextIndices = mutableListOf<Int>()
    index = currentIndex
    while (true) {
        index = currentTimeline.getNextWindowIndex(index, effectiveRepeatMode(), true)
        if (index == C.INDEX_UNSET || !visitedIndices.add(index)) break
        nextIndices += index
    }

    return previousIndices.asReversed() + currentIndex + nextIndices
}

private fun Player.effectiveRepeatMode(): Int = when (repeatMode) {
    Player.REPEAT_MODE_ONE -> Player.REPEAT_MODE_OFF
    else -> repeatMode
}

internal fun List<Track>.inPlaybackOrder(shuffledIndices: List<Int>?): List<Track> =
    shuffledIndices?.mapNotNull(::getOrNull)?.takeIf { it.size == size } ?: this

private fun Player.toPlaybackStatus(): PlaybackStatus = when {
    playbackState == Player.STATE_BUFFERING -> PlaybackStatus.Buffering
    playbackState == Player.STATE_ENDED -> PlaybackStatus.Ended
    playbackState == Player.STATE_IDLE -> PlaybackStatus.Idle
    playWhenReady -> PlaybackStatus.Playing
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
