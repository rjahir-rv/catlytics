package com.catlytics.core.playback

import androidx.media3.exoplayer.ExoPlayer
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaybackShuffleOrder @Inject constructor() {
    @Volatile
    private var player: ExoPlayer? = null

    fun attach(player: ExoPlayer) {
        this.player = player
    }

    fun detach(player: ExoPlayer) {
        if (this.player === player) {
            this.player = null
        }
    }

    fun currentPlayer(): ExoPlayer? = player
}
