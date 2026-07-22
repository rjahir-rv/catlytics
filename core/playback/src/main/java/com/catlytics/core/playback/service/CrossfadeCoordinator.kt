package com.catlytics.core.playback.service

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ShuffleOrder
import com.catlytics.core.domain.repository.PlaybackPreferencesRepository
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

internal class CrossfadeCoordinator(
    primary: ExoPlayer,
    secondary: ExoPlayer,
    preferencesRepository: PlaybackPreferencesRepository,
    scope: CoroutineScope,
    private val onActivePlayerChanged: (active: ExoPlayer, standby: ExoPlayer) -> Unit,
) : PlayerControl {
    private var active = primary
    private var standby = secondary
    private var configuredDurationMillis = 0L
    private var masterVolume = 1f
    private var monitorJob: Job? = null
    private var preferencesJob: Job? = null
    private var preparedIndex = C.INDEX_UNSET
    private var transition: Transition? = null

    private val listener = object : Player.Listener {
        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            if (transition != null) standby.playWhenReady = playWhenReady
        }

        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int,
        ) {
            if (reason == Player.DISCONTINUITY_REASON_SEEK ||
                reason == Player.DISCONTINUITY_REASON_SEEK_ADJUSTMENT
            ) {
                cancelTransition(clearPreload = true)
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            if (reason != Player.MEDIA_ITEM_TRANSITION_REASON_AUTO &&
                reason != Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT
            ) {
                cancelTransition(clearPreload = true)
            }
        }

        override fun onTimelineChanged(timeline: androidx.media3.common.Timeline, reason: Int) {
            if (reason == Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED &&
                (transition != null || preparedIndex != C.INDEX_UNSET)
            ) {
                cancelTransition(clearPreload = true)
            }
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            if (transition != null || preparedIndex != C.INDEX_UNSET) {
                cancelTransition(clearPreload = true)
            }
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            if (transition != null || preparedIndex != C.INDEX_UNSET) {
                cancelTransition(clearPreload = true)
            }
        }

        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
            cancelTransition(clearPreload = true)
        }
    }

    init {
        active.addListener(listener)
        preferencesJob = scope.launch {
            preferencesRepository.observeCrossfadeDurationSeconds()
                .distinctUntilChanged()
                .collect { seconds ->
                    configuredDurationMillis = seconds.coerceIn(
                        PlaybackPreferencesRepository.MIN_CROSSFADE_DURATION_SECONDS,
                        PlaybackPreferencesRepository.MAX_CROSSFADE_DURATION_SECONDS,
                    ) * 1_000L
                    if (configuredDurationMillis == 0L && transition == null) {
                        clearStandby()
                    }
                }
        }
        monitorJob = scope.launch {
            while (isActive) {
                update()
                delay(UPDATE_INTERVAL_MILLIS.milliseconds)
            }
        }
    }

    override val playWhenReady: Boolean
        get() = active.playWhenReady

    override var volume: Float
        get() = masterVolume
        set(value) {
            masterVolume = value.coerceIn(0f, 1f)
            applyVolumes()
        }

    override fun play() {
        active.play()
        if (transition != null) standby.play()
    }

    override fun pause() {
        active.pause()
        standby.pause()
    }

    fun release() {
        monitorJob?.cancel()
        monitorJob = null
        preferencesJob?.cancel()
        preferencesJob = null
        active.removeListener(listener)
        clearStandby()
    }

    private fun update() {
        val durationMillis = active.duration.takeIf { it != C.TIME_UNSET && it > 0L } ?: return
        val currentIndex = active.currentMediaItemIndex
        if (currentIndex == C.INDEX_UNSET) return

        val activeTransition = transition
        if (activeTransition != null) {
            updateTransition(activeTransition)
            return
        }
        if (configuredDurationMillis <= 0L || !active.isPlaying ||
            active.repeatMode == Player.REPEAT_MODE_ONE
        ) {
            return
        }

        val nextIndex = nextMediaItemIndex() ?: return
        val remainingMillis = (durationMillis - active.currentPosition).coerceAtLeast(0L)
        val nextDurationMillis = active.getMediaItemAt(nextIndex).mediaMetadata.durationMs
            ?.takeIf { it > 0L }
        val effectiveDurationMillis = minOf(
            configuredDurationMillis,
            durationMillis,
            nextDurationMillis ?: configuredDurationMillis,
        )
        if (effectiveDurationMillis <= 0L) return

        if (remainingMillis <= effectiveDurationMillis + PRELOAD_LEAD_MILLIS) {
            prepareSecondary(nextIndex)
        }
        if (remainingMillis <= effectiveDurationMillis &&
            preparedIndex == nextIndex && standby.playbackState == Player.STATE_READY
        ) {
            val elapsedMillis = (effectiveDurationMillis - remainingMillis).coerceAtLeast(0L)
            standby.seekTo(nextIndex, elapsedMillis)
            standby.playWhenReady = active.playWhenReady
            transition = Transition(
                durationMillis = effectiveDurationMillis,
            )
            applyVolumes()
        }
    }

    private fun updateTransition(activeTransition: Transition) {
        if (standby.playerError != null) {
            cancelTransition(clearPreload = true)
            return
        }
        if (!active.playWhenReady) standby.pause()

        val progress = (standby.currentPosition.toFloat() / activeTransition.durationMillis)
            .coerceIn(0f, 1f)
        applyVolumes(progress)
        if (progress < 1f) return

        val previousActive = active
        previousActive.removeListener(listener)
        active = standby
        standby = previousActive
        active.volume = masterVolume
        active.addListener(listener)
        transition = null
        preparedIndex = C.INDEX_UNSET
        onActivePlayerChanged(active, standby)
        clearStandby()
    }

    private fun prepareSecondary(index: Int) {
        if (preparedIndex == index) return
        clearStandby()
        standby.volume = 0f
        standby.setMediaItems((0 until active.mediaItemCount).map(active::getMediaItemAt))
        copyPlaybackOrder(active, standby)
        standby.repeatMode = active.repeatMode
        standby.shuffleModeEnabled = active.shuffleModeEnabled
        standby.seekTo(index, 0L)
        standby.prepare()
        preparedIndex = index
    }

    private fun nextMediaItemIndex(): Int? {
        val timeline = active.currentTimeline
        if (timeline.isEmpty) return null
        return timeline.getNextWindowIndex(
            active.currentMediaItemIndex,
            active.repeatMode,
            active.shuffleModeEnabled,
        ).takeUnless { it == C.INDEX_UNSET }
    }

    private fun cancelTransition(clearPreload: Boolean) {
        transition = null
        active.volume = masterVolume
        if (clearPreload) clearStandby()
    }

    private fun clearStandby() {
        standby.stop()
        standby.clearMediaItems()
        standby.volume = 0f
        preparedIndex = C.INDEX_UNSET
    }

    private fun applyVolumes() {
        val activeTransition = transition
        if (activeTransition == null) {
            active.volume = masterVolume
            standby.volume = 0f
            return
        }
        val progress = (standby.currentPosition.toFloat() / activeTransition.durationMillis)
            .coerceIn(0f, 1f)
        applyVolumes(progress)
    }

    private fun applyVolumes(progress: Float) {
        val volumes = equalPowerCrossfadeVolumes(progress, masterVolume)
        active.volume = volumes.primary
        standby.volume = volumes.secondary
    }

    private data class Transition(
        val durationMillis: Long,
    )

    private companion object {
        const val UPDATE_INTERVAL_MILLIS = 40L
        const val PRELOAD_LEAD_MILLIS = 2_000L
    }
}

