package com.catlytics.app.ui

import androidx.compose.runtime.saveable.SaverScope
import com.catlytics.feature.home.api.HomeRoute
import com.catlytics.feature.library.api.LibraryArtistRoute
import com.catlytics.feature.settings.api.SettingsRoute
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppSearchStateTest {
    @Test
    fun `saver restores every search field`() {
        val state = AppSearchState().apply {
            home.isExpanded = true
            home.query = "nova"
            artist.query = "luz"
        }

        val saved = with(AppSearchState.Saver) { SaverScope { true }.save(state) }
        val restored = AppSearchState.Saver.restore(saved!!)!!

        assertTrue(restored.home.isExpanded)
        assertEquals("nova", restored.home.query)
        assertEquals("luz", restored.artist.query)
        assertFalse(restored.library.isExpanded)
    }

    @Test
    fun `fields are found by route`() {
        val state = AppSearchState()
        state.artist.isExpanded = true

        assertTrue(state.isExpanded(LibraryArtistRoute("artist", "Artist")))
        assertFalse(state.isExpanded(HomeRoute))
        assertNull(state.fieldFor(SettingsRoute))

        state.artist.clear()

        assertFalse(state.artist.isExpanded)
        assertEquals("", state.artist.query)
    }
}
