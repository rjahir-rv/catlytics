package com.catlytics.app.playback

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import com.catlytics.core.designsystem.R
import com.catlytics.core.model.Track
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PlaybackQueueBottomSheet(
    queue: List<Track>,
    currentTrackId: String?,
    onDismiss: () -> Unit,
    onPlayQueueItem: (Int) -> Unit,
    onMoveQueueItem: (Int, Int) -> Unit,
    onAddToPlaylist: (Track) -> Unit,
    modifier: Modifier = Modifier,
) {
    var visibleQueue by remember { mutableStateOf(queue) }
    var draggedTrackId by remember { mutableStateOf<String?>(null) }
    var originalIndex by remember { mutableIntStateOf(-1) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val itemHeightPx = with(LocalDensity.current) { QueueItemHeight.toPx() }
    val animatedDragOffset by animateFloatAsState(
        targetValue = dragOffset,
        animationSpec = if (draggedTrackId != null) {
            snap()
        } else {
            spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow,
            )
        },
        label = "queueDragOffset",
    )

    LaunchedEffect(queue) {
        visibleQueue = queue
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
    ) {
        Text(
            text = "Cola de reproducción",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
        )

        LazyColumn {
            items(
                items = visibleQueue,
                key = Track::id,
            ) { track ->
                val isDragging = track.id == draggedTrackId
                val placementModifier = if (isDragging) {
                    Modifier
                } else {
                    Modifier.animateItem(
                        fadeInSpec = null,
                        fadeOutSpec = null,
                        placementSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMediumLow,
                        ),
                    )
                }

                QueueTrackRow(
                    track = track,
                    isCurrent = track.id == currentTrackId,
                    isDragging = isDragging,
                    onClick = {
                        visibleQueue.indexOfFirst { it.id == track.id }
                            .takeIf { it >= 0 }
                            ?.let(onPlayQueueItem)
                    },
                    modifier = placementModifier
                        .zIndex(if (isDragging) 1f else 0f)
                        .graphicsLayer {
                            translationY = if (isDragging) animatedDragOffset else 0f
                        },
                    dragModifier = Modifier.pointerInput(track.id, itemHeightPx) {
                        detectVerticalDragGestures(
                            onDragStart = {
                                draggedTrackId = track.id
                                originalIndex = visibleQueue.indexOfFirst { it.id == track.id }
                                dragOffset = 0f
                            },
                            onVerticalDrag = { change, amount ->
                                change.consume()
                                dragOffset += amount
                                if (abs(dragOffset) < itemHeightPx) return@detectVerticalDragGestures

                                val fromIndex = visibleQueue.indexOfFirst { it.id == track.id }
                                val direction = if (dragOffset > 0f) 1 else -1
                                val toIndex = (fromIndex + direction).coerceIn(visibleQueue.indices)
                                if (fromIndex >= 0 && fromIndex != toIndex) {
                                    visibleQueue = visibleQueue.moved(fromIndex, toIndex)
                                    dragOffset -= itemHeightPx * direction
                                }
                            },
                            onDragEnd = {
                                val finalIndex = visibleQueue.indexOfFirst { it.id == track.id }
                                if (originalIndex >= 0 && finalIndex >= 0 && originalIndex != finalIndex) {
                                    onMoveQueueItem(originalIndex, finalIndex)
                                }
                                draggedTrackId = null
                                originalIndex = -1
                                dragOffset = 0f
                            },
                            onDragCancel = {
                                visibleQueue = queue
                                draggedTrackId = null
                                originalIndex = -1
                                dragOffset = 0f
                            },
                        )
                    },
                    onAddToPlaylist = { onAddToPlaylist(track) },
                )
            }
        }
    }
}

@Composable
private fun QueueTrackRow(
    track: Track,
    isCurrent: Boolean,
    isDragging: Boolean,
    onClick: () -> Unit,
    dragModifier: Modifier,
    onAddToPlaylist: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scale by animateFloatAsState(
        targetValue = if (isDragging) 1.02f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "queueItemScale",
    )
    val backgroundColor by animateColorAsState(
        targetValue = when {
            isDragging -> MaterialTheme.colorScheme.surfaceContainerHighest
            isCurrent -> MaterialTheme.colorScheme.secondaryContainer
            else -> MaterialTheme.colorScheme.surfaceContainerLow
        },
        label = "queueItemBackground",
    )
    val shape = RoundedCornerShape(20.dp)
    val dragElevation = with(LocalDensity.current) { 12.dp.toPx() }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(QueueItemHeight)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                shadowElevation = if (isDragging) dragElevation else 0f
                this.shape = shape
                clip = isDragging
            }
            .background(backgroundColor, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = track.artworkUri,
            contentDescription = null,
            placeholder = painterResource(id = R.drawable.placeholder_album),
            error = painterResource(id = R.drawable.placeholder_album),
            fallback = painterResource(id = R.drawable.placeholder_album),
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(48.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = track.artist.name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .clickable(onClick = onAddToPlaylist),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_options),
                contentDescription = "Agregar ${track.title} a playlist",
            )
        }
        Box(
            modifier = dragModifier
                .size(48.dp)
                .clip(CircleShape)
                .background(
                    if (isDragging) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHighest
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_item_selection),
                contentDescription = "Reordenar ${track.title}",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private val QueueItemHeight = 80.dp

private fun <T> List<T>.moved(fromIndex: Int, toIndex: Int): List<T> =
    toMutableList().apply {
        add(toIndex, removeAt(fromIndex))
    }
