package com.catlytics.feature.home.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.catlytics.core.designsystem.R
import com.catlytics.core.designsystem.modifier.pressScale
import com.catlytics.core.designsystem.modifier.staggeredEntrance
import com.catlytics.feature.home.impl.R as HomeR

private val QUICK_ACTION_CARD_HEIGHT = 116.dp
@Composable
internal fun HomeQuickActions(
    dailyPlaylistTrackCount: Int,
    canShuffleAll: Boolean,
    favoriteTrackCount: Int,
    recentlyAddedTrackCount: Int,
    onOpenDailyPlaylist: () -> Unit,
    onShuffleAll: () -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenRecentlyAdded: () -> Unit,
    animateEntrance: Boolean,
    modifier: Modifier = Modifier,
) {
    var order = 0
    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(end = 4.dp),
        ) {
            if (recentlyAddedTrackCount > 0) {
                val index = order++
                item(key = "recently-added") {
                    HomeQuickActionCard(
                        modifier = Modifier.staggeredEntrance(
                            index = index,
                            animate = animateEntrance && index < ENTRANCE_MAX_STAGGERED_ITEMS,
                        ),
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
                val index = order++
                item(key = "daily-playlist") {
                    HomeQuickActionCard(
                        modifier = Modifier.staggeredEntrance(
                            index = index,
                            animate = animateEntrance && index < ENTRANCE_MAX_STAGGERED_ITEMS,
                        ),
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
                val index = order++
                item(key = "shuffle-all") {
                    HomeQuickActionCard(
                        modifier = Modifier.staggeredEntrance(
                            index = index,
                            animate = animateEntrance && index < ENTRANCE_MAX_STAGGERED_ITEMS,
                        ),
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
                val index = order++
                item(key = "favorites") {
                    HomeQuickActionCard(
                        modifier = Modifier.staggeredEntrance(
                            index = index,
                            animate = animateEntrance && index < ENTRANCE_MAX_STAGGERED_ITEMS,
                        ),
                        title = stringResource(HomeR.string.home_quick_action_favorites_title),
                        subtitle = pluralStringResource(
                            R.plurals.ds_track_count,
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
    val interactionSource = remember { MutableInteractionSource() }
    Card(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = modifier
            .pressScale(interactionSource)
            .width(168.dp)
            .height(QUICK_ACTION_CARD_HEIGHT)
            .semantics { this.contentDescription = contentDescription },
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent,
            contentColor = contentColor,
        ),
        shape = HomeDimens.CardShape,
        border = BorderStroke(0.5.dp, contentColor.copy(alpha = 0.12f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            containerColor,
                            lerp(containerColor, gradientTarget, 0.22f),
                        ),
                    ),
                )
                .padding(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(contentColor.copy(alpha = 0.14f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(modifier = Modifier.weight(1f))
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
