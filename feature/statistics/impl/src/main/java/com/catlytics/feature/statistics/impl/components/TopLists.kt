package com.catlytics.feature.statistics.impl.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.catlytics.core.designsystem.R as DesignSystemR
import com.catlytics.core.designsystem.component.CatlyticsArtwork
import com.catlytics.core.designsystem.text.displayAlbumTitle
import com.catlytics.core.designsystem.text.displayArtistName
import com.catlytics.core.designsystem.text.displayTrackTitle
import com.catlytics.core.designsystem.theme.CatlyticsCorners
import com.catlytics.core.model.TopAlbum
import com.catlytics.core.model.TopArtist
import com.catlytics.core.model.TopTrack
import com.catlytics.feature.statistics.impl.formatListeningDuration
import com.catlytics.feature.statistics.impl.formatPlayCountLabel

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
    RankedRow(
        rank = rank,
        title = track.title.displayTrackTitle(),
        subtitle = track.artistName.displayArtistName(),
        value = formatPlayCountLabel(track.playCount),
        artworkUri = track.artworkUri,
        seed = track.title,
        placeholderIcon = DesignSystemR.drawable.ic_audio,
        circular = false,
        showDivider = showDivider,
        modifier = modifier,
    )
}

@Composable
internal fun TopArtistItem(
    rank: Int,
    artist: TopArtist,
    modifier: Modifier = Modifier,
    showDivider: Boolean = false,
) {
    RankedRow(
        rank = rank,
        title = artist.name.displayArtistName(),
        subtitle = formatListeningDuration(artist.totalListenedMillis),
        value = formatPlayCountLabel(artist.playCount),
        artworkUri = artist.artworkUri,
        seed = artist.name,
        placeholderIcon = DesignSystemR.drawable.ic_artist,
        circular = true,
        showDivider = showDivider,
        modifier = modifier,
    )
}

@Composable
internal fun TopAlbumItem(
    rank: Int,
    album: TopAlbum,
    modifier: Modifier = Modifier,
    showDivider: Boolean = false,
) {
    RankedRow(
        rank = rank,
        title = album.title.displayAlbumTitle(),
        subtitle = album.artistName.displayArtistName(),
        value = formatListeningDuration(album.totalListenedMillis),
        artworkUri = album.artworkUri,
        seed = album.title,
        placeholderIcon = DesignSystemR.drawable.ic_album,
        circular = false,
        showDivider = showDivider,
        modifier = modifier,
    )
}

/** Fila de un ranking: posición, carátula, título, subtítulo y el valor a la derecha. */
@Composable
private fun RankedRow(
    rank: Int,
    title: String,
    subtitle: String,
    value: String,
    artworkUri: String?,
    seed: String,
    @DrawableRes placeholderIcon: Int,
    circular: Boolean,
    showDivider: Boolean,
    modifier: Modifier = Modifier,
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
            StatsArtwork(
                uri = artworkUri,
                seed = seed,
                placeholderIcon = placeholderIcon,
                circular = circular,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = value,
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
internal fun StatsArtwork(
    uri: String?,
    seed: String,
    @DrawableRes placeholderIcon: Int,
    circular: Boolean,
    size: Dp = 48.dp,
    contentDescription: String? = null,
) {
    CatlyticsArtwork(
        model = uri,
        seed = seed,
        placeholderIcon = placeholderIcon,
        contentDescription = contentDescription,
        modifier = Modifier
            .size(size)
            .clip(if (circular) CircleShape else CatlyticsCorners.Small),
    )
}
