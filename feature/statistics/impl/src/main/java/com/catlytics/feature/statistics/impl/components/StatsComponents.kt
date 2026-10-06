package com.catlytics.feature.statistics.impl.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.crossfade
import com.catlytics.core.designsystem.component.nameSeededGradient
import com.catlytics.core.designsystem.modifier.pressScale
import com.catlytics.core.designsystem.theme.CatlyticsCorners
import com.catlytics.core.domain.usecase.statistics.BuildListeningNarrativeUseCase
import com.catlytics.core.model.DailyListeningStat
import com.catlytics.core.model.ListeningNarrative
import com.catlytics.core.model.ListeningNarrativeKind
import com.catlytics.core.model.ListeningStreak
import com.catlytics.core.model.ListeningTotals
import com.catlytics.core.model.StatsGranularity
import com.catlytics.core.model.TopAlbum
import com.catlytics.core.model.TopArtist
import com.catlytics.core.model.TopTrack
import com.catlytics.feature.statistics.impl.R as StatsR
import com.catlytics.feature.statistics.impl.formatListeningDuration
import com.catlytics.feature.statistics.impl.formatPlayCountLabel

private val CardShape = CatlyticsCorners.Large
private val SoftShape = CatlyticsCorners.Medium

@Composable
internal fun DashboardHeroCard(
    streak: ListeningStreak,
    totalListenedMillis: Long,
    playCount: Int,
    modifier: Modifier = Modifier,
) {
    val hasStreak = streak.currentDays > 0
    val container = MaterialTheme.colorScheme.primaryContainer
    val gradientTarget = MaterialTheme.colorScheme.tertiaryContainer
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = CardShape,
    ) {
        Column(
            modifier = Modifier
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(container, lerp(container, gradientTarget, HERO_GRADIENT_BLEND)),
                    ),
                )
                .padding(20.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(StatsR.string.stats_dashboard_this_week),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                )
                StreakPill(
                    streak = streak,
                    hasStreak = hasStreak,
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = formatListeningDuration(animatedMillis(totalListenedMillis)),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = formatPlayCountLabel(animatedCount(playCount)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
            )
        }
    }
}

@Composable
private fun StreakPill(
    streak: ListeningStreak,
    hasStreak: Boolean,
) {
    val container = if (hasStreak) {
        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.22f)
    } else {
        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.10f)
    }
    val content = if (hasStreak) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
    }
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(container)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.LocalFireDepartment,
            contentDescription = null,
            tint = content,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = if (hasStreak) {
                pluralStringResource(
                    StatsR.plurals.stats_streak_days,
                    streak.currentDays,
                    streak.currentDays,
                )
            } else {
                stringResource(StatsR.string.stats_streak_none)
            },
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = content,
        )
    }
}

