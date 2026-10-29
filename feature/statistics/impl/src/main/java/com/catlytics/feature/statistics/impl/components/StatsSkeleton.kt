package com.catlytics.feature.statistics.impl.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.catlytics.core.designsystem.theme.CatlyticsCorners

@Composable
internal fun StatsSkeleton(
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "statsSkeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(SKELETON_PULSE_MILLIS), RepeatMode.Reverse),
        label = "statsSkeletonAlpha",
    )
    val block = MaterialTheme.colorScheme.surfaceContainerHigh
    Column(
        modifier = modifier
            .padding(contentPadding)
            .graphicsLayer { this.alpha = alpha },
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(140.dp)
                .clip(CatlyticsCorners.Large)
                .background(block),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(3) {
                Box(
                    Modifier
                        .weight(1f)
                        .height(64.dp)
                        .clip(CatlyticsCorners.Medium)
                        .background(block),
                )
            }
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(88.dp)
                .clip(CatlyticsCorners.Large)
                .background(block),
        )
        Box(
            Modifier
                .fillMaxWidth()
                .height(240.dp)
                .clip(CatlyticsCorners.Large)
                .background(block),
        )
    }
}

private const val SKELETON_PULSE_MILLIS = 800
