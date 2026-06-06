package com.catlytics.core.playback.service

import android.content.Context
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.AudioManager.OnAudioFocusChangeListener
import androidx.core.content.getSystemService

internal class PlaybackAudioFocusHandler(
    private val audioFocusGateway: AudioFocusGateway,
    private val playerControl: PlayerControl,
) : OnAudioFocusChangeListener {
    private var hasAudioFocus = false
    private var hasActiveAudioFocusRequest = false
    private var resumeOnFocusGain = false
    private var ignoreNextPlaybackPause = false
    private var isDucked = false

    fun requestPlaybackFocus(): Boolean {
        if (hasAudioFocus) return true

        hasAudioFocus = audioFocusGateway.requestAudioFocus(this)
        hasActiveAudioFocusRequest = hasAudioFocus
        return hasAudioFocus
    }

    fun onPlaybackPaused() {
        if (ignoreNextPlaybackPause) {
            ignoreNextPlaybackPause = false
            return
        }

        resumeOnFocusGain = false
        abandonAudioFocus()
    }

    fun onPlaybackEnded() {
        resumeOnFocusGain = false
        abandonAudioFocus()
    }

    fun release() {
        resumeOnFocusGain = false
        abandonAudioFocus()
    }

    override fun onAudioFocusChange(focusChange: Int) {
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                hasAudioFocus = true
                restoreVolume()
                if (resumeOnFocusGain) {
                    resumeOnFocusGain = false
                    playerControl.play()
                }
            }

            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                hasAudioFocus = false
                restoreVolume()
                resumeOnFocusGain = playerControl.playWhenReady
                pauseForFocusChange()
            }

            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                duckVolume()
            }

            AudioManager.AUDIOFOCUS_LOSS -> {
                hasAudioFocus = false
                restoreVolume()
                resumeOnFocusGain = playerControl.playWhenReady
                pauseForFocusChange()
            }
        }
    }

    private fun pauseForFocusChange() {
        if (!playerControl.playWhenReady) return

        ignoreNextPlaybackPause = true
        playerControl.pause()
    }

    private fun duckVolume() {
        if (isDucked) return

        playerControl.volume = DUCKED_VOLUME
        isDucked = true
    }

    private fun restoreVolume() {
        if (!isDucked) return

        playerControl.volume = NORMAL_VOLUME
        isDucked = false
    }

    private fun abandonAudioFocus() {
        if (!hasActiveAudioFocusRequest) return

        restoreVolume()
        audioFocusGateway.abandonAudioFocus()
        hasAudioFocus = false
        hasActiveAudioFocusRequest = false
    }

    private companion object {
        const val NORMAL_VOLUME = 1f
        const val DUCKED_VOLUME = 0.2f
    }
}

internal interface PlayerControl {
    val playWhenReady: Boolean
    var volume: Float

    fun play()

    fun pause()
}

internal interface AudioFocusGateway {
    fun requestAudioFocus(listener: OnAudioFocusChangeListener): Boolean

    fun abandonAudioFocus()
}

internal class AndroidAudioFocusGateway(
    context: Context,
) : AudioFocusGateway {
    private val audioManager = requireNotNull(context.getSystemService<AudioManager>()) {
        "AudioManager is required for playback audio focus"
    }
    private var audioFocusRequest: AudioFocusRequest? = null

    override fun requestAudioFocus(listener: OnAudioFocusChangeListener): Boolean {
        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(
                android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build(),
            )
            .setOnAudioFocusChangeListener(listener)
            .setWillPauseWhenDucked(false)
            .build()

        audioFocusRequest = request
        return audioManager.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    override fun abandonAudioFocus() {
        audioFocusRequest?.let(audioManager::abandonAudioFocusRequest)
        audioFocusRequest = null
    }
}
