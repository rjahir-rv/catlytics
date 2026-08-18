package com.catlytics.feature.home.impl

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.catlytics.core.designsystem.R
import com.catlytics.core.designsystem.component.CatlyticsTrackRow
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
    CatlyticsTrackRow(
        title = track.title,
        subtitle = "${track.artist.name} · ${track.durationMillis.formatDuration()}",
        artworkUri = track.artworkUri,
        isCurrent = isCurrent,
        isPlaying = isPlaying,
        onClick = onTrackSelected,
        trailing = {
            IconButton(onClick = onTrackOptions) {
                Icon(
                    painter = painterResource(R.drawable.ic_options),
                    contentDescription = "Opciones de ${track.title}",
                )
            }
        },
        modifier = modifier,
    )
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
