package com.catlytics.core.designsystem

import com.catlytics.core.designsystem.format.SleepTimerFormat
import org.junit.Assert.assertEquals
import org.junit.Test

class SleepTimerFormatTest {
    @Test
    fun `remaining time formats minutes and hours`() {
        assertEquals("00:00", SleepTimerFormat.formatRemaining(0L))
        assertEquals("05:01", SleepTimerFormat.formatRemaining(300_001L))
        assertEquals("1:02:03", SleepTimerFormat.formatRemaining(3_723_000L))
    }
}
