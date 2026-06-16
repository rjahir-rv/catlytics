package com.catlytics.app.playback

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
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
    gradientColors: NowPlayingGradientColors,
    onDismiss: () -> Unit,
    onPlayQueueItem: (Int) -> Unit,
    onMoveQueueItem: (Int, Int) -> Unit,
    onRemoveQueueItem: (Int) -> Unit,
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
        if (draggedTrackId == null) {
            visibleQueue = queue
        }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val maxQueueListHeight = (LocalConfiguration.current.screenHeightDp * QueueSheetMaxHeightFraction).dp
    val sheetShape = RoundedCornerShape(topStart = QueueSheetCornerRadius, topEnd = QueueSheetCornerRadius)
    val sheetGradient = remember(gradientColors) {
        Brush.verticalGradient(
            colors = listOf(
                gradientColors.start,
                gradientColors.center,
                gradientColors.end,
            ),
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier,
        shape = sheetShape,
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 0.dp,
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.32f),
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(sheetGradient, sheetShape),
        ) {
            Text(
                text = "Cola de reproducción",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxQueueListHeight),
                contentPadding = PaddingValues(bottom = 24.dp),
            ) {
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
                            fadeOutSpec = tween(durationMillis = 220),
                            placementSpec = null,
                        )
                    }

                    QueueSwipeableItem(
                        track = track,
                        gradientColors = gradientColors,
                        enabled = !isDragging,
                        onRemove = {
                            visibleQueue.indexOfFirst { it.id == track.id }
                                .takeIf { it >= 0 }
                                ?.let(onRemoveQueueItem)
                        },
                        modifier = placementModifier
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .zIndex(if (isDragging) 1f else 0f),
                    ) { swipeProgress ->
                        QueueTrackRow(
                            track = track,
                            isCurrent = track.id == currentTrackId,
                            isDragging = isDragging,
                            onClick = {
                                visibleQueue.indexOfFirst { it.id == track.id }
                                    .takeIf { it >= 0 }
                                    ?.let(onPlayQueueItem)
                            },
                            modifier = Modifier.graphicsLayer {
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
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QueueSwipeableItem(
    track: Track,
    gradientColors: NowPlayingGradientColors,
    enabled: Boolean,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (swipeProgress: Float) -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onRemove()
                true
            } else {
                false
            }
        },
        positionalThreshold = { totalDistance -> totalDistance * 0.45f },
    )
    val swipeProgress = dismissState.progress
    val shape = RoundedCornerShape(20.dp)
    val deleteBackgroundColor = lerp(
        Color.Transparent,
        gradientColors.end.copy(alpha = 0.72f),
        swipeProgress,
    )
    val iconScale = 0.7f + (0.3f * swipeProgress)
    val iconAlpha = swipeProgress.coerceIn(0f, 1f)

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = enabled,
        backgroundContent = {
            if (swipeProgress > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(shape)
                        .background(deleteBackgroundColor)
                        .padding(horizontal = 24.dp),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_delete),
                        contentDescription = "Quitar ${track.title} de la cola",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.88f),
                        modifier = Modifier.graphicsLayer {
                            scaleX = iconScale
                            scaleY = iconScale
                            alpha = iconAlpha
                        },
                    )
                }
            }
        },
        content = {
            Box(modifier = Modifier.fillMaxWidth()) {
                content(swipeProgress)
            }
        },
    )
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
            .clip(shape)
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
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp)),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                ),
                color = MaterialTheme.colorScheme.onSurface,
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
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Box(
            modifier = dragModifier
                .size(48.dp)
                .clip(CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_item_selection),
                contentDescription = "Reordenar ${track.title}",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                    alpha = if (isDragging) 1f else 0.72f,
                ),
            )
        }
    }
}

private val QueueItemHeight = 80.dp
private val QueueSheetCornerRadius = 28.dp
private const val QueueSheetMaxHeightFraction = 0.55f

private fun <T> List<T>.moved(fromIndex: Int, toIndex: Int): List<T> =
    toMutableList().apply {
        add(toIndex, removeAt(fromIndex))
    }