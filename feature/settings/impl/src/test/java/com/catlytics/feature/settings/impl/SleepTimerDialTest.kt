package com.catlytics.feature.settings.impl

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import org.junit.Assert.assertEquals
import org.junit.Test

class SleepTimerDialTest {
    @Test
    fun `snap rounds to five minute steps and clamps range`() {
        assertEquals(5, snapSleepTimerMinutes(0))
        assertEquals(5, snapSleepTimerMinutes(7))
        assertEquals(10, snapSleepTimerMinutes(8))
        assertEquals(120, snapSleepTimerMinutes(123))
    }

    @Test
    fun `dial maps cardinal positions clockwise`() {
        val size = Size(200f, 200f)

        assertEquals(5, minutesForDialPosition(Offset(100f, 0f), size))
        assertEquals(30, minutesForDialPosition(Offset(200f, 100f), size))
        assertEquals(60, minutesForDialPosition(Offset(100f, 200f), size))
        assertEquals(90, minutesForDialPosition(Offset(0f, 100f), size))
    }

    @Test
    fun `remaining time formats minutes and hours`() {
        assertEquals("00:00", formatSleepTimerRemaining(0L))
        assertEquals("05:01", formatSleepTimerRemaining(300_001L))
        assertEquals("1:02:03", formatSleepTimerRemaining(3_723_000L))
    }
}
