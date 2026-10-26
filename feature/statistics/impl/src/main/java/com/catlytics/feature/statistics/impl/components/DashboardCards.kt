package com.catlytics.feature.statistics.impl.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.catlytics.core.designsystem.R as DesignSystemR
import com.catlytics.core.designsystem.component.rememberNameSeededGradient
import com.catlytics.core.designsystem.modifier.pressScale
import com.catlytics.core.designsystem.text.displayArtistName
import com.catlytics.core.designsystem.text.displayTrackTitle
import com.catlytics.core.domain.usecase.statistics.BuildListeningNarrativeUseCase
import com.catlytics.core.model.ListeningNarrative
import com.catlytics.core.model.ListeningNarrativeKind
import com.catlytics.core.model.ListeningStreak
import com.catlytics.core.model.ListeningTotals
import com.catlytics.feature.statistics.impl.R as StatsR
import com.catlytics.feature.statistics.impl.formatListeningDuration
import com.catlytics.feature.statistics.impl.formatPlayCountLabel

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
    val gradient = rememberNameSeededGradient(seed)

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
            StatsArtwork(
                uri = artworkUri,
                seed = seed,
                placeholderIcon = if (narrative.topArtist != null) {
                    DesignSystemR.drawable.ic_artist
                } else {
                    DesignSystemR.drawable.ic_audio
                },
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
                            value = stringResource(
                                DesignSystemR.string.ds_metadata_pair,
                                track.title.displayTrackTitle(),
                                formatPlayCountLabel(track.playCount),
                            ),
                        )
                    }
                    narrative.topArtist?.let { artist ->
                        NarrativeStatRow(
                            label = stringResource(StatsR.string.stats_narrative_top_artist_label),
                            value = stringResource(
                                DesignSystemR.string.ds_metadata_pair,
                                artist.name.displayArtistName(),
                                formatListeningDuration(artist.totalListenedMillis),
                            ),
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
                value = (narrative.topArtist?.name?.displayArtistName()
                    ?: narrative.topTrack?.title?.displayTrackTitle()).orEmpty(),
            )
        }
        ListeningNarrativeKind.FavoriteTrack -> {
            NarrativeHeadlineText(
                label = stringResource(StatsR.string.stats_narrative_favorite_track),
                value = narrative.topTrack?.title?.displayTrackTitle().orEmpty(),
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

private const val HERO_GRADIENT_BLEND = 0.35f
private const val PROGRESS_FILL_MILLIS = 700
