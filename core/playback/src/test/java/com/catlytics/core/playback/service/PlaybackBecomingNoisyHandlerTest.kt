package com.catlytics.core.playback.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class PlaybackBecomingNoisyHandlerTest {
    private val noisyGateway = FakeAudioBecomingNoisyGateway()
    private val playerControl = FakeNoisyPlayerControl()
    private val handler = PlaybackBecomingNoisyHandler(
        noisyGateway = noisyGateway,
        playerControl = playerControl,
    )

    @Test
    fun `becoming noisy pauses when playback is ready`() {
        handler.start()
        playerControl.playWhenReady = true

        noisyGateway.dispatch()

        assertFalse(playerControl.playWhenReady)
        assertEquals(1, playerControl.pauseCount)
    }

    @Test
    fun `becoming noisy does not pause when already paused`() {
        handler.start()
        playerControl.playWhenReady = false

        noisyGateway.dispatch()

        assertEquals(0, playerControl.pauseCount)
    }

    @Test
    fun `start registers the gateway once`() {
        handler.start()
        handler.start()

        assertEquals(1, noisyGateway.registerCount)
    }

    @Test
    fun `stop unregisters the gateway and ignores later events`() {
        handler.start()
        playerControl.playWhenReady = true
        handler.stop()

        noisyGateway.dispatch()

        assertEquals(1, noisyGateway.unregisterCount)
        assertEquals(0, playerControl.pauseCount)
    }

    @Test
    fun `stop is ignored before start`() {
        handler.stop()

        assertEquals(0, noisyGateway.unregisterCount)
    }

    @Test
    fun `becoming noisy after a previous pause does not pause again`() {
        handler.start()
        playerControl.playWhenReady = true
        noisyGateway.dispatch()
        noisyGateway.dispatch()

        assertEquals(1, playerControl.pauseCount)
    }
}

private class FakeAudioBecomingNoisyGateway : AudioBecomingNoisyGateway {
    var registerCount = 0
    var unregisterCount = 0
    private var listener: AudioBecomingNoisyListener? = null

    override fun register(listener: AudioBecomingNoisyListener) {
        registerCount++
        this.listener = listener
    }

    override fun unregister() {
        unregisterCount++
        listener = null
    }

    fun dispatch() {
        listener?.onAudioBecomingNoisy()
    }
}

private class FakeNoisyPlayerControl : PlayerControl {
    override var playWhenReady = false
    override var volume = 1f
    var pauseCount = 0

    override fun play() {
        playWhenReady = true
    }

    override fun pause() {
        pauseCount++
        playWhenReady = false
    }
}
