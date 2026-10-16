package com.catlytics.feature.statistics.impl

import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridItemScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.catlytics.core.designsystem.component.CatlyticsEmptyState
import com.catlytics.core.designsystem.modifier.staggeredEntrance
import com.catlytics.feature.statistics.impl.components.DashboardHeroCard
import com.catlytics.feature.statistics.impl.components.ExploreStatsCta
import com.catlytics.feature.statistics.impl.components.ListeningTotalsRow
import com.catlytics.feature.statistics.impl.components.NarrativeProgressHint
import com.catlytics.feature.statistics.impl.components.NarrativeSummaryCard
import com.catlytics.feature.statistics.impl.components.StatsEmptyState
import com.catlytics.feature.statistics.impl.components.StatsSkeleton
import com.catlytics.feature.statistics.impl.components.TopListCard
import com.catlytics.feature.statistics.impl.components.TopTrackItem
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@Composable
internal fun StatisticsScreen(
    modifier: Modifier = Modifier,
    viewModel: StatisticsViewModel = hiltViewModel(),
    bottomPadding: () -> Dp = { 0.dp },
    scaffoldContentPadding: PaddingValues = PaddingValues(0.dp),
    onExploreClick: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()
    val contentPadding = statsContentPadding(scaffoldContentPadding, bottomPadding)

    Crossfade(
        targetState = uiState::class,
        modifier = modifier.fillMaxSize(),
        label = "statsState",
    ) { kind ->
        when (kind) {
            StatisticsUiState.Loading::class -> StatsSkeleton(contentPadding = contentPadding)

            StatisticsUiState.Error::class -> StatsErrorState(contentPadding = contentPadding)

            else -> (uiState as? StatisticsUiState.Success)?.let { state ->
                if (state.data.hasAnyHistory) {
                    StatisticsDashboardContent(
                        data = state.data,
                        onExploreClick = onExploreClick,
                        contentPadding = contentPadding,
                    )
                } else {
                    CatlyticsEmptyState(
                        title = stringResource(R.string.stats_empty_history_title),
                        message = stringResource(R.string.stats_empty_history_subtitle),
                        messageColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(contentPadding),
                    )
                }
            }
        }
    }
}

@Composable
private fun StatisticsDashboardContent(
    data: StatisticsDashboardData,
    onExploreClick: () -> Unit,
    contentPadding: PaddingValues,
) {
    val entranceDone = rememberStatsEntranceDone()

    // Una columna en teléfonos; en pantallas anchas las tarjetas se acomodan lado a lado.
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Adaptive(STATS_GRID_MIN_COLUMN_WIDTH),
        modifier = Modifier.fillMaxSize(),
        verticalItemSpacing = 14.dp,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = contentPadding,
    ) {
        item(key = "hero", span = StaggeredGridItemSpan.FullLine) {
            DashboardHeroCard(
                streak = data.streak,
                totalListenedMillis = data.thisWeek.totalListenedMillis,
                playCount = data.thisWeek.playCount,
                modifier = statsEntrance(0, entranceDone),
            )
        }

        item(key = "totals", span = StaggeredGridItemSpan.FullLine) {
            ListeningTotalsRow(
                totals = data.totals,
                modifier = statsEntrance(1, entranceDone),
            )
        }

        if (data.narrative.eligible) {
            item(key = "narrative") {
                NarrativeSummaryCard(
                    narrative = data.narrative,
                    title = stringResource(R.string.stats_summary_week_title),
                    modifier = statsEntrance(2, entranceDone),
                )
            }
        } else if (data.thisWeek.totalListenedMillis > 0) {
            item(key = "narrative-progress") {
                NarrativeProgressHint(
                    totalListenedMillis = data.thisWeek.totalListenedMillis,
                    modifier = statsEntrance(2, entranceDone),
                )
            }
        }

        // CTA early — primary path into deep stats.
        item(key = "explore-cta") {
            ExploreStatsCta(
                onClick = onExploreClick,
                modifier = statsEntrance(3, entranceDone),
            )
        }

        if (data.thisWeek.isEmpty) {
            item(key = "week-empty") {
                StatsEmptyState(
                    title = stringResource(R.string.stats_empty_week_title),
                    subtitle = stringResource(R.string.stats_empty_week_subtitle),
                    modifier = statsEntrance(4, entranceDone),
                )
            }
        } else if (data.thisWeek.topTracks.isNotEmpty()) {
            val tracks = data.thisWeek.topTracks.take(5)
            item(key = "top-tracks") {
                TopListCard(
                    title = stringResource(R.string.stats_top_tracks_title),
                    actionLabel = stringResource(R.string.stats_top_see_all),
                    onAction = onExploreClick,
                    modifier = statsEntrance(4, entranceDone),
                ) {
                    tracks.forEachIndexed { index, track ->
                        TopTrackItem(
                            rank = index + 1,
                            track = track,
                            showDivider = index < tracks.lastIndex,
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun LazyStaggeredGridItemScope.statsEntrance(index: Int, entranceDone: Boolean): Modifier =
    Modifier
        .animateItem()
        .staggeredEntrance(index, animate = !entranceDone && index < ENTRANCE_MAX_STAGGERED_ITEMS)

@Composable
internal fun StatsErrorState(contentPadding: PaddingValues) {
    CatlyticsEmptyState(
        message = stringResource(R.string.stats_error_loading),
        messageColor = MaterialTheme.colorScheme.error,
        mascotSize = 160.dp,
        modifier = Modifier.padding(contentPadding),
    )
}

@Composable
internal fun statsContentPadding(
    scaffoldContentPadding: PaddingValues,
    bottomPadding: () -> Dp,
): PaddingValues = PaddingValues(
    start = STATS_SCREEN_PADDING,
    end = STATS_SCREEN_PADDING,
    top = scaffoldContentPadding.calculateTopPadding() + 8.dp,
    bottom = bottomPadding() + 24.dp,
)

@Composable
internal fun rememberStatsEntranceDone(): Boolean {
    var entranceDone by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!entranceDone) {
            delay(ENTRANCE_SETTLE_MILLIS.milliseconds)
            entranceDone = true
        }
    }
    return entranceDone
}

private const val ENTRANCE_MAX_STAGGERED_ITEMS = 6
private const val ENTRANCE_SETTLE_MILLIS = 900L
private val STATS_SCREEN_PADDING = 20.dp

internal val STATS_GRID_MIN_COLUMN_WIDTH = 360.dp
