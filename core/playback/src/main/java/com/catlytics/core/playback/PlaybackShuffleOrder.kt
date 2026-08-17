package com.catlytics.core.playback

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ShuffleOrder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaybackShuffleOrder @Inject constructor() {
    @Volatile
    private var player: ExoPlayer? = null

    @OptIn(UnstableApi::class)
    fun attach(player: ExoPlayer) {
        this.player = player
    }

    fun detach(player: ExoPlayer) {
        if (this.player === player) {
            this.player = null
        }
    }

    fun currentPlayer(): ExoPlayer? = player

    @OptIn(UnstableApi::class)
    fun setShuffledIndices(indices: List<Int>): Boolean {
        val exoPlayer = player ?: return false
        if (indices.size != exoPlayer.mediaItemCount) return false
        if (indices.toSet() != (0 until exoPlayer.mediaItemCount).toSet()) return false

        exoPlayer.shuffleOrder = ShuffleOrder.DefaultShuffleOrder(
            indices.toIntArray(),
            SHUFFLE_ORDER_SEED,
        )
        return true
    }

    private companion object {
        const val SHUFFLE_ORDER_SEED = 0L
    }
}
