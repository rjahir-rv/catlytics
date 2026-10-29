package com.catlytics.feature.home.impl

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.catlytics.core.designsystem.R
import com.catlytics.core.designsystem.component.CatlyticsLetterFastScroller
import com.catlytics.core.designsystem.component.SearchCollapsibleHeader
import com.catlytics.core.designsystem.component.TrackListRow
import com.catlytics.core.designsystem.component.sectionLetter
import com.catlytics.core.designsystem.layout.CenteredListContainer
import com.catlytics.core.designsystem.layout.plusHorizontal
import com.catlytics.core.model.TopTrack
import com.catlytics.core.model.Track
import kotlinx.coroutines.delay
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
    isSearching: Boolean,
    areFeaturedSectionsVisible: Boolean,
    onToggleFeaturedSections: () -> Unit,
) {
    var entranceDone by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(ENTRANCE_SETTLE_MILLIS)
        entranceDone = true
    }
    CenteredListContainer(modifier = modifier.fillMaxSize()) { horizontalInset ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = state,
            contentPadding = contentPadding.plusHorizontal(horizontalInset),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            item(key = "home-header") {
                SearchCollapsibleHeader(isSearching = isSearching) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        FeaturedSectionsHeader(
                            areFeaturedSectionsVisible = areFeaturedSectionsVisible,
                            onToggleFeaturedSections = onToggleFeaturedSections,
                            modifier = Modifier.padding(horizontal = HomeDimens.ScreenPadding),
                        )
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
                                    .padding(horizontal = HomeDimens.ScreenPadding),
                                verticalArrangement = Arrangement.spacedBy(HomeDimens.SectionSpacing),
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
                                        animateEntrance = !entranceDone,
                                    )
                                }
                                HomeHighlights(
                                    recentlyPlayedTracks = recentlyPlayedTracks,
                                    topTracks = topTracks,
                                    onRecentlyPlayedTrackSelected = onRecentlyPlayedTrackSelected,
                                    onTopTrackSelected = onTopTrackSelected,
                                    onNavigateToStatistics = onNavigateToStatistics,
                                    animateEntrance = !entranceDone,
                                    modifier = Modifier.padding(bottom = 8.dp),
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.padding(
                                start = HomeDimens.ScreenPadding,
                                top = 8.dp,
                                end = HomeDimens.ScreenPadding,
                                bottom = 4.dp,
                            ),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Bottom,
                        ) {
                            Text(
                                text = stringResource(HomeR.string.home_all_tracks_title),
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                text = pluralStringResource(
                                    R.plurals.ds_track_count,
                                    tracks.size,
                                    tracks.size,
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 2.dp),
                            )
                        }
                    }
                }
            }
            items(items = tracks, key = Track::id, contentType = { "track" }) { track ->
                TrackListRow(
                    track = track,
                    isCurrent = track.id == currentTrackId,
                    isPlaying = track.id == currentTrackId && isCurrentTrackPlaying,
                    badgeLabel = if (track.id in newTrackIds) {
                        stringResource(HomeR.string.home_new_track_badge)
                    } else {
                        null
                    },
                    onClick = {
                        if (selectionActive) {
                            onTrackLongClick(track)
                        } else {
                            onTrackSelected(track, playbackQueue)
                        }
                    },
                    onOptions = { onTrackOptions(track) },
                    modifier = Modifier
                        .animateItem()
                        .padding(horizontal = HomeDimens.ScreenPadding),
                    selected = track.id in selectedTrackIds,
                    selectionActive = selectionActive,
                    onLongClick = { onTrackLongClick(track) },
                )
            }
        }
        CatlyticsLetterFastScroller(
            listState = state,
            itemCount = tracks.size,
            headerItemCount = HEADER_ITEM_COUNT,
            letterForVisibleTrackIndex = { index -> tracks[index].title.sectionLetter() },
            contentPadding = contentPadding,
        )
    }
}

private const val HEADER_ITEM_COUNT = 1
private const val ENTRANCE_SETTLE_MILLIS = 900L
internal const val ENTRANCE_MAX_STAGGERED_ITEMS = 4

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
            Crossfade(
                targetState = areFeaturedSectionsVisible,
                animationSpec = tween(durationMillis = 180),
                label = "featuredSectionsToggle",
            ) { visible ->
                Icon(
                    painter = painterResource(if (visible) R.drawable.ic_hide else R.drawable.ic_show),
                    contentDescription = if (visible) {
                        stringResource(HomeR.string.home_toggle_featured_sections_hide_content_description)
                    } else {
                        stringResource(HomeR.string.home_toggle_featured_sections_show_content_description)
                    },
                )
            }
        }
    }
}
