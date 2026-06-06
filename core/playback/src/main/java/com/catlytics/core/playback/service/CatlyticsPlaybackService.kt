package com.catlytics.core.playback.service

import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

class CatlyticsPlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    private var player: ExoPlayer? = null
    private var audioFocusHandler: PlaybackAudioFocusHandler? = null

    override fun onCreate() {
        super.onCreate()
        val exoPlayer = ExoPlayer.Builder(this)
            .setAudioAttributes(playbackAudioAttributes(), false)
            .build()
        player = exoPlayer

        val focusHandler = PlaybackAudioFocusHandler(
            audioFocusGateway = AndroidAudioFocusGateway(this),
            playerControl = ExoPlayerControl(exoPlayer),
        )
        audioFocusHandler = focusHandler
        exoPlayer.addListener(
            object : Player.Listener {
                override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                    if (playWhenReady) {
                        ensurePlaybackFocus()
                    } else {
                        focusHandler.onPlaybackPaused()
                    }
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_ENDED) {
                        focusHandler.onPlaybackEnded()
                    } else if (exoPlayer.playWhenReady) {
                        ensurePlaybackFocus()
                    }
                }

                private fun ensurePlaybackFocus() {
                    if (!focusHandler.requestPlaybackFocus()) {
                        exoPlayer.pause()
                    }
                }
            },
        )

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("catlytics://nowplaying")).apply {
            setPackage(packageName)
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        mediaSession = MediaSession.Builder(this, exoPlayer)
            .setSessionActivity(pendingIntent)
            .build()
    }

    override fun onGetSession(
        controllerInfo: MediaSession.ControllerInfo,
    ): MediaSession? = mediaSession

    @OptIn(UnstableApi::class)
    override fun onDestroy() {
        audioFocusHandler?.release()
        mediaSession?.run {
            player.release()
            release()
        }
        audioFocusHandler = null
        mediaSession = null
        player = null
        super.onDestroy()
    }

    private fun playbackAudioAttributes(): AudioAttributes = AudioAttributes.Builder()
        .setUsage(C.USAGE_MEDIA)
        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
        .build()
}

private class ExoPlayerControl(
    private val player: ExoPlayer,
) : PlayerControl {
    override val playWhenReady: Boolean
        get() = player.playWhenReady

    override var volume: Float
        get() = player.volume
        set(value) {
            player.volume = value
        }

    override fun play() {
        player.play()
    }

    override fun pause() {
        player.pause()
    }
}
