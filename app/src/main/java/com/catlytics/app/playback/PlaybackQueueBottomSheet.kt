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
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import com.catlytics.app.R as AppR
import com.catlytics.core.designsystem.R
import com.catlytics.core.designsystem.component.ArtworkGradientColors
import com.catlytics.core.model.Track
import kotlin.math.abs
import kotlin.math.sign
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
    // Posición visual del track arrastrado dentro del viewport; deriva solo del gesto para no
    // depender de un layout que puede ir un frame atrasado tras un intercambio.
    var dragVisualTop by remember { mutableFloatStateOf(0f) }
    // Último sentido del gesto (-1 arriba, 1 abajo, 0 sin mover): el auto-scroll solo avanza hacia
    // donde el usuario arrastró, así tomar un track que ya está en el borde no desplaza la lista.
    var dragDirection by remember { mutableIntStateOf(0) }
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
    // Las canciones que ya sonaron se agrupan bajo una cabecera que arranca contraída.
    var isHistoryCollapsed by rememberSaveable { mutableStateOf(true) }

    fun currentTrackScrollIndex(): Int? = visibleQueue.indexOfFirst { it.id == currentTrackId }
        .takeIf { it >= 0 }
        ?.let { index -> queueCurrentScrollIndex(index, isHistoryCollapsed) }

    LaunchedEffect(currentTrackId) {
        if (!hasScrolledToCurrent && draggedTrackId == null) {
            currentTrackScrollIndex()?.let { scrollIndex ->
                listState.scrollToItem(scrollIndex)
                hasScrolledToCurrent = true
            }
        }
    }

    // Al mezclar o restaurar el orden cambia casi toda la fila. En lugar de que cada fila viaje a su
    // nueva posición (y la lista se desplace después), la canción actual se queda fija y el resto
    // aparece al instante en su nuevo orden con una entrada corta y escalonada.
    val reshuffleProgress = remember { Animatable(1f) }
    val isReshuffling by remember { derivedStateOf { reshuffleProgress.value < 1f } }
    var isReshufflePending by remember { mutableStateOf(false) }
    var lastShuffleEnabled by remember { mutableStateOf(isShuffleEnabled) }
    LaunchedEffect(queue, isShuffleEnabled) {
        if (isShuffleEnabled != lastShuffleEnabled) {
            lastShuffleEnabled = isShuffleEnabled
            isReshufflePending = queue.size > 1
        }
        if (draggedTrackId != null) return@LaunchedEffect
        val orderChanged = visibleQueue.map(Track::id) != queue.map(Track::id)
        if (!isReshufflePending || !orderChanged) {
            visibleQueue = queue
            reshuffleProgress.snapTo(1f)
            return@LaunchedEffect
        }
        isReshufflePending = false
        reshuffleProgress.snapTo(0f)
        visibleQueue = queue
        currentTrackScrollIndex()?.let { listState.scrollToItem(it) }
        reshuffleProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = ReshuffleRevealMillis, easing = LinearEasing),
        )
    }

    // Si la canción actual estaba a la vista, la lista la sigue cuando avanza la reproducción.
    var lastCurrentTrackId by remember { mutableStateOf(currentTrackId) }
    LaunchedEffect(currentTrackId) {
        val previousId = lastCurrentTrackId
        lastCurrentTrackId = currentTrackId
        if (previousId == null || previousId == currentTrackId || draggedTrackId != null) return@LaunchedEffect
        val wasVisible = listState.layoutInfo.visibleItemsInfo.any { it.key == previousId }
        val scrollIndex = currentTrackScrollIndex()
        if (wasVisible && scrollIndex != null) {
            listState.animateScrollToItem(scrollIndex)
        }
    }

    val currentIndex = visibleQueue.indexOfFirst { it.id == currentTrackId }
    val upcomingCount = if (currentIndex >= 0) visibleQueue.size - currentIndex - 1 else visibleQueue.size
    val historyCount = currentIndex.coerceAtLeast(0)
    val sleepTimerEnd = (sleepTimerState as? SleepTimerState.Active)?.let { timer ->
        sleepTimerEndIndex(
            queue = visibleQueue,
            currentIndex = currentIndex,
            positionMillis = positionMillis,
            remainingMillis = timer.remainingMillis,
            repeatMode = repeatMode,
        )
    }?.takeIf { draggedTrackId == null && settlingTrackId == null }

    // La hoja siempre está "expandida" para que la barra de controles del fondo se vea desde el
    // inicio; su altura arranca a media pantalla y crece por completo en cuanto el usuario hace scroll.
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val density = LocalDensity.current
    val containerHeightPx = LocalWindowInfo.current.containerSize.height
    val maxSheetHeight = remember(density, containerHeightPx) {
        with(density) { (containerHeightPx * QueueSheetMaxHeightFraction).toDp() }
    }
    val collapsedSheetHeight = remember(density, containerHeightPx) {
        with(density) { (containerHeightPx * QueueSheetCollapsedHeightFraction).toDp() }
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
                // La hoja absorbe este primer gesto para que la lista no salte mientras crece.
                return available
            }
        }
    }
    val latestCurrentTrackId by rememberUpdatedState(currentTrackId)

    // Intercambia el track arrastrado con sus vecinos mientras su desplazamiento supere el umbral;
    // puede encadenar varios pasos si el auto-scroll avanzó más de una fila. Ningún track cruza la
    // canción en reproducción: lo que viene después no pasa al historial y viceversa.
    fun applyDragSwaps(trackId: String) {
        val swapThreshold = itemHeightPx * QueueSwapThresholdFraction
        while (true) {
            val direction = when {
                dragOffsetSync > swapThreshold -> 1
                dragOffsetSync < -swapThreshold -> -1
                else -> return
            }
            val fromIndex = visibleQueue.indexOfFirst { it.id == trackId }
            if (fromIndex < 0) return
            val allowedRange = queueDragIndexRange(
                draggedIndex = fromIndex,
                currentIndex = visibleQueue.indexOfFirst { it.id == latestCurrentTrackId },
                size = visibleQueue.size,
            )
            val toIndex = fromIndex + direction
            if (toIndex !in allowedRange) {
                // Tope: el track se queda en su posición límite en lugar de seguir al dedo.
                val limitedOffset = dragOffsetSync.coerceIn(-swapThreshold, swapThreshold)
                dragVisualTop += limitedOffset - dragOffsetSync
                dragOffsetSync = limitedOffset
                return
            }
            // La lista ancla su posición a la key del primer elemento visible; si ese elemento
            // participa en el intercambio, la lista saltaría y el track arrastrado quedaría fuera
            // de la vista. Se fija la posición por índice para que el intercambio no desplace nada.
            val firstVisibleKey = listState.layoutInfo.visibleItemsInfo.firstOrNull()?.key
            val swapsFirstVisible = firstVisibleKey == trackId ||
                firstVisibleKey == visibleQueue[toIndex].id
            val firstVisibleIndex = listState.firstVisibleItemIndex
            val firstVisibleOffset = listState.firstVisibleItemScrollOffset
            visibleQueue = visibleQueue.moved(fromIndex, toIndex)
            if (swapsFirstVisible) {
                listState.requestScrollToItem(firstVisibleIndex, firstVisibleOffset)
            }
            dragOffsetSync -= itemHeightPx * direction
        }
    }

    // Mientras el track arrastrado está en un borde, la lista sigue avanzando por debajo de él
    // aunque el dedo no se mueva.
    LaunchedEffect(draggedTrackId) {
        val trackId = draggedTrackId ?: return@LaunchedEffect
        val maxStepPx = itemHeightPx * QueueAutoScrollMaxStepFraction
        while (true) {
            withFrameNanos { }
            val layoutInfo = listState.layoutInfo
            val delta = queueAutoScrollDelta(
                itemTopPx = dragVisualTop,
                itemBottomPx = dragVisualTop + itemHeightPx,
                viewportStartPx = layoutInfo.viewportStartOffset.toFloat(),
                viewportEndPx = layoutInfo.viewportEndOffset.toFloat(),
                edgePx = itemHeightPx,
                maxStepPx = maxStepPx,
            )
            if (delta == 0f || sign(delta).toInt() != dragDirection) continue
            // No se desplaza hacia un lado en el que el track ya está en su límite.
            val draggedIndex = visibleQueue.indexOfFirst { it.id == trackId }
            val allowedRange = queueDragIndexRange(
                draggedIndex = draggedIndex,
                currentIndex = visibleQueue.indexOfFirst { it.id == latestCurrentTrackId },
                size = visibleQueue.size,
            )
            if (draggedIndex == allowedRange.first && delta < 0f) continue
            if (draggedIndex == allowedRange.last && delta > 0f) continue
            val consumed = listState.dispatchRawDelta(delta)
            if (consumed != 0f) {
                // La fila del track sube/baja con el scroll; el offset lo mantiene bajo el dedo.
                dragOffsetSync += consumed
                applyDragSwaps(trackId)
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
                userScrollEnabled = draggedTrackId == null,
                contentPadding = PaddingValues(bottom = QueueListBottomPadding),
            ) {
                if (historyCount > 0) {
                    item(key = HistoryHeaderKey) {
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
                visibleQueue.forEachIndexed { index, track ->
                if (isHistoryCollapsed && index < currentIndex) return@forEachIndexed
                item(key = track.id) {
                    val isDragging = track.id == draggedTrackId
                    val isSettling = track.id == settlingTrackId
                    val isLifted = isDragging || isSettling
                    val isCurrentTrack = track.id == currentTrackId
                    // Durante una mezcla solo la canción actual conserva su animación de posición; el
                    // resto entra con el escalonado en lugar de viajar desde su sitio anterior.
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
                            visibleQueue.indexOfFirst { it.id == track.id }
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
                                translationY = when {
                                    isDragging -> dragOffsetSync
                                    isSettling -> dragOffset.value
                                    else -> 0f
                                } + (1f - reveal) * ReshuffleRevealOffset.toPx()
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
                                        dragDirection = 0
                                        dragVisualTop = listState.layoutInfo.visibleItemsInfo
                                            .firstOrNull { it.key == track.id }
                                            ?.offset
                                            ?.toFloat()
                                            ?: 0f
                                        scope.launch { dragOffset.snapTo(0f) }
                                    },
                                    onVerticalDrag = { change, amount ->
                                        change.consume()
                                        if (amount != 0f) dragDirection = sign(amount).toInt()
                                        // El track no sale del viewport: se queda pegado al borde y el
                                        // auto-scroll hace pasar la lista por debajo.
                                        val layoutInfo = listState.layoutInfo
                                        val minTop = layoutInfo.viewportStartOffset.toFloat()
                                        val maxTop = (layoutInfo.viewportEndOffset - itemHeightPx)
                                            .coerceAtLeast(minTop)
                                        val targetTop = (dragVisualTop + amount).coerceIn(minTop, maxTop)
                                        dragOffsetSync += targetTop - dragVisualTop
                                        dragVisualTop = targetTop
                                        applyDragSwaps(track.id)

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
                    .background(gradientColors.end),
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
private val QueueListBottomPadding = 16.dp
private val queuePlacementSpec = spring(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = Spring.StiffnessMediumLow,
    visibilityThreshold = IntOffset.VisibilityThreshold,
)
private const val SleepTimerMarkerKey = "queue_sleep_timer_marker"
private const val RepeatAllFooterKey = "queue_repeat_all_footer"
private const val HistoryHeaderKey = "queue_history_header"
private const val DimmedRowAlpha = 0.5f
private const val CurrentRowHighlightAlpha = 0.12f
private const val ReshuffleRevealMillis = 360
private const val ReshuffleStaggerFraction = 0.07f
private const val ReshuffleMaxStaggerSteps = 7
private const val ReshuffleRowRevealFraction = 0.45f
private val ReshuffleRevealOffset = 12.dp
private const val DeleteIconRevealProgress = 0.08f
private const val QueueSheetMaxHeightFraction = 0.92f
private const val QueueSheetCollapsedHeightFraction = 0.65f
private const val QueueAutoScrollMaxStepFraction = 0.2f
private const val QueueSwapThresholdFraction = 0.55f
private const val QueueDragLiftScale = 0.02f

/**
 * Índice de la lista al que desplazarse para mostrar la canción actual. Con historial, la cabecera
 * ocupa el primer lugar: contraída se muestra junto a la canción actual; desplegada, la canción
 * actual va después de la cabecera y de las canciones que ya sonaron.
 */
internal fun queueCurrentScrollIndex(currentIndex: Int, isHistoryCollapsed: Boolean): Int = when {
    currentIndex <= 0 -> 0
    isHistoryCollapsed -> 0
    else -> currentIndex + 1
}

/**
 * Avance (0..1) de la entrada de una fila tras mezclar la fila: cada fila empieza un poco después
 * que la anterior según su [distance] a la canción actual, con un tope para que las lejanas no
 * esperen de más.
 */
internal fun queueRevealFraction(progress: Float, distance: Int): Float {
    val start = distance.coerceIn(0, ReshuffleMaxStaggerSteps) * ReshuffleStaggerFraction
    val raw = ((progress - start) / ReshuffleRowRevealFraction).coerceIn(0f, 1f)
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

/**
 * Posiciones a las que puede moverse un track arrastrado: los que vienen después de la canción en
 * reproducción no pueden subir por encima de ella y los del historial no pueden bajar. La canción
 * en reproducción se queda en su sitio (moverla cambiaría qué pertenece al historial); sin canción
 * actual, cualquier track se mueve libremente.
 */
internal fun queueDragIndexRange(draggedIndex: Int, currentIndex: Int, size: Int): IntRange = when {
    size <= 0 -> IntRange.EMPTY
    currentIndex !in 0 until size -> 0 until size
    draggedIndex == currentIndex -> currentIndex..currentIndex
    draggedIndex > currentIndex -> (currentIndex + 1) until size
    else -> 0 until currentIndex
}

/**
 * Desplazamiento de la lista por frame mientras se arrastra un track: crece a medida que el track
 * se adentra en la zona de borde ([edgePx]) hasta [maxStepPx]. Negativo hacia arriba, positivo hacia
 * abajo y 0 fuera de los bordes.
 */
internal fun queueAutoScrollDelta(
    itemTopPx: Float,
    itemBottomPx: Float,
    viewportStartPx: Float,
    viewportEndPx: Float,
    edgePx: Float,
    maxStepPx: Float,
): Float {
    if (edgePx <= 0f) return 0f
    val topDepth = viewportStartPx + edgePx - itemTopPx
    val bottomDepth = itemBottomPx - (viewportEndPx - edgePx)
    return when {
        topDepth > 0f -> -maxStepPx * (topDepth / edgePx).coerceAtMost(1f)
        bottomDepth > 0f -> maxStepPx * (bottomDepth / edgePx).coerceAtMost(1f)
        else -> 0f
    }
}

private fun <T> List<T>.moved(fromIndex: Int, toIndex: Int): List<T> =
    toMutableList().apply {
        add(toIndex, removeAt(fromIndex))
    }
