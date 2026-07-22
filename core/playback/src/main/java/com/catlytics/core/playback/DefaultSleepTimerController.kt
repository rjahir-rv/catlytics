package com.catlytics.core.playback

import android.os.SystemClock
import com.catlytics.core.domain.repository.PlaybackController
import com.catlytics.core.domain.repository.SleepTimerController
import com.catlytics.core.model.SleepTimerState
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@Singleton
class DefaultSleepTimerController @Inject constructor(
    playbackController: PlaybackController,
) : SleepTimerController {
    private val engine = SleepTimerEngine(
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
        elapsedRealtimeMillis = SystemClock::elapsedRealtime,
        onExpired = playbackController::pause,
    )

    override val state: StateFlow<SleepTimerState> = engine.state

    override fun start(durationMinutes: Int) {
        engine.start(durationMinutes)
    }

    override fun cancel() {
        engine.cancel()
    }
}

internal class SleepTimerEngine(
    private val scope: CoroutineScope,
    private val elapsedRealtimeMillis: () -> Long,
    private val onExpired: suspend () -> Unit,
) {
    private val _state = MutableStateFlow<SleepTimerState>(SleepTimerState.Inactive)
    val state: StateFlow<SleepTimerState> = _state.asStateFlow()

    private var timerJob: Job? = null
    private var generation = 0L

    fun start(durationMinutes: Int) {
        require(durationMinutes > 0) { "Sleep timer duration must be positive." }

        generation += 1
        val currentGeneration = generation
        timerJob?.cancel()

        val totalDurationMillis = durationMinutes * MILLIS_PER_MINUTE
        val deadlineMillis = elapsedRealtimeMillis() + totalDurationMillis
        _state.value = SleepTimerState.Active(
            totalDurationMillis = totalDurationMillis,
            remainingMillis = totalDurationMillis,
        )
        timerJob = scope.launch {
            try {
                var remainingMillis = totalDurationMillis
                while (remainingMillis > 0L) {
                    delay(minOf(COUNTDOWN_UPDATE_INTERVAL_MILLIS, remainingMillis).milliseconds)
                    remainingMillis = (deadlineMillis - elapsedRealtimeMillis()).coerceAtLeast(0L)
                    _state.value = SleepTimerState.Active(
                        totalDurationMillis = totalDurationMillis,
                        remainingMillis = remainingMillis,
                    )
                }
                onExpired()
            } finally {
                if (generation == currentGeneration) {
                    timerJob = null
                    _state.value = SleepTimerState.Inactive
                }
            }
        }
    }

    fun cancel() {
        generation += 1
        timerJob?.cancel()
        timerJob = null
        _state.value = SleepTimerState.Inactive
    }

    private companion object {
        const val MILLIS_PER_MINUTE = 60_000L
        const val COUNTDOWN_UPDATE_INTERVAL_MILLIS = 1_000L
    }
}
