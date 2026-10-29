package com.catlytics.core.playback.service

import android.media.AudioManager
import android.media.AudioManager.OnAudioFocusChangeListener
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackAudioFocusHandlerTest {
    private val audioFocusGateway = FakeAudioFocusGateway()
    private val playerControl = FakePlayerControl()
    private val handler = PlaybackAudioFocusHandler(
        audioFocusGateway = audioFocusGateway,
        playerControl = playerControl,
    )

    @Test
    fun `requestPlaybackFocus requests audio focus`() {
        val granted = handler.requestPlaybackFocus()

        assertTrue(granted)
        assertEquals(1, audioFocusGateway.requestCount)
    }

    @Test
    fun `requestPlaybackFocus returns false when audio focus is denied`() {
        audioFocusGateway.requestGranted = false

        val granted = handler.requestPlaybackFocus()

        assertFalse(granted)
        assertEquals(1, audioFocusGateway.requestCount)
    }

    @Test
    fun `transient focus loss pauses and resumes on focus gain`() {
        handler.requestPlaybackFocus()
        playerControl.playWhenReady = true

        audioFocusGateway.dispatchFocusChange(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT)
        handler.onPlaybackPaused()

        assertFalse(playerControl.playWhenReady)
        assertEquals(1, playerControl.pauseCount)
        assertEquals(0, audioFocusGateway.abandonCount)

        audioFocusGateway.dispatchFocusChange(AudioManager.AUDIOFOCUS_GAIN)

        assertTrue(playerControl.playWhenReady)
        assertEquals(1, playerControl.playCount)
    }

    @Test
    fun `duckable transient focus loss lowers volume without pausing`() {
        handler.requestPlaybackFocus()
        playerControl.playWhenReady = true

        audioFocusGateway.dispatchFocusChange(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK)

        assertTrue(playerControl.playWhenReady)
        assertEquals(0, playerControl.pauseCount)
        assertEquals(0.2f, playerControl.volume)

        audioFocusGateway.dispatchFocusChange(AudioManager.AUDIOFOCUS_GAIN)

        assertTrue(playerControl.playWhenReady)
        assertEquals(0, playerControl.playCount)
        assertEquals(1f, playerControl.volume)
    }

    @Test
    fun `manual pause while ducked restores volume and abandons focus`() {
        handler.requestPlaybackFocus()
        playerControl.playWhenReady = true
        audioFocusGateway.dispatchFocusChange(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK)

        playerControl.playWhenReady = false
        handler.onPlaybackPaused()

        assertEquals(1f, playerControl.volume)
        assertEquals(1, audioFocusGateway.abandonCount)
    }

    @Test
    fun `focus loss pauses and resumes on focus gain when playback was interrupted`() {
        handler.requestPlaybackFocus()
        playerControl.playWhenReady = true

        audioFocusGateway.dispatchFocusChange(AudioManager.AUDIOFOCUS_LOSS)
        handler.onPlaybackPaused()

        assertFalse(playerControl.playWhenReady)
        assertEquals(1, playerControl.pauseCount)
        assertEquals(0, audioFocusGateway.abandonCount)

        audioFocusGateway.dispatchFocusChange(AudioManager.AUDIOFOCUS_GAIN)

        assertTrue(playerControl.playWhenReady)
        assertEquals(1, playerControl.playCount)
    }

    @Test
    fun `manual pause abandons focus and disables automatic resume`() {
        handler.requestPlaybackFocus()
        playerControl.playWhenReady = false

        handler.onPlaybackPaused()
        audioFocusGateway.dispatchFocusChange(AudioManager.AUDIOFOCUS_GAIN)

        assertEquals(1, audioFocusGateway.abandonCount)
        assertEquals(0, playerControl.playCount)
    }

    @Test
    fun `playback ended abandons focus`() {
        handler.requestPlaybackFocus()

        handler.onPlaybackEnded()

        assertEquals(1, audioFocusGateway.abandonCount)
    }

    @Test
    fun `release abandons active request after focus loss`() {
        handler.requestPlaybackFocus()
        playerControl.playWhenReady = true
        audioFocusGateway.dispatchFocusChange(AudioManager.AUDIOFOCUS_LOSS)
        handler.onPlaybackPaused()

        handler.release()

        assertEquals(1, audioFocusGateway.abandonCount)
    }
}

private class FakeAudioFocusGateway : AudioFocusGateway {
    var requestGranted = true
    var requestCount = 0
    var abandonCount = 0
    private var listener: OnAudioFocusChangeListener? = null

    override fun requestAudioFocus(listener: OnAudioFocusChangeListener): Boolean {
        requestCount++
        this.listener = listener
        return requestGranted
    }

    override fun abandonAudioFocus() {
        abandonCount++
    }

    fun dispatchFocusChange(focusChange: Int) {
        listener?.onAudioFocusChange(focusChange)
    }
}

private class FakePlayerControl : PlayerControl {
    override var playWhenReady = false
    override var volume = 1f
    var playCount = 0
    var pauseCount = 0

    override fun play() {
        playCount++
        playWhenReady = true
    }

    override fun pause() {
        pauseCount++
        playWhenReady = false
    }
}
