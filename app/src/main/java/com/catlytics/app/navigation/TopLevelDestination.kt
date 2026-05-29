package com.catlytics.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavKey
import com.catlytics.feature.home.api.HomeRoute
import com.catlytics.feature.library.api.LibraryRoute
import com.catlytics.feature.playlists.api.PlaylistsRoute
import com.catlytics.feature.statistics.api.StatisticsRoute

enum class TopLevelDestination(
    val route: NavKey,
    val label: String,
    val icon: ImageVector,
) {
    Home(
        route = HomeRoute,
        label = "Inicio",
        icon = Icons.Filled.Home,
    ),
    Library(
        route = LibraryRoute,
        label = "Biblioteca",
        icon = Icons.Filled.LibraryMusic,
    ),
    Playlists(
        route = PlaylistsRoute,
        label = "Playlists",
        icon = Icons.AutoMirrored.Filled.QueueMusic,
    ),
    Statistics(
        route = StatisticsRoute,
        label = "Estadisticas",
        icon = Icons.Filled.Analytics,
    ),
}
