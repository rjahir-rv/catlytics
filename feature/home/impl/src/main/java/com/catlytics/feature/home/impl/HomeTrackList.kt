package com.catlytics.feature.home.impl

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.catlytics.core.designsystem.R
import com.catlytics.core.designsystem.component.CatlyticsLetterFastScroller
import com.catlytics.core.designsystem.component.sectionLetter
import com.catlytics.core.model.TopTrack
import com.catlytics.core.model.Track
import com.catlytics.feature.home.impl.R as HomeR

@Composable
internal fun HomeTrackList(
    tracks: List<Track>,
    playbackQueue: List<Track>,
    dailyPlaylistTrackCount: Int,
    canShuffleAll: Boolean,
    favoriteTrackCount: Int,
    recentlyAddedTrackCount: Int,
    newTrackIds: Set<String>,
    showRecommendedPlaylists: Boolean,
    recentlyPlayedTracks: List<Track>,
    topTracks: List<TopTrack>,
    currentTrackId: String?,
    isCurrentTrackPlaying: Boolean,
    onTrackSelected: (Track, List<Track>) -> Unit,
    onOpenDailyPlaylist: () -> Unit,
    onShuffleAll: () -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenRecentlyAdded: () -> Unit,
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onTrackOptions: (Track) -> Unit,
    selectedTrackIds: Set<String> = emptySet(),
    selectionActive: Boolean = false,
    onTrackLongClick: (Track) -> Unit = {},
    onRecentlyPlayedTrackSelected: (Track) -> Unit,
    onTopTrackSelected: (String) -> Unit,
    onNavigateToStatistics: () -> Unit,
    showHighlights: Boolean,
    areFeaturedSectionsVisible: Boolean,
    onToggleFeaturedSections: () -> Unit,
) {
    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = state,
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (showHighlights) {
                item(key = "featured-sections-header") {
                    FeaturedSectionsHeader(
                        areFeaturedSectionsVisible = areFeaturedSectionsVisible,
                        onToggleFeaturedSections = onToggleFeaturedSections,
                        modifier = Modifier.padding(horizontal = 20.dp),
                    )
                }
                item(key = "featured-sections-content") {
                    AnimatedVisibility(
                        visible = areFeaturedSectionsVisible,
                        enter = expandVertically(
                            animationSpec = tween(durationMillis = 260),
                            expandFrom = Alignment.Top,
                        ) + fadeIn(animationSpec = tween(durationMillis = 180)),
                        exit = shrinkVertically(
                            animationSpec = tween(durationMillis = 240),
                            shrinkTowards = Alignment.Top,
                        ) + fadeOut(animationSpec = tween(durationMillis = 160)),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            if (
                                showRecommendedPlaylists &&
                                (
                                    recentlyAddedTrackCount > 0 ||
                                        dailyPlaylistTrackCount > 0 ||
                                        canShuffleAll ||
                                        favoriteTrackCount > 0
                                    )
                            ) {
                                HomeQuickActions(
                                    dailyPlaylistTrackCount = dailyPlaylistTrackCount,
                                    canShuffleAll = canShuffleAll,
                                    favoriteTrackCount = favoriteTrackCount,
                                    recentlyAddedTrackCount = recentlyAddedTrackCount,
                                    onOpenDailyPlaylist = onOpenDailyPlaylist,
                                    onShuffleAll = onShuffleAll,
                                    onOpenFavorites = onOpenFavorites,
                                    onOpenRecentlyAdded = onOpenRecentlyAdded,
                                )
                            }
                            HomeHighlights(
                                recentlyPlayedTracks = recentlyPlayedTracks,
                                topTracks = topTracks,
                                onRecentlyPlayedTrackSelected = onRecentlyPlayedTrackSelected,
                                onTopTrackSelected = onTopTrackSelected,
                                onNavigateToStatistics = onNavigateToStatistics,
                                modifier = Modifier.padding(bottom = 8.dp),
                            )
                        }
                    }
                }
                item {
                    Text(
                        text = stringResource(HomeR.string.home_all_tracks_title),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(
                            start = 20.dp,
                            top = 8.dp,
                            end = 20.dp,
                            bottom = 4.dp,
                        ),
                    )
                }
            }
            items(items = tracks, key = Track::id) { track ->
                TrackRow(
                    track = track,
                    isCurrent = track.id == currentTrackId,
                    isPlaying = track.id == currentTrackId && isCurrentTrackPlaying,
                    isNew = track.id in newTrackIds,
                    onTrackSelected = {
                        if (selectionActive) {
                            onTrackLongClick(track)
                        } else {
                            onTrackSelected(track, playbackQueue)
                        }
                    },
                    onTrackOptions = { onTrackOptions(track) },
                    modifier = Modifier.padding(horizontal = 20.dp),
                    selected = track.id in selectedTrackIds,
                    selectionActive = selectionActive,
                    onLongClick = { onTrackLongClick(track) },
                )
            }
        }
        CatlyticsLetterFastScroller(
            listState = state,
            itemCount = tracks.size,
            headerItemCount = if (showHighlights) HIGHLIGHT_HEADER_ITEM_COUNT else 0,
            letterForVisibleTrackIndex = { index -> tracks[index].title.sectionLetter() },
            contentPadding = contentPadding,
        )
    }
}

private const val HIGHLIGHT_HEADER_ITEM_COUNT = 3

