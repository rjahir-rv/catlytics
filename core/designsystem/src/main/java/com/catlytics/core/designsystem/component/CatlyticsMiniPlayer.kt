package com.catlytics.core.designsystem.component

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.catlytics.core.designsystem.R
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun CatlyticsMiniPlayer(
    title: String,
    artist: String,
    isPlaying: Boolean,
    isBuffering: Boolean,
    positionMillis: Long,
    durationMillis: Long,
    onTogglePlayback: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSkipNext: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    artwork: @Composable (Modifier) -> Unit = { artworkModifier ->
        Image(
            painter = painterResource(id = R.drawable.placeholder_album),
            contentDescription = null,
            modifier = artworkModifier,
            contentScale = ContentScale.Crop,
        )
    },
) {
    val containerShape = RoundedCornerShape(24.dp)
    val positionText = remember(positionMillis / MILLIS_PER_SECOND) {
        positionMillis.formatDuration()
    }
    val durationText = remember(durationMillis) {
        durationMillis.formatDuration()
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(containerShape)
            .animateContentSize(),
        onClick = onClick,
        shape = containerShape,
        color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.85f),
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column {
            LinearProgressIndicator(
                progress = {
                    if (durationMillis > 0L) {
                        (positionMillis.toFloat() / durationMillis).coerceIn(0f, 1f)
                    } else {
                        0f
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                trackColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.85f),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(MaterialTheme.shapes.small),
                ) {
                    artwork(Modifier.matchParentSize())
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "$artist - $positionText / $durationText",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.70f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Row(
                    modifier = Modifier.widthIn(min = 144.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onSkipPrevious) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_skip_back),
                            contentDescription = "Anterior",
                        )
                    }
                    IconButton(onClick = onTogglePlayback) {
                        Icon(
                            painter = if (isPlaying || isBuffering) {
                                painterResource(R.drawable.ic_pause)
                            } else {
                                painterResource(id = R.drawable.ic_play)
                            },
                            contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                        )
                    }
                    IconButton(onClick = onSkipNext) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_skip_next),
                            contentDescription = "Siguiente",
                        )
                    }
                }
            }
        }
    }
}

private const val MILLIS_PER_SECOND = 1_000L

private fun Long.formatDuration(): String {
    val duration = milliseconds
    val totalSeconds = duration.inWholeSeconds
    val hours = totalSeconds / 3_600
    val minutes = (totalSeconds % 3_600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%d:%02d", minutes, seconds)
    }
}
