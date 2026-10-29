package com.catlytics.core.model

data class HomeRecommendationsSettings(
    val showRecommendedPlaylists: Boolean = true,
    val recentAddedWindow: RecentAddedWindow = RecentAddedWindow.Days21,
    val showNewTrackBadge: Boolean = true,
)

enum class RecentAddedWindow(val days: Int) {
    Days21(21),
    Days14(14),
    Days10(10),
    ;

    val windowMillis: Long
        get() = days * 24L * 60L * 60L * 1_000L
}
