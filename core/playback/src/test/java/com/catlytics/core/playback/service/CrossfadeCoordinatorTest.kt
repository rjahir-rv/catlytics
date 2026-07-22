package com.catlytics.core.playback.service

import org.junit.Assert.assertEquals
import org.junit.Test

class CrossfadeCoordinatorTest {
    @Test
    fun `equal power curve starts with only primary deck`() {
        val volumes = equalPowerCrossfadeVolumes(progress = 0f, masterVolume = 1f)

        assertEquals(1f, volumes.primary, 0.001f)
        assertEquals(0f, volumes.secondary, 0.001f)
    }

    @Test
    fun `equal power curve balances both decks halfway`() {
        val volumes = equalPowerCrossfadeVolumes(progress = 0.5f, masterVolume = 1f)

        assertEquals(0.707f, volumes.primary, 0.001f)
        assertEquals(0.707f, volumes.secondary, 0.001f)
    }

    @Test
    fun `equal power curve ends with only secondary deck and respects master volume`() {
        val volumes = equalPowerCrossfadeVolumes(progress = 1f, masterVolume = 0.2f)

        assertEquals(0f, volumes.primary, 0.001f)
        assertEquals(0.2f, volumes.secondary, 0.001f)
    }
}
