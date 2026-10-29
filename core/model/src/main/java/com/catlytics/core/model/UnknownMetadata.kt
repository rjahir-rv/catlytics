package com.catlytics.core.model

/**
 * Valores canónicos PERSISTIDOS para metadatos ausentes (Room, eventos de reproducción,
 * respaldos de playlists y estadísticas). Forman parte de la identidad de los datos
 * (`artistIdentityKey`, `albumId` de fallback, `TrackReconciler`): no localizar ni cambiar el valor.
 */
const val UNKNOWN_ARTIST_NAME = "Artista desconocido"
const val UNKNOWN_ALBUM_TITLE = "Álbum desconocido"
const val UNTITLED_TRACK_TITLE = "Canción sin título"