@Composable
private fun FeaturedSectionsHeader(
    areFeaturedSectionsVisible: Boolean,
    onToggleFeaturedSections: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(HomeR.string.home_quick_actions_title),
            style = MaterialTheme.typography.titleMedium,
        )
        IconButton(onClick = onToggleFeaturedSections) {
            Icon(
                painter = painterResource(
                    if (areFeaturedSectionsVisible) {
                        R.drawable.ic_hide
                    } else {
                        R.drawable.ic_show
                    },
                ),
                contentDescription = if (areFeaturedSectionsVisible) {
                    stringResource(HomeR.string.home_toggle_featured_sections_hide_content_description)
                } else {
                    stringResource(HomeR.string.home_toggle_featured_sections_show_content_description)
                },
            )
        }
    }
}

@Composable
private fun HomeQuickActions(
    dailyPlaylistTrackCount: Int,
    canShuffleAll: Boolean,
    favoriteTrackCount: Int,
    recentlyAddedTrackCount: Int,
    onOpenDailyPlaylist: () -> Unit,
    onShuffleAll: () -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenRecentlyAdded: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(end = 4.dp),
        ) {
            if (recentlyAddedTrackCount > 0) {
                item(key = "recently-added") {
                    HomeQuickActionCard(
                        title = stringResource(HomeR.string.home_quick_action_recently_added_title),
                        subtitle = pluralStringResource(
                            HomeR.plurals.home_quick_action_recently_added_subtitle,
                            recentlyAddedTrackCount,
                            recentlyAddedTrackCount,
                        ),
                        icon = R.drawable.ic_recently_added,
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        gradientTarget = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        contentDescription = stringResource(
                            HomeR.string.home_quick_action_recently_added_content_description,
                        ),
                        onClick = onOpenRecentlyAdded,
                    )
                }
            }
            if (dailyPlaylistTrackCount > 0) {
                item(key = "daily-playlist") {
                    HomeQuickActionCard(
                        title = stringResource(HomeR.string.home_quick_action_daily_playlist_title),
                        subtitle = pluralStringResource(
                            HomeR.plurals.home_quick_action_daily_playlist_subtitle,
                            dailyPlaylistTrackCount,
                            dailyPlaylistTrackCount,
                        ),
                        icon = R.drawable.ic_playlist,
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        gradientTarget = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        contentDescription = stringResource(
                            HomeR.string.home_quick_action_daily_playlist_content_description,
                        ),
                        onClick = onOpenDailyPlaylist,
                    )
                }
            }
            if (canShuffleAll) {
                item(key = "shuffle-all") {
                    HomeQuickActionCard(
                        title = stringResource(HomeR.string.home_quick_action_shuffle_title),
                        subtitle = stringResource(HomeR.string.home_quick_action_shuffle_subtitle),
                        icon = R.drawable.ic_shuffle_square,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        gradientTarget = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        contentDescription = stringResource(
                            HomeR.string.home_quick_action_shuffle_content_description,
                        ),
                        onClick = onShuffleAll,
                    )
                }
            }
            if (favoriteTrackCount > 0) {
                item(key = "favorites") {
                    HomeQuickActionCard(
                        title = stringResource(HomeR.string.home_quick_action_favorites_title),
                        subtitle = pluralStringResource(
                            HomeR.plurals.home_quick_action_favorites_subtitle,
                            favoriteTrackCount,
                            favoriteTrackCount,
                        ),
                        icon = R.drawable.ic_favorite_fill,
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        gradientTarget = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        contentDescription = stringResource(
                            HomeR.string.home_quick_action_favorites_content_description,
                        ),
                        onClick = onOpenFavorites,
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeQuickActionCard(
    title: String,
    subtitle: String,
    icon: Int,
    containerColor: Color,
    gradientTarget: Color,
    contentColor: Color,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .width(168.dp)
            .heightIn(min = 104.dp)
            .semantics { this.contentDescription = contentDescription },
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent,
            contentColor = contentColor,
        ),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            containerColor,
                            lerp(containerColor, gradientTarget, 0.22f),
                        ),
                    ),
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier.size(28.dp),
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = contentColor.copy(alpha = 0.8f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun HomeHighlights(
    recentlyPlayedTracks: List<Track>,
    topTracks: List<TopTrack>,
    onRecentlyPlayedTrackSelected: (Track) -> Unit,
    onTopTrackSelected: (String) -> Unit,
    onNavigateToStatistics: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
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
                    items(items = recentlyPlayedTracks, key = Track::id) { track ->
                        RecentlyPlayedTrackCard(
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
    Card(
        onClick = onClick,
        modifier = modifier.width(128.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(14.dp),
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            AsyncImage(
                model = track.artworkUri,
                contentDescription = stringResource(
                    HomeR.string.home_play_recently_played_content_description,
                    track.title,
                ),
                placeholder = painterResource(R.drawable.placeholder_track),
                error = painterResource(R.drawable.placeholder_track),
                fallback = painterResource(R.drawable.placeholder_track),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(104.dp)
                    .clip(RoundedCornerShape(10.dp)),
            )
            Text(
                text = track.title,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                text = track.artist.name,
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
        track.title,
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(role = Role.Button, onClick = onClick)
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
        AsyncImage(
            model = track.artworkUri,
            contentDescription = null,
            placeholder = painterResource(R.drawable.placeholder_track),
            error = painterResource(R.drawable.placeholder_track),
            fallback = painterResource(R.drawable.placeholder_track),
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(10.dp)),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = track.artistName,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = "${track.playCount}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
