package com.catlytics.app.navigation

import com.catlytics.app.playback.NowPlayingRoute
import com.catlytics.core.navigation.TopLevelBackStack
import com.catlytics.feature.home.api.HomeRoute
import com.catlytics.feature.library.api.LibraryAlbumRoute
import com.catlytics.feature.library.api.LibraryArtistRoute
import com.catlytics.feature.library.api.LibraryRoute
import com.catlytics.feature.settings.api.SettingsRoute
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppNavigatorTest {
    private val backStack = TopLevelBackStack(HomeRoute)
    private var isTwoPane = false
    private val navigator = AppNavigator(backStack) { isTwoPane }

    @Test
    fun `now playing stays in the back stack but out of the displayed stack`() {
        navigator.openNowPlaying()
        navigator.openNowPlaying()

        assertTrue(navigator.isNowPlayingOpen)
        assertEquals(listOf(HomeRoute, NowPlayingRoute), backStack.backStack.toList())
        assertEquals(listOf(HomeRoute), navigator.contentBackStack)

        navigator.closeNowPlaying()
        navigator.closeNowPlaying()

        assertFalse(navigator.isNowPlayingOpen)
        assertEquals(listOf(HomeRoute), backStack.backStack.toList())
    }

    @Test
    fun `opening a library detail from now playing closes it first`() {
        navigator.openNowPlaying()

        navigator.openLibraryDetail(LibraryAlbumRoute("album", "Album"))

        assertEquals(
            listOf(HomeRoute, LibraryRoute, LibraryAlbumRoute("album", "Album")),
            backStack.backStack.toList(),
        )
    }

    @Test
    fun `settings is not pushed twice`() {
        navigator.openSettings()
        navigator.openSettings()

        assertEquals(listOf(HomeRoute, SettingsRoute), backStack.backStack.toList())
    }

    @Test
    fun `in two panes a new detail replaces the open one`() {
        navigator.addTopLevel(LibraryRoute)
        navigator.openFromListPane(LibraryAlbumRoute("album", "Album"))
        isTwoPane = true

        navigator.openFromListPane(LibraryArtistRoute("artist", "Artist"))
        navigator.openFromListPane(LibraryArtistRoute("artist", "Artist"))

        assertEquals(
            listOf(HomeRoute, LibraryRoute, LibraryArtistRoute("artist", "Artist")),
            backStack.backStack.toList(),
        )
    }
}
