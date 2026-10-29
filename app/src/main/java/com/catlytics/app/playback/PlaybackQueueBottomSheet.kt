package com.catlytics.app.playback

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.catlytics.core.designsystem.R
import com.catlytics.core.designsystem.component.ArtworkGradientColors
import com.catlytics.core.model.PlaybackRepeatMode
import com.catlytics.core.model.SleepTimerState
import com.catlytics.core.model.Track
import kotlin.math.abs
import com.catlytics.app.R as AppR

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
    positionMillis: () -> Long = { 0L },
    sleepTimerState: SleepTimerState = SleepTimerState.Inactive,
    onToggleShuffle: () -> Unit = {},
    onCycleRepeatMode: () -> Unit = {},
    onStartSleepTimer: (Int) -> Unit = {},
    onCancelSleepTimer: () -> Unit = {},
) {
    val hapticFeedback = LocalHapticFeedback.current
    val itemHeightPx = with(LocalDensity.current) { QueueItemHeight.toPx() }

    val listState = rememberLazyListState()
    val reorder = rememberQueueReorderState(queue, listState, currentTrackId)
    var hasScrolledToCurrent by remember { mutableStateOf(false) }
    // Las canciones que ya sonaron se agrupan bajo una cabecera que arranca contraída.
    var isHistoryCollapsed by rememberSaveable { mutableStateOf(true) }

    fun currentTrackScrollIndex(): Int? = reorder.visibleQueue.indexOfFirst { it.id == currentTrackId }
        .takeIf { it >= 0 }
        ?.let { index -> queueCurrentScrollIndex(index, isHistoryCollapsed) }

    LaunchedEffect(currentTrackId) {
        if (!hasScrolledToCurrent && !reorder.isDragging) {
            currentTrackScrollIndex()?.let { scrollIndex ->
                listState.scrollToItem(scrollIndex)
                hasScrolledToCurrent = true
            }
        }
    }

    val reshuffleProgress = remember { Animatable(1f) }
    val isReshuffling by remember { derivedStateOf { reshuffleProgress.value < 1f } }
    var isReshufflePending by remember { mutableStateOf(false) }
    var lastShuffleEnabled by remember { mutableStateOf(isShuffleEnabled) }
    LaunchedEffect(queue, isShuffleEnabled) {
        if (isShuffleEnabled != lastShuffleEnabled) {
            lastShuffleEnabled = isShuffleEnabled
            isReshufflePending = queue.size > 1
        }
        if (reorder.isDragging) return@LaunchedEffect
        val orderChanged = reorder.visibleQueue.map(Track::id) != queue.map(Track::id)
        if (!isReshufflePending || !orderChanged) {
            reorder.visibleQueue = queue
            reshuffleProgress.snapTo(1f)
            return@LaunchedEffect
        }
        isReshufflePending = false
        reshuffleProgress.snapTo(0f)
        reorder.visibleQueue = queue
        currentTrackScrollIndex()?.let { listState.scrollToItem(it) }
        reshuffleProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = RESHUFFLE_REVEAL_MILLIS, easing = LinearEasing),
        )
    }

    // Si la canción actual estaba a la vista, la lista la sigue cuando avanza la reproducción.
    var lastCurrentTrackId by remember { mutableStateOf(currentTrackId) }
    LaunchedEffect(currentTrackId) {
        val previousId = lastCurrentTrackId
        lastCurrentTrackId = currentTrackId
        if (previousId == null || previousId == currentTrackId || reorder.isDragging) return@LaunchedEffect
        val wasVisible = listState.layoutInfo.visibleItemsInfo.any { it.key == previousId }
        val scrollIndex = currentTrackScrollIndex()
        if (wasVisible && scrollIndex != null) {
            listState.animateScrollToItem(scrollIndex)
        }
    }

    val currentIndex = reorder.visibleQueue.indexOfFirst { it.id == currentTrackId }
    val upcomingCount = if (currentIndex >=
        0
    ) {
        reorder.visibleQueue.size - currentIndex - 1
    } else {
        reorder.visibleQueue.size
    }
    val historyCount = currentIndex.coerceAtLeast(0)
    val sleepTimerEnd = (sleepTimerState as? SleepTimerState.Active)?.let { timer ->
        sleepTimerEndIndex(
            queue = reorder.visibleQueue,
            currentIndex = currentIndex,
            positionMillis = positionMillis(),
            remainingMillis = timer.remainingMillis,
            repeatMode = repeatMode,
        )
    }?.takeIf { !reorder.isReordering }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val density = LocalDensity.current
    val containerHeightPx = LocalWindowInfo.current.containerSize.height
    val maxSheetHeight = remember(density, containerHeightPx) {
        with(density) { (containerHeightPx * QUEUE_SHEET_MAX_HEIGHT_FRACTION).toDp() }
    }
    val collapsedSheetHeight = remember(density, containerHeightPx) {
        with(density) { (containerHeightPx * QUEUE_SHEET_COLLAPSED_HEIGHT_FRACTION).toDp() }
    }
    var isSheetFullyOpen by rememberSaveable { mutableStateOf(false) }
    val sheetHeightLimit by animateDpAsState(
        targetValue = if (isSheetFullyOpen) maxSheetHeight else collapsedSheetHeight,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "QueueSheetHeight",
    )
    val expandOnScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (isSheetFullyOpen || available.y >= 0f || source != NestedScrollSource.UserInput) {
                    return Offset.Zero
                }
                isSheetFullyOpen = true
                return available
            }
        }
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
                .heightIn(max = sheetHeightLimit)
                .clip(sheetShape)
                .background(sheetGradient)
                .windowInsetsPadding(WindowInsets.navigationBars),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(),
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
            Box(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .nestedScroll(expandOnScrollConnection),
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    state = listState,
                    userScrollEnabled = !reorder.isDragging,
                    contentPadding = PaddingValues(bottom = QueueListBottomPadding),
                ) {
                    if (historyCount > 0) {
                        item(key = HISTORY_HEADER_KEY) {
                            QueueHistoryHeader(
                                count = historyCount,
                                isExpanded = !isHistoryCollapsed,
                                onToggle = { isHistoryCollapsed = !isHistoryCollapsed },
                                modifier = Modifier.animateItem(
                                    fadeInSpec = tween(durationMillis = 220),
                                    placementSpec = queuePlacementSpec,
                                    fadeOutSpec = tween(durationMillis = 180),
                                ),
                            )
                        }
                    }
                    reorder.visibleQueue.forEachIndexed { index, track ->
                        if (isHistoryCollapsed && index < currentIndex) return@forEachIndexed
                        item(key = track.id, contentType = "track") {
                            val isDragging = track.id == reorder.draggedTrackId
                            val isSettling = track.id == reorder.settlingTrackId
                            val isLifted = isDragging || isSettling
                            val isCurrentTrack = track.id == currentTrackId
                            val revealsOnReshuffle = isReshuffling && !isCurrentTrack
                            val revealDistance = if (currentIndex >= 0) abs(index - currentIndex) - 1 else index
                            val placementModifier = if (isLifted || revealsOnReshuffle) {
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
                                    reorder.visibleQueue.indexOfFirst { it.id == track.id }
                                        .takeIf { it >= 0 }
                                        ?.let(onRemoveQueueItem)
                                },
                                modifier = placementModifier
                                    .padding(horizontal = 8.dp)
                                    .zIndex(if (isLifted) 1f else 0f)
                                    .graphicsLayer {
                                        val reveal = if (revealsOnReshuffle) {
                                            queueRevealFraction(reshuffleProgress.value, revealDistance)
                                        } else {
                                            1f
                                        }
                                        alpha = reveal
                                        translationY = reorder.translationOf(track.id) +
                                            (1f - reveal) * ReshuffleRevealOffset.toPx()
                                        val scale = 1f + QUEUE_DRAG_LIFT_SCALE * lift
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
                                    targetValue = if (isDimmed && !isLifted) DIMMED_ROW_ALPHA else 1f,
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
                                        reorder.visibleQueue.indexOfFirst { it.id == track.id }
                                            .takeIf { it >= 0 }
                                            ?.let(onPlayQueueItem)
                                    },
                                    dragModifier = Modifier.pointerInput(track.id, itemHeightPx) {
                                        detectVerticalDragGestures(
                                            onDragStart = {
                                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                                reorder.startDrag(track.id)
                                            },
                                            onVerticalDrag = { change, amount ->
                                                change.consume()
                                                reorder.dragBy(track.id, amount)
                                            },
                                            onDragEnd = { reorder.endDrag(track.id, onMoveQueueItem) },
                                            onDragCancel = { reorder.cancelDrag(queue) },
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
                            item(key = SLEEP_TIMER_MARKER_KEY) {
                                SleepTimerMarker(
                                    accent = accent,
                                    modifier = if (isReshuffling) {
                                        Modifier
                                    } else {
                                        Modifier.animateItem(
                                            fadeInSpec = tween(durationMillis = 260),
                                            placementSpec = queuePlacementSpec,
                                            fadeOutSpec = tween(durationMillis = 180),
                                        )
                                    },
                                )
                            }
                        }
                    }
                    item(key = REPEAT_ALL_FOOTER_KEY) {
                        AnimatedContent(
                            targetState = repeatMode == PlaybackRepeatMode.All && reorder.visibleQueue.size > 1,
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
                enabled = reorder.visibleQueue.isNotEmpty(),
                onToggleShuffle = onToggleShuffle,
                onCycleRepeatMode = onCycleRepeatMode,
                onStartSleepTimer = onStartSleepTimer,
                onCancelSleepTimer = onCancelSleepTimer,
                modifier = Modifier
                    .background(gradientColors.end),
            )
        }
    }
}

internal val QueueItemHeight = 64.dp
private val QueueSheetCornerRadius = 28.dp
internal val QueueRowShape = RoundedCornerShape(14.dp)
internal val QueueActionIconSize = 20.dp
private val QueueDragHandleWidth = 32.dp
private val QueueDragHandleHeight = 4.dp
private val QueueFadeHeight = 24.dp
private val QueueListBottomPadding = 16.dp
private val queuePlacementSpec = spring(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = Spring.StiffnessMediumLow,
    visibilityThreshold = IntOffset.VisibilityThreshold,
)
private const val SLEEP_TIMER_MARKER_KEY = "queue_sleep_timer_marker"
private const val REPEAT_ALL_FOOTER_KEY = "queue_repeat_all_footer"
private const val HISTORY_HEADER_KEY = "queue_history_header"
private const val DIMMED_ROW_ALPHA = 0.5f
private const val RESHUFFLE_REVEAL_MILLIS = 360
private const val RESHUFFLE_STAGGER_FRACTION = 0.07f
private const val RESHUFFLE_MAX_STAGGER_STEPS = 7
private const val RESHUFFLE_ROW_REVEAL_FRACTION = 0.45f
private val ReshuffleRevealOffset = 12.dp
private const val QUEUE_SHEET_MAX_HEIGHT_FRACTION = 0.92f
private const val QUEUE_SHEET_COLLAPSED_HEIGHT_FRACTION = 0.65f
private const val QUEUE_DRAG_LIFT_SCALE = 0.02f

internal fun queueCurrentScrollIndex(currentIndex: Int, isHistoryCollapsed: Boolean): Int = when {
    currentIndex <= 0 -> 0
    isHistoryCollapsed -> 0
    else -> currentIndex + 1
}

internal fun queueRevealFraction(progress: Float, distance: Int): Float {
    val start = distance.coerceIn(0, RESHUFFLE_MAX_STAGGER_STEPS) * RESHUFFLE_STAGGER_FRACTION
    val raw = ((progress - start) / RESHUFFLE_ROW_REVEAL_FRACTION).coerceIn(0f, 1f)
    return FastOutSlowInEasing.transform(raw)
}

@Composable
private fun QueueHistoryHeader(
    count: Int,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val chevronRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = tween(durationMillis = 220),
        label = "queueHistoryChevron",
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .clip(QueueRowShape)
            .clickable(role = Role.Button, onClick = onToggle)
            .padding(start = 16.dp, end = 14.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = pluralStringResource(AppR.plurals.app_queue_history_count, count, count),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Icon(
            painter = painterResource(id = R.drawable.ic_arrow_down),
            contentDescription = stringResource(
                if (isExpanded) AppR.string.app_queue_history_hide else AppR.string.app_queue_history_show,
            ),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .size(QueueActionIconSize)
                .graphicsLayer { rotationZ = chevronRotation },
        )
    }
}

@Composable
internal fun queueUpNextLabel(count: Int): String =
    pluralStringResource(AppR.plurals.app_queue_up_next_count, count, count)
