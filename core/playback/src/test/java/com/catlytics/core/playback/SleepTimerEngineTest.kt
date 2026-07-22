package com.catlytics.core.playback

import com.catlytics.core.model.SleepTimerState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SleepTimerEngineTest {
    @Test
    fun `timer pauses playback and becomes inactive when duration expires`() = runTest {
        var pauseCalls = 0
        val engine = SleepTimerEngine(
            scope = backgroundScope,
            elapsedRealtimeMillis = { testScheduler.currentTime },
            onExpired = { pauseCalls += 1 },
        )

        engine.start(durationMinutes = 5)
        assertEquals(
            SleepTimerState.Active(
                totalDurationMillis = 300_000L,
                remainingMillis = 300_000L,
            ),
            engine.state.value,
        )

        advanceTimeBy(299_999L)
        runCurrent()
        assertEquals(0, pauseCalls)
        assertTrue(engine.state.value is SleepTimerState.Active)

        advanceTimeBy(1L)
        runCurrent()
        assertEquals(1, pauseCalls)
        assertEquals(SleepTimerState.Inactive, engine.state.value)
    }

    @Test
    fun `cancel prevents playback pause`() = runTest {
        var pauseCalls = 0
        val engine = SleepTimerEngine(
            scope = backgroundScope,
            elapsedRealtimeMillis = { testScheduler.currentTime },
            onExpired = { pauseCalls += 1 },
        )

        engine.start(durationMinutes = 5)
        advanceTimeBy(60_000L)
        engine.cancel()
        advanceTimeBy(300_000L)
        runCurrent()

        assertEquals(0, pauseCalls)
        assertEquals(SleepTimerState.Inactive, engine.state.value)
    }

    @Test
    fun `starting again replaces previous timer`() = runTest {
        var pauseCalls = 0
        val engine = SleepTimerEngine(
            scope = backgroundScope,
            elapsedRealtimeMillis = { testScheduler.currentTime },
            onExpired = { pauseCalls += 1 },
        )

        engine.start(durationMinutes = 5)
        advanceTimeBy(60_000L)
        engine.start(durationMinutes = 10)
        advanceTimeBy(240_000L)
        runCurrent()

        assertEquals(0, pauseCalls)
        assertEquals(600_000L, (engine.state.value as SleepTimerState.Active).totalDurationMillis)

        advanceTimeBy(360_000L)
        runCurrent()
        assertEquals(1, pauseCalls)
        assertEquals(SleepTimerState.Inactive, engine.state.value)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `duration must be positive`() = runTest {
        SleepTimerEngine(
            scope = backgroundScope,
            elapsedRealtimeMillis = { testScheduler.currentTime },
            onExpired = {},
        ).start(durationMinutes = 0)
    }
}
