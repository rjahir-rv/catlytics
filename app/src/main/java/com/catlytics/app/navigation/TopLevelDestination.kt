package com.catlytics.app.navigation

import androidx.annotation.DrawableRes
import androidx.navigation3.runtime.NavKey
import com.catlytics.core.designsystem.R
import com.catlytics.feature.home.api.HomeRoute
import com.catlytics.feature.library.api.LibraryRoute
import com.catlytics.feature.playlists.api.PlaylistsRoute
import com.catlytics.feature.statistics.api.StatisticsRoute

enum class TopLevelDestination(
    val route: NavKey,
    val label: String,
    @param:DrawableRes val iconRes: Int,
) {
    Home(
        route = HomeRoute,
        label = "Inicio",
        iconRes = R.drawable.ic_home,
    ),
    Library(
        route = LibraryRoute,
        label = "Biblioteca",
        iconRes = R.drawable.ic_library,
    ),
    Playlists(
        route = PlaylistsRoute,
        label = "Playlists",
        iconRes = R.drawable.ic_playlist,
    ),
    Statistics(
        route = StatisticsRoute,
        label = "Estadisticas",
        iconRes = R.drawable.ic_line_chart,
    ),
}
