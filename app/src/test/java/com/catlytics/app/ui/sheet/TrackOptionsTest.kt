package com.catlytics.app.ui.sheet

import com.catlytics.app.R
import com.catlytics.core.model.Artist
import com.catlytics.core.model.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class TrackOptionsTest {
    @Test
    fun `edit option is offered only when the host can open the editor`() {
        var edited = false
        val withEditor = options(onEditMetadata = { edited = true })
        val editOption = withEditor.single { it.textRes == R.string.app_track_option_edit_metadata }

        editOption.onClick()

        assertEquals(true, edited)
        assertFalse(options(onEditMetadata = null).any { it.textRes == R.string.app_track_option_edit_metadata })
    }

    private fun options(onEditMetadata: (() -> Unit)?) = buildTrackOptions(
        track = Track("t", "Título", Artist("a", "Artista"), 1L, mediaUri = "content://t"),
        isLiked = false,
        canAddToQueue = true,
        canRemoveFromPlaylist = false,
        callbacks = TrackOptionsCallbacks(
            onAddToPlaylist = {},
            onToggleLiked = {},
            onPlayNext = {},
            onAddToQueue = {},
            onGoToAlbum = {},
            onGoToArtist = {},
            onRemoveFromPlaylist = {},
            onEditMetadata = onEditMetadata,
        ),
    )
}
