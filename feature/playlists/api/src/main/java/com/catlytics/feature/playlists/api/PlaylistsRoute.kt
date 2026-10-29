package com.catlytics.feature.playlists.api

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object PlaylistsRoute : NavKey

@Serializable
data class PlaylistDetailRoute(
    val playlistId: String,
    val playlistName: String,
) : NavKey

/**
 * Pantalla completa para crear una playlist. [openDetailOnCreate] decide si al crear se
 * navega al detalle (desde Playlists) o solo se vuelve atrás (desde "Agregar a playlist").
 */
@Serializable
data class CreatePlaylistRoute(
    val initialTrackIds: List<String> = emptyList(),
    val openDetailOnCreate: Boolean = true,
) : NavKey
