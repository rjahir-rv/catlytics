package com.catlytics.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Estos valores están persistidos en Room, eventos de reproducción y respaldos de
 * playlists/estadísticas. Cambiarlos rompe el emparejamiento de respaldos existentes y
 * fragmenta agrupaciones de artistas/álbumes; si es intencional, hace falta una migración.
 */
class UnknownMetadataTest {

    @Test
    fun persistedValuesAreStable() {
        assertEquals("Artista desconocido", UNKNOWN_ARTIST_NAME)
        assertEquals("Álbum desconocido", UNKNOWN_ALBUM_TITLE)
        assertEquals("Canción sin título", UNTITLED_TRACK_TITLE)
    }
}
