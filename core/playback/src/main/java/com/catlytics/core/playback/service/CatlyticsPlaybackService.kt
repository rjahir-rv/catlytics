package com.catlytics.core.playback.service

import android.app.PendingIntent
import android.content.Intent
import android.media.AudioManager
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.catlytics.core.domain.repository.PlaybackPreferencesRepository
import com.catlytics.core.playback.AndroidEqualizerRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import androidx.core.net.toUri

@OptIn(UnstableApi::class)
@AndroidEntryPoint
class CatlyticsPlaybackService : MediaSessionService() {
    @Inject
    lateinit var equalizerRepository: AndroidEqualizerRepository

    @Inject
    lateinit var playbackPreferencesRepository: PlaybackPreferencesRepository

    private var mediaSession: MediaSession? = null
    private var player: ExoPlayer? = null
    private var crossfadePlayer: ExoPlayer? = null
    private var crossfadeCoordinator: CrossfadeCoordinator? = null
    private var audioFocusHandler: PlaybackAudioFocusHandler? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val playerListener = object : Player.Listener {
        override fun onAudioSessionIdChanged(audioSessionId: Int) {
            equalizerRepository.attachAudioSessionId(audioSessionId)
        }

        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            val focusHandler = audioFocusHandler ?: return
            if (playWhenReady) {
                ensurePlaybackFocus(focusHandler)
            } else {
                focusHandler.onPlaybackPaused()
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            val focusHandler = audioFocusHandler ?: return
            if (playbackState == Player.STATE_ENDED) {
                focusHandler.onPlaybackEnded()
            } else if (player?.playWhenReady == true) {
                ensurePlaybackFocus(focusHandler)
            }
        }
    }


    override fun onCreate() {
        super.onCreate()
        val exoPlayer = ExoPlayer.Builder(this)
            .setAudioAttributes(playbackAudioAttributes(), false)
            .build()
        val secondaryPlayer = ExoPlayer.Builder(this)
            .setAudioAttributes(playbackAudioAttributes(), false)
            .build()
        val sharedAudioSessionId = getSystemService(AudioManager::class.java).generateAudioSessionId()
        if (sharedAudioSessionId != AudioManager.ERROR) {
            exoPlayer.audioSessionId = sharedAudioSessionId
            secondaryPlayer.audioSessionId = sharedAudioSessionId
        }
        player = exoPlayer
        crossfadePlayer = secondaryPlayer

        val coordinator = CrossfadeCoordinator(
            primary = exoPlayer,
            secondary = secondaryPlayer,
            preferencesRepository = playbackPreferencesRepository,
            scope = serviceScope,
            onActivePlayerChanged = ::onActivePlayerChanged,
        )
        crossfadeCoordinator = coordinator

        val focusHandler = PlaybackAudioFocusHandler(
            audioFocusGateway = AndroidAudioFocusGateway(this),
            playerControl = coordinator,
        )
        audioFocusHandler = focusHandler
        exoPlayer.addListener(playerListener)
        equalizerRepository.attachAudioSessionId(exoPlayer.audioSessionId)

        val intent = Intent(Intent.ACTION_VIEW, "catlytics://nowplaying".toUri()).apply {
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
        player?.removeListener(playerListener)
        equalizerRepository.release()
        crossfadeCoordinator?.release()
        audioFocusHandler?.release()
        mediaSession?.run {
            player.release()
            release()
        }
        audioFocusHandler = null
        crossfadeCoordinator = null
        crossfadePlayer?.release()
        crossfadePlayer = null
        mediaSession = null
        player = null
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun playbackAudioAttributes(): AudioAttributes = AudioAttributes.Builder()
        .setUsage(C.USAGE_MEDIA)
        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
        .build()

    private fun onActivePlayerChanged(active: ExoPlayer, standby: ExoPlayer) {
        player?.removeListener(playerListener)
        player = active
        crossfadePlayer = standby
        active.addListener(playerListener)
        mediaSession?.setPlayer(active)
    }

    private fun ensurePlaybackFocus(focusHandler: PlaybackAudioFocusHandler) {
        if (!focusHandler.requestPlaybackFocus()) {
            player?.pause()
        }
    }
}