@Composable
internal fun ListeningTotalsRow(
    totals: ListeningTotals,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(StatsR.string.stats_totals_title),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp, start = 2.dp),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ListeningTotalItem(
                label = stringResource(StatsR.string.stats_totals_tracks),
                value = totals.trackCount,
                modifier = Modifier.weight(1f),
            )
            ListeningTotalItem(
                label = stringResource(StatsR.string.stats_totals_artists),
                value = totals.artistCount,
                modifier = Modifier.weight(1f),
            )
            ListeningTotalItem(
                label = stringResource(StatsR.string.stats_totals_albums),
                value = totals.albumCount,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ListeningTotalItem(
    label: String,
    value: Int,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        shape = SoftShape,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = animatedCount(value).toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
internal fun NarrativeSummaryCard(
    modifier: Modifier = Modifier,
    narrative: ListeningNarrative,
    title: String = stringResource(StatsR.string.stats_summary_title)
) {
    if (!narrative.eligible) return

    val artworkUri = narrative.topArtist?.artworkUri
        ?: narrative.topTrack?.artworkUri
    val seed = (narrative.topArtist?.name ?: narrative.topTrack?.title).orEmpty()
    val colorScheme = MaterialTheme.colorScheme
    val isDark = colorScheme.surface.luminance() < 0.5f
    val gradient = remember(seed, isDark, colorScheme.surface) {
        nameSeededGradient(seed, isDark, colorScheme.surface)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = CardShape,
    ) {
        Row(
            modifier = Modifier
                .background(Brush.linearGradient(listOf(gradient.start, gradient.center)))
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ArtworkBox(
                uri = artworkUri,
                seed = seed,
                circular = true,
                size = 72.dp,
                contentDescription = seed.ifEmpty { null },
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                )
                Spacer(modifier = Modifier.height(6.dp))
                NarrativeHeadline(narrative = narrative)
                if (narrative.topTrack != null || narrative.topArtist != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    narrative.topTrack?.let { track ->
                        NarrativeStatRow(
                            label = stringResource(StatsR.string.stats_narrative_top_track_label),
                            value = "${track.title} · ${formatPlayCountLabel(track.playCount)}",
                        )
                    }
                    narrative.topArtist?.let { artist ->
                        NarrativeStatRow(
                            label = stringResource(StatsR.string.stats_narrative_top_artist_label),
                            value = "${artist.name} · ${formatListeningDuration(artist.totalListenedMillis)}",
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NarrativeHeadline(narrative: ListeningNarrative) {
    when (narrative.kind) {
        ListeningNarrativeKind.TimeWithArtist -> {
            NarrativeHeadlineText(
                label = stringResource(StatsR.string.stats_narrative_time_with_artist),
                value = (narrative.topArtist?.name ?: narrative.topTrack?.title).orEmpty(),
            )
        }
        ListeningNarrativeKind.FavoriteTrack -> {
            NarrativeHeadlineText(
                label = stringResource(StatsR.string.stats_narrative_favorite_track),
                value = narrative.topTrack?.title.orEmpty(),
            )
        }
        ListeningNarrativeKind.Summary -> {
            Text(
                text = stringResource(StatsR.string.stats_narrative_default_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun NarrativeHeadlineText(
    label: String,
    value: String,
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun NarrativeStatRow(
    label: String,
    value: String,
) {
    Column(modifier = Modifier.padding(vertical = 3.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
internal fun NarrativeProgressHint(
    modifier: Modifier = Modifier,
    totalListenedMillis: Long,
    thresholdMillis: Long = BuildListeningNarrativeUseCase.ELIGIBILITY_THRESHOLD_MILLIS,
) {
    if (totalListenedMillis !in 1..<thresholdMillis) return
    val targetProgress = (totalListenedMillis.toFloat() / thresholdMillis.toFloat()).coerceIn(0f, 1f)
    val remaining = thresholdMillis - totalListenedMillis
    val progress = remember { Animatable(0f) }
    LaunchedEffect(targetProgress) {
        progress.animateTo(
            targetValue = targetProgress,
            animationSpec = tween(PROGRESS_FILL_MILLIS, easing = FastOutSlowInEasing),
        )
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        shape = SoftShape,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(StatsR.string.stats_narrative_unlock_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(
                    StatsR.string.stats_narrative_unlock_hint,
                    formatListeningDuration(remaining),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { progress.value },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(50)),
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(
                    StatsR.string.stats_narrative_unlock_progress,
                    formatListeningDuration(totalListenedMillis),
                    formatListeningDuration(thresholdMillis),
                ),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
internal fun ExploreStatsCta(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    Card(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactionSource,
        modifier = modifier
            .pressScale(interactionSource)
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
        shape = CardShape,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.Insights,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(26.dp),
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(StatsR.string.stats_explore_cta_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Text(
                    text = stringResource(StatsR.string.stats_explore_cta_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}

@Composable
internal fun StatsEmptyState(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(SoftShape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(vertical = 20.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.MusicNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(32.dp),
            )
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp),
                textAlign = TextAlign.Center,
            )
        }
    }
}

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
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
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
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
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

@Composable
internal fun ActivityChart(
    modifier: Modifier = Modifier,
    dailyListening: List<DailyListeningStat>,
    dayCount: Int,
    title: String,
    subtitle: String,
    dayLabels: List<String>? = null,
    todayIndex: Int? = null,
) {
    val safeDayCount = dayCount.coerceAtLeast(1)
    val dailyMinutes = remember(dailyListening, safeDayCount) {
        List(safeDayCount) { index ->
            dailyListening.firstOrNull { it.dayIndex == index + 1 }
                ?.totalListenedMillis
                ?.div(60_000f)
                ?: 0f
        }
    }
    val maxMinutes = remember(dailyMinutes) {
        dailyMinutes.maxOrNull()?.coerceAtLeast(1f) ?: 1f
    }
    val weekDayLabels = stringArrayResource(StatsR.array.stats_weekday_short_labels)
    val resolvedLabels = dayLabels ?: defaultDayLabels(safeDayCount, weekDayLabels.toList())
    var selectedIndex by remember(safeDayCount) { mutableStateOf<Int?>(null) }
    // Mantiene el último día tocado para que el texto no desaparezca de golpe al ocultarse.
    var lastSelectedIndex by remember(safeDayCount) { mutableStateOf(0) }
    val entrance = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        entrance.animateTo(
            targetValue = 1f,
            animationSpec = tween(CHART_ENTRANCE_MILLIS, easing = FastOutSlowInEasing),
        )
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        shape = CardShape,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            val selected = selectedIndex
            val shownIndex = lastSelectedIndex.coerceIn(dailyMinutes.indices)
            AnimatedVisibility(
                visible = selected != null && selected in dailyMinutes.indices,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Text(
                    text = stringResource(
                        StatsR.string.stats_chart_selected_day,
                        shownIndex + 1,
                        dailyMinutes[shownIndex].toInt(),
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            val barSpacing = if (safeDayCount > 14) 2.dp else 4.dp
            val activeColor = MaterialTheme.colorScheme.primary
            val restColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
            val dimmedColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
            val trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            val emptyTickColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .padding(top = 4.dp)
                    .clip(SoftShape)
                    .background(trackColor)
                    .padding(horizontal = 6.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(barSpacing),
                verticalAlignment = Alignment.Bottom,
            ) {
                dailyMinutes.forEachIndexed { index, minutes ->
                    DayActivityBar(
                        minutes = minutes,
                        maxMinutes = maxMinutes,
                        entranceProgress = { entrance.value },
                        barColor = when {
                            selectedIndex != null ->
                                if (selectedIndex == index) activeColor else dimmedColor
                            todayIndex != null ->
                                if (todayIndex == index) activeColor else restColor
                            else -> activeColor
                        },
                        emptyTickColor = emptyTickColor,
                        onClick = {
                            if (selectedIndex == index) {
                                selectedIndex = null
                            } else {
                                selectedIndex = index
                                lastSelectedIndex = index
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    )
                }
            }

            if (resolvedLabels.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    resolvedLabels.forEach { day ->
                        Text(
                            text = day,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Clip,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DayActivityBar(
    minutes: Float,
    maxMinutes: Float,
    entranceProgress: () -> Float,
    barColor: Color,
    emptyTickColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val fraction = (minutes / maxMinutes).coerceIn(0f, 1f)
    val animatedFraction by animateFloatAsState(
        targetValue = fraction,
        label = "dayBar",
    )
    val animatedColor by animateColorAsState(targetValue = barColor, label = "dayBarColor")
    val minVisible = if (minutes > 0f) 0.06f else 0f
    val heightFraction = if (minutes > 0f) {
        animatedFraction.coerceAtLeast(minVisible)
    } else {
        0f
    }
    val barShape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier.clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick,
        ),
        contentAlignment = Alignment.BottomCenter,
    ) {
        if (heightFraction > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(heightFraction)
                    .graphicsLayer {
                        // Crecen desde la base la primera vez que aparece la gráfica.
                        transformOrigin = TransformOrigin(0.5f, 1f)
                        scaleY = entranceProgress()
                    }
                    .clip(barShape)
                    .background(animatedColor),
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(emptyTickColor),
            )
        }
    }
}

private fun defaultDayLabels(dayCount: Int, weekDayLabels: List<String>): List<String> {
    if (dayCount <= 0 || weekDayLabels.isEmpty()) return emptyList()
    if (dayCount <= 7) {
        return List(dayCount) { weekDayLabels[it % weekDayLabels.size] }
    }
    val step = when {
        dayCount <= 14 -> 2
        dayCount <= 21 -> 3
        else -> 5
    }
    return List(dayCount) { index ->
        val day = index + 1
        when {
            day == 1 || day == dayCount || day % step == 0 -> day.toString()
            else -> ""
        }
    }
}

@Composable
internal fun TopListCard(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        shape = CardShape,
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            SectionTitle(
                text = title,
                actionLabel = actionLabel,
                onAction = onAction,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
            content()
        }
    }
}

@Composable
internal fun TopTrackItem(
    rank: Int,
    track: TopTrack,
    modifier: Modifier = Modifier,
    showDivider: Boolean = false,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RankBadge(rank = rank)
            Spacer(modifier = Modifier.width(12.dp))
            ArtworkBox(uri = track.artworkUri, seed = track.title, circular = false)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = track.artistName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = formatPlayCountLabel(track.playCount),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End,
            )
        }
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 72.dp, end = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            )
        }
    }
}

@Composable
internal fun TopArtistItem(
    rank: Int,
    artist: TopArtist,
    modifier: Modifier = Modifier,
    showDivider: Boolean = false,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RankBadge(rank = rank)
            Spacer(modifier = Modifier.width(12.dp))
            ArtworkBox(uri = artist.artworkUri, seed = artist.name, circular = true)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = artist.name,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = formatListeningDuration(artist.totalListenedMillis),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = formatPlayCountLabel(artist.playCount),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End,
            )
        }
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 72.dp, end = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            )
        }
    }
}

@Composable
internal fun TopAlbumItem(
    rank: Int,
    album: TopAlbum,
    modifier: Modifier = Modifier,
    showDivider: Boolean = false,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RankBadge(rank = rank)
            Spacer(modifier = Modifier.width(12.dp))
            ArtworkBox(uri = album.artworkUri, seed = album.title, circular = false)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = album.title,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = album.artistName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = formatListeningDuration(album.totalListenedMillis),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End,
            )
        }
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 72.dp, end = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            )
        }
    }
}

@Composable
private fun RankBadge(rank: Int) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = rank.toString(),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

@Composable
private fun ArtworkBox(
    uri: String?,
    seed: String,
    circular: Boolean,
    size: Dp = 48.dp,
    contentDescription: String? = null,
) {
    val colorScheme = MaterialTheme.colorScheme
    val isDark = colorScheme.surface.luminance() < 0.5f
    val gradient = remember(seed, isDark, colorScheme.surface) {
        nameSeededGradient(seed, isDark, colorScheme.surface)
    }
    // El degradado con la inicial queda debajo: se ve mientras carga o si no hay carátula.
    Box(
        modifier = Modifier
            .size(size)
            .clip(if (circular) CircleShape else CatlyticsCorners.Small)
            .background(Brush.linearGradient(listOf(gradient.start, gradient.center))),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = seed.trim().firstOrNull()?.uppercase().orEmpty(),
            style = if (size > 56.dp) {
                MaterialTheme.typography.headlineSmall
            } else {
                MaterialTheme.typography.titleMedium
            },
            fontWeight = FontWeight.Bold,
            color = colorScheme.onSurface.copy(alpha = 0.7f),
        )
        if (uri != null) {
            AsyncImage(
                model = coil3.request.ImageRequest.Builder(coil3.compose.LocalPlatformContext.current)
                    .data(uri)
                    .crossfade(true)
                    .build(),
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
internal fun SectionTitle(
    text: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium,
        )
        if (actionLabel != null && onAction != null) {
            val interactionSource = remember { MutableInteractionSource() }
            Text(
                text = actionLabel,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .pressScale(interactionSource, pressedScale = 0.94f)
                    .clip(RoundedCornerShape(50))
                    .clickable(interactionSource = interactionSource, indication = ripple(), onClick = onAction)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }
}

private data class PeriodHeading(
    val granularity: StatsGranularity,
    val offset: Int,
    val title: String,
    val subtitle: String?,
)

@Composable
private fun animatedMillis(millis: Long): Long {
    val animated by animateFloatAsState(
        targetValue = millis.toFloat(),
        animationSpec = tween(COUNTER_ANIMATION_MILLIS, easing = FastOutSlowInEasing),
        label = "animatedMillis",
    )
    return animated.toLong()
}

@Composable
private fun animatedCount(count: Int): Int {
    val animated by animateIntAsState(
        targetValue = count,
        animationSpec = tween(COUNTER_ANIMATION_MILLIS, easing = FastOutSlowInEasing),
        label = "animatedCount",
    )
    return animated
}

private const val HERO_GRADIENT_BLEND = 0.35f
private const val PROGRESS_FILL_MILLIS = 700
private const val CHART_ENTRANCE_MILLIS = 600
private const val COUNTER_ANIMATION_MILLIS = 500
