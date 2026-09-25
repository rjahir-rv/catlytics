package com.catlytics.app.playback

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import com.catlytics.core.designsystem.R
import com.catlytics.core.designsystem.component.ArtworkGradientColors
import com.catlytics.core.model.Track
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PlaybackQueueBottomSheet(
    queue: List<Track>,
    currentTrackId: String?,
    gradientColors: ArtworkGradientColors,
    onDismiss: () -> Unit,
    onPlayQueueItem: (Int) -> Unit,
    onMoveQueueItem: (Int, Int) -> Unit,
    onRemoveQueueItem: (Int) -> Unit,
    onTrackOptions: (Track) -> Unit,
    modifier: Modifier = Modifier,
) {
    var visibleQueue by remember { mutableStateOf(queue) }
    var draggedTrackId by remember { mutableStateOf<String?>(null) }
    var settlingTrackId by remember { mutableStateOf<String?>(null) }
    var originalIndex by remember { mutableIntStateOf(-1) }
    var dragOffsetSync by remember { mutableFloatStateOf(0f) }
    var headerHeightPx by remember { mutableIntStateOf(0) }
    val dragOffset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val hapticFeedback = LocalHapticFeedback.current
    val itemHeightPx = with(LocalDensity.current) { QueueItemHeight.toPx() }
    val dragSettleSpec = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow,
    )

    val listState = rememberLazyListState()
    var hasScrolledToCurrent by remember { mutableStateOf(false) }

    LaunchedEffect(currentTrackId) {
        if (!hasScrolledToCurrent && draggedTrackId == null) {
            val currentIndex = visibleQueue.indexOfFirst { it.id == currentTrackId }
            if (currentIndex >= 0) {
                listState.scrollToItem(currentIndex)
                hasScrolledToCurrent = true
            }
        }
    }

    LaunchedEffect(queue) {
        if (draggedTrackId == null) {
            visibleQueue = queue
        }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val density = LocalDensity.current
    val containerHeightPx = LocalWindowInfo.current.containerSize.height
    val maxSheetHeight = remember(density, containerHeightPx) {
        with(density) { (containerHeightPx * QueueSheetMaxHeightFraction).toDp() }
    }
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
        dragHandle = null,
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxSheetHeight)
                .clip(sheetShape)
                .background(sheetGradient)
                .windowInsetsPadding(WindowInsets.navigationBars),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .onSizeChanged { headerHeightPx = it.height },
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(
                                width = QueueDragHandleWidth,
                                height = QueueDragHandleHeight,
                            )
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.32f)),
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Cola de reproducción",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = queueTracksLabel(visibleQueue.size),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                state = listState,
                userScrollEnabled = draggedTrackId == null,
                contentPadding = PaddingValues(bottom = 16.dp),
            ) {
                items(
                    items = visibleQueue,
                    key = Track::id,
                ) { track ->
                    val isDragging = track.id == draggedTrackId
                    val isSettling = track.id == settlingTrackId
                    val isLifted = isDragging || isSettling
                    val placementModifier = if (isLifted) {
                        Modifier
                    } else {
                        Modifier.animateItem(
                            fadeInSpec = null,
                            fadeOutSpec = tween(durationMillis = 220),
                        )
                    }
                    val lift by animateFloatAsState(
                        targetValue = if (isDragging) 1f else 0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMediumLow,
                        ),
                        label = "queueItemLift",
                    )

                    QueueSwipeableItem(
                        track = track,
                        gradientColors = gradientColors,
                        enabled = !isLifted,
                        onRemove = {
                            visibleQueue.indexOfFirst { it.id == track.id }
                                .takeIf { it >= 0 }
                                ?.let(onRemoveQueueItem)
                        },
                        modifier = placementModifier
                            .padding(horizontal = 8.dp)
                            .zIndex(if (isLifted) 1f else 0f)
                            .graphicsLayer {
                                translationY = when {
                                    isDragging -> dragOffsetSync
                                    isSettling -> dragOffset.value
                                    else -> 0f
                                }
                                val scale = 1f + QueueDragLiftScale * lift
                                scaleX = scale
                                scaleY = scale
                            },
                    ) {
                        QueueTrackRow(
                            track = track,
                            isCurrent = track.id == currentTrackId,
                            isDragging = isDragging,
                            liftProgress = lift,
                            onClick = {
                                visibleQueue.indexOfFirst { it.id == track.id }
                                    .takeIf { it >= 0 }
                                    ?.let(onPlayQueueItem)
                            },
                            dragModifier = Modifier.pointerInput(track.id, itemHeightPx) {
                                detectVerticalDragGestures(
                                    onDragStart = {
                                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                        settlingTrackId = null
                                        draggedTrackId = track.id
                                        originalIndex = visibleQueue.indexOfFirst { it.id == track.id }
                                        dragOffsetSync = 0f
                                        scope.launch { dragOffset.snapTo(0f) }
                                    },
                                    onVerticalDrag = { change, amount ->
                                        change.consume()
                                        dragOffsetSync += amount

                                        val layoutInfo = listState.layoutInfo
                                        val draggedItem = layoutInfo.visibleItemsInfo
                                            .firstOrNull { it.key == track.id }
                                        if (draggedItem != null) {
                                            val viewportStart = layoutInfo.viewportStartOffset.toFloat()
                                            val viewportEnd = layoutInfo.viewportEndOffset.toFloat()
                                            val visibleBottom = queueVisibleBottomPx(
                                                containerHeightPx = containerHeightPx,
                                                sheetOffsetPx = sheetState.requireOffset(),
                                                headerHeightPx = headerHeightPx,
                                                viewportStartPx = viewportStart,
                                                viewportEndPx = viewportEnd,
                                            )
                                            val itemTop = draggedItem.offset + dragOffsetSync
                                            val itemBottom = itemTop + itemHeightPx
                                            val edge = itemHeightPx
                                            val scrollDelta = when {
                                                itemTop < viewportStart + edge &&
                                                    listState.canScrollBackward -> {
                                                    -(viewportStart + edge - itemTop)
                                                        .coerceAtMost(itemHeightPx)
                                                }
                                                itemBottom > visibleBottom - edge &&
                                                    listState.canScrollForward -> {
                                                    (itemBottom - (visibleBottom - edge))
                                                        .coerceAtMost(itemHeightPx)
                                                }
                                                else -> 0f
                                            }
                                            if (scrollDelta != 0f) {
                                                dragOffsetSync -= listState.dispatchRawDelta(scrollDelta)
                                            }
                                        }

                                        val swapThreshold = itemHeightPx * QueueSwapThresholdFraction
                                        val direction = when {
                                            dragOffsetSync > swapThreshold -> 1
                                            dragOffsetSync < -swapThreshold -> -1
                                            else -> 0
                                        }
                                        if (direction != 0) {
                                            val fromIndex = visibleQueue.indexOfFirst { it.id == track.id }
                                            val toIndex = (fromIndex + direction)
                                                .coerceIn(visibleQueue.indices)
                                            if (fromIndex >= 0 && fromIndex != toIndex) {
                                                visibleQueue = visibleQueue.moved(fromIndex, toIndex)
                                                dragOffsetSync -= itemHeightPx * direction
                                            }
                                        }

                                        scope.launch { dragOffset.snapTo(dragOffsetSync) }
                                    },
                                    onDragEnd = {
                                        val finalIndex = visibleQueue.indexOfFirst { it.id == track.id }
                                        if (originalIndex >= 0 && finalIndex >= 0 && originalIndex != finalIndex) {
                                            onMoveQueueItem(originalIndex, finalIndex)
                                        }
                                        val residualOffset = dragOffsetSync
                                        dragOffsetSync = 0f
                                        settlingTrackId = track.id
                                        draggedTrackId = null
                                        originalIndex = -1
                                        scope.launch {
                                            dragOffset.snapTo(residualOffset)
                                            dragOffset.animateTo(
                                                targetValue = 0f,
                                                animationSpec = dragSettleSpec,
                                            )
                                            settlingTrackId = null
                                        }
                                    },
                                    onDragCancel = {
                                        visibleQueue = queue
                                        draggedTrackId = null
                                        settlingTrackId = null
                                        originalIndex = -1
                                        dragOffsetSync = 0f
                                        scope.launch { dragOffset.snapTo(0f) }
                                    },
                                )
                            },
                            onTrackOptions = { onTrackOptions(track) },
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
    gradientColors: ArtworkGradientColors,
    enabled: Boolean,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (swipeProgress: Float) -> Unit,
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
                        contentDescription = "Quitar ${track.title} de la cola",
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
    liftProgress: Float,
    onClick: () -> Unit,
    dragModifier: Modifier,
    onTrackOptions: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(QueueItemHeight)
            .clip(QueueRowShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = liftProgress))
            .clickable(onClick = onClick)
            .padding(start = 16.dp, end = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = track.artworkUri,
            contentDescription = null,
            placeholder = painterResource(id = R.drawable.placeholder_track),
            error = painterResource(id = R.drawable.placeholder_track),
            fallback = painterResource(id = R.drawable.placeholder_track),
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(QueueArtworkSize)
                .clip(QueueArtworkShape),
        )
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (isCurrent) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_play),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp),
                    )
                }
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                    ),
                    color = if (isCurrent) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
            }
            Text(
                text = if (isCurrent) {
                    "Reproduciendo · ${track.artist.name}"
                } else {
                    track.artist.name
                },
                style = MaterialTheme.typography.bodyMedium,
                color = if (isCurrent) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(0.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(QueueActionTouchTarget)
                    .clip(CircleShape)
                    .clickable(onClick = onTrackOptions),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_options),
                    contentDescription = "Opciones de ${track.title}",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(QueueActionIconSize),
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
                    contentDescription = "Reordenar ${track.title}",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                        alpha = if (isDragging) 1f else 0.72f,
                    ),
                    modifier = Modifier.size(QueueActionIconSize),
                )
            }
        }
    }
}

