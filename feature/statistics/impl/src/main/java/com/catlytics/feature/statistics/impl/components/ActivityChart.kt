package com.catlytics.feature.statistics.impl.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.catlytics.core.model.DailyListeningStat
import com.catlytics.feature.statistics.impl.R as StatsR

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

private const val CHART_ENTRANCE_MILLIS = 600
