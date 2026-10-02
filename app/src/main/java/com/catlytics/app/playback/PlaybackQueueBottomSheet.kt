package com.catlytics.app.playback

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.unit.IntOffset
import com.catlytics.core.model.PlaybackRepeatMode
import com.catlytics.core.model.SleepTimerState
import kotlinx.coroutines.delay
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import com.catlytics.app.R as AppR
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
    accent: Color = MaterialTheme.colorScheme.primary,
    isPlaying: Boolean = false,
    isShuffleEnabled: Boolean = false,
    repeatMode: PlaybackRepeatMode = PlaybackRepeatMode.Off,
    positionMillis: Long = 0L,
    sleepTimerState: SleepTimerState = SleepTimerState.Inactive,
    onToggleShuffle: () -> Unit = {},
    onCycleRepeatMode: () -> Unit = {},
    onStartSleepTimer: (Int) -> Unit = {},
    onCancelSleepTimer: () -> Unit = {},
) {
    var visibleQueue by remember { mutableStateOf(queue) }
    var draggedTrackId by remember { mutableStateOf<String?>(null) }
    var settlingTrackId by remember { mutableStateOf<String?>(null) }
    var originalIndex by remember { mutableIntStateOf(-1) }
    var dragOffsetSync by remember { mutableFloatStateOf(0f) }
    var headerHeightPx by remember { mutableIntStateOf(0) }
    var controlsHeightPx by remember { mutableIntStateOf(0) }
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

    // Al mezclar o restaurar el orden, la fila llega reordenada: tras la animación de reubicación,
    // se lleva la canción actual a la vista.
    var lastShuffleEnabled by remember { mutableStateOf(isShuffleEnabled) }
    LaunchedEffect(isShuffleEnabled) {
        if (isShuffleEnabled == lastShuffleEnabled) return@LaunchedEffect
        lastShuffleEnabled = isShuffleEnabled
        delay(SHUFFLE_SCROLL_DELAY_MILLIS)
        val currentIndex = visibleQueue.indexOfFirst { it.id == currentTrackId }
        if (currentIndex >= 0 && draggedTrackId == null) {
            listState.animateScrollToItem(currentIndex)
        }
    }

    // Si la canción actual estaba a la vista, la lista la sigue cuando avanza la reproducción.
    var lastCurrentTrackId by remember { mutableStateOf(currentTrackId) }
    LaunchedEffect(currentTrackId) {
        val previousId = lastCurrentTrackId
        lastCurrentTrackId = currentTrackId
        if (previousId == null || previousId == currentTrackId || draggedTrackId != null) return@LaunchedEffect
        val wasVisible = listState.layoutInfo.visibleItemsInfo.any { it.key == previousId }
        val currentIndex = visibleQueue.indexOfFirst { it.id == currentTrackId }
        if (wasVisible && currentIndex >= 0) {
            listState.animateScrollToItem(currentIndex)
        }
    }

    val currentIndex = visibleQueue.indexOfFirst { it.id == currentTrackId }
    val upcomingCount = if (currentIndex >= 0) visibleQueue.size - currentIndex - 1 else visibleQueue.size
    val sleepTimerEnd = (sleepTimerState as? SleepTimerState.Active)?.let { timer ->
        sleepTimerEndIndex(
            queue = visibleQueue,
            currentIndex = currentIndex,
            positionMillis = positionMillis,
            remainingMillis = timer.remainingMillis,
            repeatMode = repeatMode,
        )
    }?.takeIf { draggedTrackId == null && settlingTrackId == null }

    // Siempre expandido: la barra de controles vive al fondo y debe verse desde el inicio.
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val density = LocalDensity.current
    val containerHeightPx = LocalWindowInfo.current.containerSize.height
    val maxSheetHeight = remember(density, containerHeightPx) {
        with(density) { (containerHeightPx * QueueSheetMaxHeightFraction).toDp() }
    }
    // Extra end space so any track, even the last one, can be scrolled to the top of the list.
    val listBottomPadding = with(density) {
        (maxSheetHeight - headerHeightPx.toDp() - controlsHeightPx.toDp() - QueueItemHeight)
            .coerceAtLeast(16.dp)
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
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = stringResource(AppR.string.app_queue_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    AnimatedContent(
                        targetState = upcomingCount,
                        transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
                        label = "QueueUpcomingCount",
                    ) { count ->
                        Text(
                            text = queueUpNextLabel(count),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Box(modifier = Modifier.weight(1f, fill = false)) {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                state = listState,
                userScrollEnabled = draggedTrackId == null,
                contentPadding = PaddingValues(bottom = listBottomPadding),
            ) {
                visibleQueue.forEachIndexed { index, track ->
                item(key = track.id) {
                    val isDragging = track.id == draggedTrackId
                    val isSettling = track.id == settlingTrackId
                    val isLifted = isDragging || isSettling
                    val placementModifier = if (isLifted) {
                        Modifier
                    } else {
                        Modifier.animateItem(
                            fadeInSpec = tween(durationMillis = 220),
                            placementSpec = queuePlacementSpec,
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
                        val isDimmed = currentIndex >= 0 && (
                            index < currentIndex ||
                                (repeatMode == PlaybackRepeatMode.One && index > currentIndex) ||
                                (sleepTimerEnd != null && index > sleepTimerEnd)
                            )
                        val contentAlpha by animateFloatAsState(
                            targetValue = if (isDimmed && !isLifted) DimmedRowAlpha else 1f,
                            animationSpec = tween(durationMillis = 260),
                            label = "queueItemAlpha",
                        )
                        QueueTrackRow(
                            track = track,
                            isCurrent = track.id == currentTrackId,
                            isPlaying = isPlaying,
                            isRepeatingOne = repeatMode == PlaybackRepeatMode.One,
                            accent = accent,
                            contentAlpha = contentAlpha,
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
                            onTrackOptions = {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                onTrackOptions(track)
                            },
                        )
                    }
                }
                if (index == sleepTimerEnd) {
                    item(key = SleepTimerMarkerKey) {
                        SleepTimerMarker(
                            accent = accent,
                            modifier = Modifier.animateItem(
                                fadeInSpec = tween(durationMillis = 260),
                                placementSpec = queuePlacementSpec,
                                fadeOutSpec = tween(durationMillis = 180),
                            ),
                        )
                    }
                }
                }
                item(key = RepeatAllFooterKey) {
                    AnimatedContent(
                        targetState = repeatMode == PlaybackRepeatMode.All && visibleQueue.size > 1,
                        transitionSpec = {
                            (fadeIn(tween(220)) togetherWith fadeOut(tween(160)))
                                .using(SizeTransform(clip = false))
                        },
                        label = "QueueRepeatAllFooter",
                    ) { showFooter ->
                        if (showFooter) {
                            RepeatAllFooter()
                        } else {
                            Spacer(modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(QueueFadeHeight)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, gradientColors.end),
                        ),
                    ),
            )
            }
            QueueControlsBar(
                isShuffleEnabled = isShuffleEnabled,
                repeatMode = repeatMode,
                sleepTimerState = sleepTimerState,
                accent = accent,
                enabled = visibleQueue.isNotEmpty(),
                onToggleShuffle = onToggleShuffle,
                onCycleRepeatMode = onCycleRepeatMode,
                onStartSleepTimer = onStartSleepTimer,
                onCancelSleepTimer = onCancelSleepTimer,
                modifier = Modifier
                    .background(gradientColors.end)
                    .onSizeChanged { controlsHeightPx = it.height },
            )
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
                        contentDescription = stringResource(
                            AppR.string.app_queue_remove_track,
                            track.title,
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
                content(swipeProgress)
            }
        },
    )
}

@Composable
private fun QueueTrackRow(
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
        AsyncImage(
            model = track.artworkUri,
            contentDescription = null,
            placeholder = painterResource(id = R.drawable.placeholder_track),
            error = painterResource(id = R.drawable.placeholder_track),
            fallback = painterResource(id = R.drawable.placeholder_track),
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
                    text = track.title,
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
                    stringResource(AppR.string.app_queue_now_playing_track, track.artist.name)
                } else {
                    track.artist.name
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
                    track.title,
                ),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                    alpha = if (isDragging) 1f else 0.72f,
                ),
                modifier = Modifier.size(QueueActionIconSize),
            )
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
private val QueueFadeHeight = 24.dp
private val queuePlacementSpec = spring(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = Spring.StiffnessMediumLow,
    visibilityThreshold = IntOffset.VisibilityThreshold,
)
private const val SleepTimerMarkerKey = "queue_sleep_timer_marker"
private const val RepeatAllFooterKey = "queue_repeat_all_footer"
private const val DimmedRowAlpha = 0.5f
private const val CurrentRowHighlightAlpha = 0.12f
private const val SHUFFLE_SCROLL_DELAY_MILLIS = 120L
private const val DeleteIconRevealProgress = 0.08f
private const val QueueSheetMaxHeightFraction = 0.92f
private const val QueueSwapThresholdFraction = 0.55f
private const val QueueDragLiftScale = 0.02f

@Composable
internal fun queueUpNextLabel(count: Int): String =
    pluralStringResource(AppR.plurals.app_queue_up_next_count, count, count)

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
