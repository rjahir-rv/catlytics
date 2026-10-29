package com.catlytics.core.designsystem.component

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class CatlyticsTopAppBarColorsTest {
    @Test
    fun `scrolled container color falls back to the solid container color`() {
        val containerColor = Color(0xFF101418)

        assertEquals(containerColor, resolveScrolledContainerColor(containerColor, null))
    }

    @Test
    fun `explicit scrolled container color is preserved`() {
        val containerColor = Color(0xFF101418)
        val scrolledContainerColor = Color.Transparent

        assertEquals(
            scrolledContainerColor,
            resolveScrolledContainerColor(containerColor, scrolledContainerColor),
        )
    }
}
