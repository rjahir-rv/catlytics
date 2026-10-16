package com.catlytics.core.widget

import com.catlytics.core.model.PlaybackState
import com.catlytics.core.model.PlaybackStatus

data class NowPlayingWidgetState(
    val hasTrack: Boolean = false,
    val title: String = "",
    val artist: String = "",
    val artworkUri: String? = null,
    val isPlaying: Boolean = false,
)

internal fun PlaybackState.toWidgetState(): NowPlayingWidgetState? {
    val track = currentTrack ?: return null
    return NowPlayingWidgetState(
        hasTrack = true,
        title = track.title,
        artist = track.artist.name,
        artworkUri = track.artworkUri,
        isPlaying = status == PlaybackStatus.Playing || status == PlaybackStatus.Buffering,
    )
}

internal fun resolveWidgetState(
    live: NowPlayingWidgetState?,
    hasSavedSession: Boolean,
    cached: NowPlayingWidgetState,
): NowPlayingWidgetState = when {
    live != null -> live
    hasSavedSession && cached.hasTrack -> cached.copy(isPlaying = false)
    else -> NowPlayingWidgetState()
}
