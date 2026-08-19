package com.catlytics.core.designsystem.component

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.catlytics.core.designsystem.R

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CatlyticsTrackRow(
    title: String,
    subtitle: String,
    artworkUri: String?,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    trailing: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    clickEnabled: Boolean = true,
    selected: Boolean = false,
    selectionActive: Boolean = false,
    onLongClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (selected) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                } else {
                    Color.Transparent
                },
            )
            .combinedClickable(
                enabled = clickEnabled || onLongClick != null,
                onClick = { if (clickEnabled) onClick() },
                onLongClick = onLongClick,
            )
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TrackArtwork(
            title = title,
            artworkUri = artworkUri,
            isCurrent = isCurrent,
            isPlaying = isPlaying && !selectionActive,
            selected = selected,
            selectionActive = selectionActive,
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = if (isCurrent && !selectionActive) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onBackground
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (!selectionActive) {
            trailing()
        }
    }
}

@Composable
private fun TrackArtwork(
    title: String,
    artworkUri: String?,
    isCurrent: Boolean,
    isPlaying: Boolean,
    selected: Boolean = false,
    selectionActive: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val artworkShape = RoundedCornerShape(10.dp)
    Box(
        modifier = modifier.size(56.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (isCurrent) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .blur(radius = 16.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.65f),
                        shape = artworkShape,
                    ),
            )
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .blur(radius = 8.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                        shape = artworkShape,
                    ),
            )
        }
        Box(modifier = Modifier.size(48.dp).clip(artworkShape)) {
            AsyncImage(
                model = artworkUri,
                contentDescription = null,
                placeholder = painterResource(R.drawable.placeholder_track),
                error = painterResource(R.drawable.placeholder_track),
                fallback = painterResource(R.drawable.placeholder_track),
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
            if (isPlaying) {
                PlayingBarsOverlay(
                    trackTitle = title,
                    modifier = Modifier.matchParentSize(),
                )
            }
            if (selectionActive) {
                TrackSelectionMark(selected = selected)
            }
        }
    }
}

@Composable
fun TrackSelectionMark(
    selected: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                if (selected) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.72f)
                } else {
                    Color.Black.copy(alpha = 0.28f)
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = "Seleccionada",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .size(22.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                    .padding(3.dp),
            )
        }
    }
}

@Composable
private fun PlayingBarsOverlay(
    trackTitle: String,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "playing bars")
    val barHeights = listOf(0, 150, 300).mapIndexed { index, delayMillis ->
        transition.animateFloat(
            initialValue = if (index == 1) 0.35f else 0.75f,
            targetValue = if (index == 1) 0.9f else 0.3f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 520, delayMillis = delayMillis),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "playing bar $index",
        ).value
    }
    Box(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.38f))
            .semantics { contentDescription = "Reproduciendo $trackTitle" },
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier.height(24.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            barHeights.forEach { heightFraction ->
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height((24.dp * heightFraction).coerceAtLeast(7.dp))
                        .background(color = Color.White, shape = RoundedCornerShape(2.dp)),
                )
            }
        }
    }
}
