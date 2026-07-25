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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.catlytics.core.domain.usecase.statistics.StatsPeriodCalculator
import com.catlytics.core.model.StatsGranularity
import com.catlytics.feature.statistics.impl.components.ActivityChart
import com.catlytics.feature.statistics.impl.components.NarrativeSummaryCard
import com.catlytics.feature.statistics.impl.components.PeriodSelectorHeader
import com.catlytics.feature.statistics.impl.components.PeriodSummaryCard
import com.catlytics.feature.statistics.impl.components.StatsEmptyState
import com.catlytics.feature.statistics.impl.components.TopAlbumItem
import com.catlytics.feature.statistics.impl.components.TopArtistItem
import com.catlytics.feature.statistics.impl.components.TopListCard
import com.catlytics.feature.statistics.impl.components.TopTrackItem
import java.time.Clock

@Composable
internal fun StatisticsExploreScreen(
    modifier: Modifier = Modifier,
    viewModel: StatisticsExploreViewModel = hiltViewModel(),
    bottomPadding: () -> androidx.compose.ui.unit.Dp = { 0.dp },
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(modifier = modifier.fillMaxSize()) {
        when (val state = uiState) {
            is StatisticsExploreUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            is StatisticsExploreUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Ocurrió un error al cargar las estadísticas.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            is StatisticsExploreUiState.Success -> {
                StatisticsExploreContent(
                    data = state.data,
                    onGranularityChange = viewModel::setGranularity,
                    onShift = viewModel::shiftPeriod,
                    bottomPadding = bottomPadding,
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
    bottomPadding: () -> androidx.compose.ui.unit.Dp,
) {
    val stats = data.stats
    val dayCount = remember(stats.range) {
        StatsPeriodCalculator.dayCount(stats.range, Clock.systemDefaultZone())
    }
    val weekLabels = remember { listOf("L", "M", "X", "J", "V", "S", "D") }
    val dayLabels = when (stats.range.granularity) {
        StatsGranularity.WEEK -> weekLabels
        StatsGranularity.MONTH -> null
    }
    val periodTitle = remember(data.selection, stats.range.label) {
        friendlyPeriodTitle(
            granularity = data.selection.granularity,
            offset = data.selection.offset,
            fallbackLabel = stats.range.label,
        )
    }
    val periodSubtitle = remember(data.selection.offset, stats.range.label, periodTitle) {
        if (data.selection.offset == 0 || data.selection.offset == -1) {
            stats.range.label.takeIf { it != periodTitle }
        } else {
            null
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = bottomPadding() + 24.dp),
    ) {
        item {
            PeriodSelectorHeader(
                granularity = data.selection.granularity,
                title = periodTitle,
                subtitle = periodSubtitle,
                canGoBack = data.canGoBack,
                canGoForward = data.canGoForward,
                onGranularityChange = onGranularityChange,
                onShift = onShift,
            )
        }

        item {
            PeriodSummaryCard(
                totalListenedMillis = stats.totalListenedMillis,
                playCount = stats.playCount,
                uniqueTracks = stats.uniqueTracks,
                uniqueArtists = stats.uniqueArtists,
                uniqueAlbums = stats.uniqueAlbums,
            )
        }

        item {
            ActivityChart(
                dailyListening = stats.dailyListening,
                dayCount = dayCount,
                title = when (stats.range.granularity) {
                    StatsGranularity.WEEK -> "Actividad semanal"
                    StatsGranularity.MONTH -> "Actividad del mes"
                },
                subtitle = "Minutos escuchados por día",
                dayLabels = dayLabels,
            )
        }

        if (stats.isEmpty) {
            item {
                StatsEmptyState(
                    title = "Sin reproducciones en este periodo",
                    subtitle = "Prueba otra semana o mes, o vuelve a escuchar música.",
                    compact = true,
                )
            }
        } else {
            if (data.narrative.eligible) {
                item {
                    NarrativeSummaryCard(
                        narrative = data.narrative,
                        title = "Tu resumen",
                    )
                }
            }

            if (stats.topTracks.isNotEmpty()) {
                item {
                    TopListCard(title = "Top canciones") {
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
                item {
                    TopListCard(title = "Top artistas") {
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
                item {
                    TopListCard(title = "Top álbumes") {
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

private fun friendlyPeriodTitle(
    granularity: StatsGranularity,
    offset: Int,
    fallbackLabel: String,
): String {
    return when (granularity) {
        StatsGranularity.WEEK -> when (offset) {
            0 -> "Esta semana"
            -1 -> "Semana pasada"
            else -> fallbackLabel
        }
        StatsGranularity.MONTH -> when (offset) {
            0 -> "Este mes"
            -1 -> "Mes pasado"
            else -> fallbackLabel
        }
    }
}