private val QueueItemHeight = 64.dp
private val QueueArtworkSize = 40.dp
private val QueueArtworkShape = RoundedCornerShape(10.dp)
private val QueueSheetCornerRadius = 28.dp
private val QueueRowShape = RoundedCornerShape(14.dp)
private val QueueActionIconSize = 20.dp
private val QueueActionTouchTarget = 48.dp
private val QueueDragHandleWidth = 32.dp
private val QueueDragHandleHeight = 4.dp
private const val DeleteIconRevealProgress = 0.08f
private const val QueueSheetMaxHeightFraction = 0.92f
private const val QueueSwapThresholdFraction = 0.55f
private const val QueueDragLiftScale = 0.02f

internal fun queueTracksLabel(count: Int): String =
    if (count == 1) "1 canción" else "$count canciones"

internal fun queueVisibleBottomPx(
    containerHeightPx: Int,
    sheetOffsetPx: Float,
    headerHeightPx: Int,
    viewportStartPx: Float,
    viewportEndPx: Float,
): Float = (containerHeightPx - sheetOffsetPx - headerHeightPx)
    .coerceIn(viewportStartPx, viewportEndPx)

private fun <T> List<T>.moved(fromIndex: Int, toIndex: Int): List<T> =
    toMutableList().apply {
        add(toIndex, removeAt(fromIndex))
    }
