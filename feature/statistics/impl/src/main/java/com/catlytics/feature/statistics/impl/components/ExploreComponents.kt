package com.catlytics.feature.statistics.impl.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.catlytics.core.model.StatsGranularity
import com.catlytics.feature.statistics.impl.formatListeningDuration
import com.catlytics.feature.statistics.impl.formatPlayCountLabel
import com.catlytics.core.designsystem.R as DesignSystemR
import com.catlytics.feature.statistics.impl.R as StatsR

@Composable
internal fun PeriodSelectorHeader(
    granularity: StatsGranularity,
    offset: Int,
    title: String,
    subtitle: String?,
    canGoBack: Boolean,
    canGoForward: Boolean,
    onGranularityChange: (StatsGranularity) -> Unit,
    onShift: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = granularity == StatsGranularity.WEEK,
                onClick = { onGranularityChange(StatsGranularity.WEEK) },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
            ) {
                Text(stringResource(StatsR.string.stats_granularity_week))
            }
            SegmentedButton(
                selected = granularity == StatsGranularity.MONTH,
                onClick = { onGranularityChange(StatsGranularity.MONTH) },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
            ) {
                Text(stringResource(StatsR.string.stats_granularity_month))
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(
                onClick = { onShift(-1) },
                enabled = canGoBack,
            ) {
                Icon(
                    painter = painterResource(DesignSystemR.drawable.ic_arrow_back),
                    contentDescription = stringResource(
                        StatsR.string.stats_period_previous_content_description,
                    ),
                )
            }
            AnimatedContent(
                targetState = PeriodHeading(granularity, offset, title, subtitle),
                modifier = Modifier.weight(1f),
                transitionSpec = {
                    val direction = when {
                        targetState.granularity != initialState.granularity -> 0
                        targetState.offset > initialState.offset -> 1
                        else -> -1
                    }
                    (slideInHorizontally { it / 3 * direction } + fadeIn())
                        .togetherWith(slideOutHorizontally { -it / 3 * direction } + fadeOut())
                },
                contentKey = { it.granularity to it.offset },
                label = "periodHeading",
            ) { heading ->
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = heading.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (!heading.subtitle.isNullOrBlank()) {
                        Text(
                            text = heading.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            IconButton(
                onClick = { onShift(1) },
                enabled = canGoForward,
            ) {
                Icon(
                    painter = painterResource(DesignSystemR.drawable.ic_arrow_forward),
                    contentDescription = stringResource(
                        StatsR.string.stats_period_next_content_description,
                    ),
                )
            }
        }
    }
}

@Composable
internal fun PeriodSummaryCard(
    totalListenedMillis: Long,
    playCount: Int,
    uniqueTracks: Int,
    uniqueArtists: Int,
    uniqueAlbums: Int,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
        shape = CardShape,
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = formatListeningDuration(animatedMillis(totalListenedMillis)),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                text = formatPlayCountLabel(animatedCount(playCount)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
            )
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                PeriodMetric(
                    value = uniqueTracks,
                    label = stringResource(StatsR.string.stats_period_metric_tracks),
                )
                PeriodMetric(
                    value = uniqueArtists,
                    label = stringResource(StatsR.string.stats_period_metric_artists),
                )
                PeriodMetric(
                    value = uniqueAlbums,
                    label = stringResource(StatsR.string.stats_period_metric_albums),
                )
            }
        }
    }
}

@Composable
private fun PeriodMetric(value: Int, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = animatedCount(value).toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
        )
    }
}

private data class PeriodHeading(
    val granularity: StatsGranularity,
    val offset: Int,
    val title: String,
    val subtitle: String?,
)
