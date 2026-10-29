package com.catlytics.feature.home.impl

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.catlytics.core.designsystem.R
import com.catlytics.core.designsystem.component.CatlyticsArtwork
import com.catlytics.core.designsystem.modifier.pressScale
import com.catlytics.core.designsystem.modifier.staggeredEntrance
import com.catlytics.core.designsystem.text.displayArtistName
import com.catlytics.core.designsystem.text.displayTrackTitle
import com.catlytics.core.model.TopTrack
import com.catlytics.core.model.Track
import com.catlytics.feature.home.impl.R as HomeR

@Composable
internal fun HomeHighlights(
    recentlyPlayedTracks: List<Track>,
    topTracks: List<TopTrack>,
    onRecentlyPlayedTrackSelected: (Track) -> Unit,
    onTopTrackSelected: (String) -> Unit,
    onNavigateToStatistics: () -> Unit,
    animateEntrance: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(HomeDimens.SectionSpacing),
    ) {
        if (recentlyPlayedTracks.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(HomeR.string.home_recently_played_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(end = 4.dp),
                ) {
                    itemsIndexed(items = recentlyPlayedTracks, key = { _, track -> track.id }) { index, track ->
                        RecentlyPlayedTrackCard(
                            modifier = Modifier.staggeredEntrance(
                                index = index,
                                animate = animateEntrance && index < ENTRANCE_MAX_STAGGERED_ITEMS,
                            ),
                            track = track,
                            onClick = { onRecentlyPlayedTrackSelected(track) },
                        )
                    }
                }
            }
        }

        if (topTracks.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            text = stringResource(HomeR.string.home_top_tracks_title),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = stringResource(HomeR.string.home_top_tracks_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    TextButton(onClick = onNavigateToStatistics) {
                        Text(text = stringResource(HomeR.string.home_open_statistics))
                    }
                }
                topTracks.forEachIndexed { index, track ->
                    TopTrackRow(
                        rank = index + 1,
                        track = track,
                        onClick = { onTopTrackSelected(track.trackId) },
                    )
                }
            }
        }

        if (recentlyPlayedTracks.isEmpty() && topTracks.isEmpty()) {
            Text(
                text = stringResource(HomeR.string.home_activity_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (topTracks.isEmpty()) {
            TextButton(
                onClick = onNavigateToStatistics,
                modifier = Modifier.align(Alignment.End),
            ) {
                Text(text = stringResource(HomeR.string.home_open_statistics))
            }
        }
    }
}

@Composable
private fun RecentlyPlayedTrackCard(
    track: Track,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Card(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = modifier
            .pressScale(interactionSource)
            .width(128.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = HomeDimens.CardShape,
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            CatlyticsArtwork(
                model = track.artworkUri,
                seed = track.title,
                placeholderIcon = R.drawable.ic_audio,
                contentDescription = stringResource(
                    HomeR.string.home_play_recently_played_content_description,
                    track.title.displayTrackTitle(),
                ),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(104.dp)
                    .clip(HomeDimens.ThumbShape),
            )
            Text(
                text = track.title.displayTrackTitle(),
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                text = track.artist.name.displayArtistName(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun TopTrackRow(
    rank: Int,
    track: TopTrack,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val topTrackContentDescription = stringResource(
        HomeR.string.home_play_top_track_content_description,
        track.title.displayTrackTitle(),
    )
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .pressScale(interactionSource)
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                role = Role.Button,
                onClick = onClick,
            )
            .semantics {
                contentDescription = topTrackContentDescription
            }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = rank.toString(),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.width(20.dp),
            textAlign = TextAlign.Center,
        )
        CatlyticsArtwork(
            model = track.artworkUri,
            seed = track.title,
            placeholderIcon = R.drawable.ic_audio,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(44.dp)
                .clip(HomeDimens.ThumbShape),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title.displayTrackTitle(),
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = track.artistName.displayArtistName(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = pluralStringResource(
                R.plurals.ds_play_count,
                track.playCount,
                track.playCount,
            ),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
