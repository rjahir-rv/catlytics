package com.catlytics.feature.home.impl

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.catlytics.core.model.Track
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds

@Composable
internal fun TrackRow(
    track: Track,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onTrackSelected: () -> Unit,
    onTrackOptions: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clickable(onClick = onTrackSelected)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TrackArtwork(track = track, isCurrent = isCurrent, isPlaying = isPlaying)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.titleMedium,
                color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${track.artist.name} · ${track.durationMillis.formatDuration()}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(onClick = onTrackOptions) {
            Icon(
                painter = painterResource(R.drawable.ic_options),
                contentDescription = "Opciones de ${track.title}",
            )
        }
    }
}

@Composable
private fun TrackArtwork(
    track: Track,
    isCurrent: Boolean,
    isPlaying: Boolean,
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
                model = track.artworkUri,
                contentDescription = null,
                placeholder = painterResource(R.drawable.placeholder_album),
                error = painterResource(R.drawable.placeholder_album),
                fallback = painterResource(R.drawable.placeholder_album),
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
            if (isPlaying) {
                PlayingBarsOverlay(
                    trackTitle = track.title,
                    modifier = Modifier.matchParentSize(),
                )
            }
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

private fun Long.formatDuration(): String {
    val totalSeconds = milliseconds.inWholeSeconds
    val hours = totalSeconds / 3_600
    val minutes = (totalSeconds % 3_600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%d:%02d", minutes, seconds)
    }
}
