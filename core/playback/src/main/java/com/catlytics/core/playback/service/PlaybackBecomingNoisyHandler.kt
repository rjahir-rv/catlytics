package com.catlytics.core.playback.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import androidx.core.content.ContextCompat

internal class PlaybackBecomingNoisyHandler(
    private val noisyGateway: AudioBecomingNoisyGateway,
    private val playerControl: PlayerControl,
) {
    private var registered = false

    fun start() {
        if (registered) return

        noisyGateway.register(::onBecomingNoisy)
        registered = true
    }

    fun stop() {
        if (!registered) return

        noisyGateway.unregister()
        registered = false
    }

    internal fun onBecomingNoisy() {
        if (!playerControl.playWhenReady) return

        playerControl.pause()
    }
}

internal fun interface AudioBecomingNoisyListener {
    fun onAudioBecomingNoisy()
}

internal interface AudioBecomingNoisyGateway {
    fun register(listener: AudioBecomingNoisyListener)

    fun unregister()
}

internal class AndroidAudioBecomingNoisyGateway(
    private val context: Context,
) : AudioBecomingNoisyGateway {
    private var listener: AudioBecomingNoisyListener? = null
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != AudioManager.ACTION_AUDIO_BECOMING_NOISY) return

            listener?.onAudioBecomingNoisy()
        }
    }

    override fun register(listener: AudioBecomingNoisyListener) {
        this.listener = listener
        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
    }

    override fun unregister() {
        context.unregisterReceiver(receiver)
        listener = null
    }
}
