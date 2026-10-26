package com.catlytics.app.playback

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.combinedClickable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.catlytics.app.R as AppR
import com.catlytics.core.designsystem.R
import com.catlytics.core.designsystem.component.ArtworkGradientColors
import com.catlytics.core.designsystem.component.CatlyticsArtwork
import com.catlytics.core.designsystem.text.displayArtistName
import com.catlytics.core.designsystem.text.displayTrackTitle
import com.catlytics.core.model.Track

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun QueueSwipeableItem(
    track: Track,
    gradientColors: ArtworkGradientColors,
    enabled: Boolean,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        positionalThreshold = { totalDistance -> totalDistance * 0.45f },
    )

    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
            onRemove()
        }
    }
    val isSwipeToDeleteActive = dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart ||
        dismissState.targetValue == SwipeToDismissBoxValue.EndToStart ||
        dismissState.currentValue == SwipeToDismissBoxValue.EndToStart
    val swipeProgress = if (isSwipeToDeleteActive) dismissState.progress else 0f
    val deleteBackgroundColor = lerp(
        Color.Transparent,
        gradientColors.end.copy(alpha = 0.72f),
        swipeProgress,
    )
    val deleteIconProgress =
        ((swipeProgress - DeleteIconRevealProgress) / (1f - DeleteIconRevealProgress)).coerceIn(0f, 1f)
    val iconScale = 0.7f + (0.3f * deleteIconProgress)

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = enabled,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(QueueRowShape)
                    .background(deleteBackgroundColor)
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                if (deleteIconProgress > 0f) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_delete),
                        contentDescription = stringResource(
                            AppR.string.app_queue_remove_track,
                            track.title.displayTrackTitle(),
                        ),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.88f),
                        modifier = Modifier.graphicsLayer {
                            scaleX = iconScale
                            scaleY = iconScale
                            alpha = deleteIconProgress
                        },
                    )
                }
            }
        },
        content = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Transparent),
            ) {
                content()
            }
        },
    )
}

@Composable
internal fun QueueTrackRow(
    track: Track,
    isCurrent: Boolean,
    isPlaying: Boolean,
    isRepeatingOne: Boolean,
    accent: Color,
    contentAlpha: Float,
    isDragging: Boolean,
    liftProgress: Float,
    onClick: () -> Unit,
    dragModifier: Modifier,
    onTrackOptions: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentHighlight by animateColorAsState(
        targetValue = if (isCurrent) accent.copy(alpha = CurrentRowHighlightAlpha) else Color.Transparent,
        animationSpec = tween(durationMillis = 300),
        label = "queueCurrentHighlight",
    )
    val titleColor by animateColorAsState(
        targetValue = if (isCurrent) accent else MaterialTheme.colorScheme.onSurface,
        animationSpec = tween(durationMillis = 300),
        label = "queueTitleColor",
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(QueueItemHeight)
            .clip(QueueRowShape)
            .background(currentHighlight)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = liftProgress))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onTrackOptions,
                onLongClickLabel = stringResource(AppR.string.app_queue_track_options_hint),
            )
            .padding(start = 16.dp, end = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CatlyticsArtwork(
            model = track.artworkUri,
            seed = track.title,
            placeholderIcon = R.drawable.ic_audio,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(QueueArtworkSize)
                .graphicsLayer { alpha = contentAlpha }
                .clip(QueueArtworkShape),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .graphicsLayer { alpha = contentAlpha },
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (isCurrent) {
                    NowPlayingBars(isPlaying = isPlaying, color = accent)
                }
                Text(
                    text = track.title.displayTrackTitle(),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                    ),
                    color = titleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                AnimatedVisibility(
                    visible = isCurrent && isRepeatingOne,
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut(),
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_repeat_one),
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
            Text(
                text = if (isCurrent) {
                    stringResource(
                        AppR.string.app_queue_now_playing_track,
                        track.artist.name.displayArtistName(),
                    )
                } else {
                    track.artist.name.displayArtistName()
                },
                style = MaterialTheme.typography.bodyMedium,
                color = if (isCurrent) {
                    accent.copy(alpha = 0.8f)
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Box(
            modifier = dragModifier
                .size(QueueActionTouchTarget)
                .clip(CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_item_selection),
                contentDescription = stringResource(
                    AppR.string.app_content_description_reorder_track,
                    track.title.displayTrackTitle(),
                ),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                    alpha = if (isDragging) 1f else 0.72f,
                ),
                modifier = Modifier.size(QueueActionIconSize),
            )
        }
    }
}

private val QueueArtworkSize = 40.dp
private val QueueArtworkShape = RoundedCornerShape(10.dp)
private val QueueActionTouchTarget = 48.dp
private const val CurrentRowHighlightAlpha = 0.12f
private const val DeleteIconRevealProgress = 0.08f
