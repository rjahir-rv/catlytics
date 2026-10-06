package com.catlytics.feature.statistics.impl

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.catlytics.core.domain.usecase.statistics.StatsPeriodCalculator
import com.catlytics.core.model.StatsGranularity
import com.catlytics.feature.statistics.impl.components.ActivityChart
import com.catlytics.feature.statistics.impl.components.NarrativeSummaryCard
import com.catlytics.feature.statistics.impl.components.PeriodSelectorHeader
import com.catlytics.feature.statistics.impl.components.PeriodSummaryCard
import com.catlytics.feature.statistics.impl.components.StatsEmptyState
import com.catlytics.feature.statistics.impl.components.StatsSkeleton
import com.catlytics.feature.statistics.impl.components.TopAlbumItem
import com.catlytics.feature.statistics.impl.components.TopArtistItem
import com.catlytics.feature.statistics.impl.components.TopListCard
import com.catlytics.feature.statistics.impl.components.TopTrackItem
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

@Composable
internal fun StatisticsExploreScreen(
    modifier: Modifier = Modifier,
    viewModel: StatisticsExploreViewModel = hiltViewModel(),
    bottomPadding: () -> Dp = { 0.dp },
    scaffoldContentPadding: PaddingValues = PaddingValues(0.dp),
) {
    val uiState by viewModel.uiState.collectAsState()
    val contentPadding = statsContentPadding(scaffoldContentPadding, bottomPadding)

    Crossfade(
        targetState = uiState::class,
        modifier = modifier.fillMaxSize(),
        label = "statsExploreState",
    ) { kind ->
        when (kind) {
            StatisticsExploreUiState.Loading::class -> StatsSkeleton(contentPadding = contentPadding)

            StatisticsExploreUiState.Error::class -> StatsErrorState(contentPadding = contentPadding)

            else -> (uiState as? StatisticsExploreUiState.Success)?.let { state ->
                StatisticsExploreContent(
                    data = state.data,
                    onGranularityChange = viewModel::setGranularity,
                    onShift = viewModel::shiftPeriod,
                    contentPadding = contentPadding,
                )
            }
        }
    }
}

@Composable
private fun StatisticsExploreContent(
    data: StatisticsExploreData,
    onGranularityChange: (StatsGranularity) -> Unit,
    onShift: (Int) -> Unit,
    contentPadding: PaddingValues,
) {
    val stats = data.stats
    val entranceDone = rememberStatsEntranceDone()
    val dayCount = remember(stats.range) {
        StatsPeriodCalculator.dayCount(stats.range, Clock.systemDefaultZone())
    }
    // Solo el periodo actual contiene "hoy"; en periodos pasados todas las barras van igual.
    val todayIndex = remember(stats.range) {
        if (stats.range.offset != 0) {
            null
        } else {
            val zone = ZoneId.systemDefault()
            val start = Instant.ofEpochMilli(stats.range.startMillis).atZone(zone).toLocalDate()
            ChronoUnit.DAYS.between(start, LocalDate.now(zone)).toInt()
        }
    }
    val weekLabels = stringArrayResource(R.array.stats_weekday_short_labels).toList()
    val dayLabels = when (stats.range.granularity) {
        StatsGranularity.WEEK -> weekLabels
        StatsGranularity.MONTH -> null
    }
    val periodTitle = friendlyPeriodTitle(
        granularity = data.selection.granularity,
        offset = data.selection.offset,
        fallbackLabel = stats.range.label,
    )
    val periodSubtitle = if (data.selection.offset == 0 || data.selection.offset == -1) {
        stats.range.label.takeIf { it != periodTitle }
    } else {
        null
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = contentPadding,
    ) {
        item(key = "period-header") {
            PeriodSelectorHeader(
                granularity = data.selection.granularity,
                offset = data.selection.offset,
                title = periodTitle,
                subtitle = periodSubtitle,
                canGoBack = data.canGoBack,
                canGoForward = data.canGoForward,
                onGranularityChange = onGranularityChange,
                onShift = onShift,
                modifier = statsEntrance(0, entranceDone),
            )
        }

        item(key = "period-summary") {
            PeriodSummaryCard(
                totalListenedMillis = stats.totalListenedMillis,
                playCount = stats.playCount,
                uniqueTracks = stats.uniqueTracks,
                uniqueArtists = stats.uniqueArtists,
                uniqueAlbums = stats.uniqueAlbums,
                modifier = statsEntrance(1, entranceDone),
            )
        }

        item(key = "activity-chart") {
            ActivityChart(
                dailyListening = stats.dailyListening,
                dayCount = dayCount,
                title = when (stats.range.granularity) {
                    StatsGranularity.WEEK -> stringResource(R.string.stats_chart_weekly_title)
                    StatsGranularity.MONTH -> stringResource(R.string.stats_chart_monthly_title)
                },
                subtitle = stringResource(R.string.stats_chart_subtitle),
                dayLabels = dayLabels,
                todayIndex = todayIndex,
                modifier = statsEntrance(2, entranceDone),
            )
        }

        if (stats.isEmpty) {
            item(key = "period-empty") {
                StatsEmptyState(
                    title = stringResource(R.string.stats_empty_period_title),
                    subtitle = stringResource(R.string.stats_empty_period_subtitle),
                    modifier = statsEntrance(3, entranceDone),
                )
            }
        } else {
            if (data.narrative.eligible) {
                item(key = "narrative") {
                    NarrativeSummaryCard(
                        narrative = data.narrative,
                        title = stringResource(R.string.stats_summary_title),
                        modifier = statsEntrance(3, entranceDone),
                    )
                }
            }

            if (stats.topTracks.isNotEmpty()) {
                item(key = "top-tracks") {
                    TopListCard(
                        title = stringResource(R.string.stats_top_tracks_title),
                        modifier = statsEntrance(4, entranceDone),
                    ) {
                        stats.topTracks.forEachIndexed { index, track ->
                            TopTrackItem(
                                rank = index + 1,
                                track = track,
                                showDivider = index < stats.topTracks.lastIndex,
                            )
                        }
                    }
                }
            }

            if (stats.topArtists.isNotEmpty()) {
                item(key = "top-artists") {
                    TopListCard(
                        title = stringResource(R.string.stats_top_artists_title),
                        modifier = statsEntrance(5, entranceDone),
                    ) {
                        stats.topArtists.forEachIndexed { index, artist ->
                            TopArtistItem(
                                rank = index + 1,
                                artist = artist,
                                showDivider = index < stats.topArtists.lastIndex,
                            )
                        }
                    }
                }
            }

            if (stats.topAlbums.isNotEmpty()) {
                item(key = "top-albums") {
                    TopListCard(
                        title = stringResource(R.string.stats_top_albums_title),
                        modifier = statsEntrance(6, entranceDone),
                    ) {
                        stats.topAlbums.forEachIndexed { index, album ->
                            TopAlbumItem(
                                rank = index + 1,
                                album = album,
                                showDivider = index < stats.topAlbums.lastIndex,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun friendlyPeriodTitle(
    granularity: StatsGranularity,
    offset: Int,
    fallbackLabel: String,
): String {
    return when (granularity) {
        StatsGranularity.WEEK -> when (offset) {
            0 -> stringResource(R.string.stats_period_this_week)
            -1 -> stringResource(R.string.stats_period_last_week)
            else -> fallbackLabel
        }
        StatsGranularity.MONTH -> when (offset) {
            0 -> stringResource(R.string.stats_period_this_month)
            -1 -> stringResource(R.string.stats_period_last_month)
            else -> fallbackLabel
        }
    }
}
