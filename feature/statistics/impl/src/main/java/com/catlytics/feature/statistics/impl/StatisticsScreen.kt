package com.catlytics.feature.statistics.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.catlytics.feature.statistics.impl.components.DashboardHeroCard
import com.catlytics.feature.statistics.impl.components.ExploreStatsCta
import com.catlytics.feature.statistics.impl.components.ListeningTotalsRow
import com.catlytics.feature.statistics.impl.components.NarrativeProgressHint
import com.catlytics.feature.statistics.impl.components.NarrativeSummaryCard
import com.catlytics.feature.statistics.impl.components.StatsEmptyState
import com.catlytics.feature.statistics.impl.components.TopListCard
import com.catlytics.feature.statistics.impl.components.TopTrackItem

@Composable
internal fun StatisticsScreen(
    modifier: Modifier = Modifier,
    viewModel: StatisticsViewModel = hiltViewModel(),
    bottomPadding: () -> androidx.compose.ui.unit.Dp = { 0.dp },
    scaffoldContentPadding: PaddingValues = PaddingValues(0.dp),
    onExploreClick: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(modifier = modifier.fillMaxSize()) {
        when (val state = uiState) {
            is StatisticsUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            is StatisticsUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.stats_error_loading),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            is StatisticsUiState.Success -> {
                StatisticsDashboardContent(
                    data = state.data,
                    onExploreClick = onExploreClick,
                    bottomPadding = bottomPadding,
                    scaffoldContentPadding = scaffoldContentPadding,
                )
            }
        }
    }
}

@Composable
private fun StatisticsDashboardContent(
    data: StatisticsDashboardData,
    onExploreClick: () -> Unit,
    bottomPadding: () -> androidx.compose.ui.unit.Dp,
    scaffoldContentPadding: PaddingValues,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(
            top = scaffoldContentPadding.calculateTopPadding() + 8.dp,
            bottom = bottomPadding() + 24.dp,
        ),
    ) {
        if (!data.hasAnyHistory) {
            item {
                StatsEmptyState(
                    title = stringResource(R.string.stats_empty_history_title),
                    subtitle = stringResource(R.string.stats_empty_history_subtitle),
                )
            }
            return@LazyColumn
        }

        item {
            DashboardHeroCard(
                streak = data.streak,
                totalListenedMillis = data.thisWeek.totalListenedMillis,
                playCount = data.thisWeek.playCount,
            )
        }

        item {
            ListeningTotalsRow(totals = data.totals)
        }

        if (data.narrative.eligible) {
            item {
                NarrativeSummaryCard(
                    narrative = data.narrative,
                    title = stringResource(R.string.stats_summary_week_title),
                )
            }
        } else {
            item {
                NarrativeProgressHint(
                    totalListenedMillis = data.thisWeek.totalListenedMillis,
                )
            }
        }

        // CTA early — primary path into deep stats.
        item {
            ExploreStatsCta(onClick = onExploreClick)
        }

        if (data.thisWeek.isEmpty) {
            item {
                StatsEmptyState(
                    title = stringResource(R.string.stats_empty_week_title),
                    subtitle = stringResource(R.string.stats_empty_week_subtitle),
                    compact = true,
                )
            }
        } else if (data.thisWeek.topTracks.isNotEmpty()) {
            val tracks = data.thisWeek.topTracks.take(5)
            item {
                TopListCard(
                    title = stringResource(R.string.stats_top_tracks_title),
                    actionLabel = stringResource(R.string.stats_top_see_all),
                    onAction = onExploreClick,
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
