package com.catlytics.core.widget

import com.catlytics.core.model.Artist
import com.catlytics.core.model.PlaybackState
import com.catlytics.core.model.PlaybackStatus
import com.catlytics.core.model.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NowPlayingWidgetStateTest {
    private val track = Track(
        id = "1",
        title = "Canción",
        artist = Artist(id = "a", name = "Artista"),
        durationMillis = 180_000L,
        mediaUri = "content://media/external/audio/media/1",
        artworkUri = "content://media/external/audio/albumart/7",
    )

    @Test
    fun `sin pista no hay estado en vivo`() {
        assertNull(PlaybackState().toWidgetState())
    }

    @Test
    fun `reproduciendo o cargando cuenta como sonando`() {
        listOf(PlaybackStatus.Playing, PlaybackStatus.Buffering).forEach { status ->
            assertTrue(PlaybackState(status = status, currentTrack = track).toWidgetState()!!.isPlaying)
        }
        listOf(PlaybackStatus.Paused, PlaybackStatus.Ended, PlaybackStatus.Idle).forEach { status ->
            assertFalse(PlaybackState(status = status, currentTrack = track).toWidgetState()!!.isPlaying)
        }
    }

    @Test
    fun `la posicion no cambia el estado del widget`() {
        val first = PlaybackState(status = PlaybackStatus.Playing, currentTrack = track, positionMillis = 1_000L)
        val second = first.copy(positionMillis = 2_000L, bufferedPositionMillis = 9_000L)

        assertEquals(first.toWidgetState(), second.toWidgetState())
    }

    @Test
    fun `copia titulo artista y caratula de la pista`() {
        val state = PlaybackState(status = PlaybackStatus.Paused, currentTrack = track).toWidgetState()!!

        assertEquals(
            NowPlayingWidgetState(
                hasTrack = true,
                title = "Canción",
                artist = "Artista",
                artworkUri = "content://media/external/audio/albumart/7",
                isPlaying = false,
            ),
            state,
        )
    }

    @Test
    fun `con sesion guardada se conserva lo pintado pero en pausa`() {
        val cached = NowPlayingWidgetState(hasTrack = true, title = "Canción", artist = "Artista", isPlaying = true)

        assertEquals(cached.copy(isPlaying = false), resolveWidgetState(null, hasSavedSession = true, cached))
    }

    @Test
    fun `sin sesion guardada el widget queda vacio`() {
        val cached = NowPlayingWidgetState(hasTrack = true, title = "Canción", artist = "Artista")

        assertEquals(NowPlayingWidgetState(), resolveWidgetState(null, hasSavedSession = false, cached))
    }

    @Test
    fun `el estado en vivo manda sobre la cache`() {
        val live = NowPlayingWidgetState(hasTrack = true, title = "Nueva", artist = "Otra", isPlaying = true)
        val cached = NowPlayingWidgetState(hasTrack = true, title = "Vieja", artist = "Artista")

        assertEquals(live, resolveWidgetState(live, hasSavedSession = true, cached))
    }
}
