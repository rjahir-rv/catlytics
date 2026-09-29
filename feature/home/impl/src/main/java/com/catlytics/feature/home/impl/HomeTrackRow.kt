package com.catlytics.feature.home.impl

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.catlytics.core.designsystem.R
import com.catlytics.core.designsystem.component.CatlyticsTrackRow
import com.catlytics.core.designsystem.format.TrackDurationFormat
import com.catlytics.core.model.Track
import com.catlytics.feature.home.impl.R as HomeR

@Composable
internal fun TrackRow(
    track: Track,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onTrackSelected: () -> Unit,
    onTrackOptions: () -> Unit,
    modifier: Modifier = Modifier,
    isNew: Boolean = false,
    selected: Boolean = false,
    selectionActive: Boolean = false,
    onLongClick: (() -> Unit)? = null,
) {
    CatlyticsTrackRow(
        title = track.title,
        subtitle = "${track.artist.name} · ${TrackDurationFormat.format(track.durationMillis)}",
        artworkUri = track.artworkUri,
        isCurrent = isCurrent,
        isPlaying = isPlaying,
        onClick = onTrackSelected,
        trailing = {
            IconButton(onClick = onTrackOptions) {
                Icon(
                    painter = painterResource(R.drawable.ic_options),
                    contentDescription = stringResource(
                        HomeR.string.home_track_options_content_description,
                        track.title,
                    ),
                )
            }
        },
        modifier = modifier,
        badgeLabel = if (isNew) stringResource(HomeR.string.home_new_track_badge) else null,
        selected = selected,
        selectionActive = selectionActive,
        onLongClick = onLongClick,
    )
}
