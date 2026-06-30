package com.catlytics.core.playback

import android.os.SystemClock
import com.catlytics.core.domain.repository.PlaybackEventRepository
import com.catlytics.core.model.PlaybackEvent
import com.catlytics.core.model.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaybackTracker @Inject constructor(
    private val playbackEventRepository: PlaybackEventRepository
) {
    internal var trackerScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private var currentTrack: Track? = null
    private var playStartTimestamp: Long = 0L
    private var accumulatedMillis: Long = 0L
    private var isPlaying: Boolean = false

    companion object {
        const val MIN_LISTEN_THRESHOLD_MILLIS = 30_000L // 30 seconds
    }

    /**
     * Called when the playback play/pause state changes.
     */
    @Synchronized
    fun onPlayingChanged(playing: Boolean, track: Track?) {
        if (playing && track != null) {
            startTracking(track)
        } else {
            pauseTracking()
        }
    }

    /**
     * Called when transitioning to a new media item (track).
     */
    @Synchronized
    fun onTrackTransition(newTrack: Track?) {
        val wasPlaying = isPlaying
        flushCurrentTrack()
        if (wasPlaying && newTrack != null) {
            startTracking(newTrack)
        }
    }

    /**
     * Called when the player is released or session ends.
     */
    @Synchronized
    fun onSessionEnd() {
        flushCurrentTrack()
    }

    private fun startTracking(track: Track) {
        if (track.id != currentTrack?.id) {
            flushCurrentTrack()
        }
        currentTrack = track
        playStartTimestamp = SystemClock.elapsedRealtime()
        isPlaying = true
    }

    private fun pauseTracking() {
        if (isPlaying && currentTrack != null) {
            accumulatedMillis += SystemClock.elapsedRealtime() - playStartTimestamp
        }
        isPlaying = false
    }

    private fun flushCurrentTrack() {
        pauseTracking()
        val track = currentTrack ?: return
        val listened = accumulatedMillis

        if (listened >= MIN_LISTEN_THRESHOLD_MILLIS) {
            val eventTime = System.currentTimeMillis()
            trackerScope.launch {
                playbackEventRepository.recordEvent(
                    PlaybackEvent(
                        trackId = track.id,
                        trackTitle = track.title,
                        artistId = track.artist.id,
                        artistName = track.artist.name,
                        artworkUri = track.artworkUri,
                        durationListenedMillis = listened,
                        trackDurationMillis = track.durationMillis,
                        timestamp = eventTime
                    )
                )
            }
        }

        // Reset tracking state
        currentTrack = null
        accumulatedMillis = 0L
    }
}
