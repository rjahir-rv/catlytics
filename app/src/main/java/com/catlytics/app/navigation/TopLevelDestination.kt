package com.catlytics.app.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.navigation3.runtime.NavKey
import com.catlytics.app.R as AppR
import com.catlytics.core.designsystem.R
import com.catlytics.feature.home.api.HomeRoute
import com.catlytics.feature.library.api.LibraryRoute
import com.catlytics.feature.playlists.api.PlaylistsRoute
import com.catlytics.feature.statistics.api.StatisticsRoute

enum class TopLevelDestination(
    val route: NavKey,
    @param:StringRes val labelRes: Int,
    @param:DrawableRes val iconRes: Int,
) {
    Home(
        route = HomeRoute,
        labelRes = AppR.string.app_nav_home,
        iconRes = R.drawable.ic_home,
    ),
    Library(
        route = LibraryRoute,
        labelRes = AppR.string.app_nav_library,
        iconRes = R.drawable.ic_library,
    ),
    Playlists(
        route = PlaylistsRoute,
        labelRes = AppR.string.app_nav_playlists,
        iconRes = R.drawable.ic_playlist,
    ),
    Statistics(
        route = StatisticsRoute,
        labelRes = AppR.string.app_nav_statistics,
        iconRes = R.drawable.ic_line_chart,
    ),
}