@OptIn(UnstableApi::class)
private fun copyPlaybackOrder(source: ExoPlayer, target: ExoPlayer) {
    if (!source.shuffleModeEnabled || source.mediaItemCount == 0) return
    val timeline = source.currentTimeline
    val shuffledIndices = buildList {
        var index = timeline.getFirstWindowIndex(true)
        while (index != C.INDEX_UNSET && size < source.mediaItemCount) {
            add(index)
            index = timeline.getNextWindowIndex(index, Player.REPEAT_MODE_OFF, true)
        }
    }
    if (shuffledIndices.size == source.mediaItemCount) {
        target.shuffleOrder =
            ShuffleOrder.DefaultShuffleOrder(shuffledIndices.toIntArray(), SHUFFLE_ORDER_SEED)
    }
}

private const val SHUFFLE_ORDER_SEED = 0L

internal data class CrossfadeVolumes(
    val primary: Float,
    val secondary: Float,
)

internal fun equalPowerCrossfadeVolumes(progress: Float, masterVolume: Float): CrossfadeVolumes {
    val normalizedProgress = progress.coerceIn(0f, 1f)
    val normalizedMasterVolume = masterVolume.coerceIn(0f, 1f)
    val angle = normalizedProgress * PI.toFloat() / 2f
    return CrossfadeVolumes(
        primary = cos(angle) * normalizedMasterVolume,
        secondary = sin(angle) * normalizedMasterVolume,
    )
}
