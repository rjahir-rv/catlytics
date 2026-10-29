package com.catlytics.app.playback

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.platform.LocalDensity
import com.catlytics.core.model.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.sign

@Stable
internal class QueueReorderState(
    initialQueue: List<Track>,
    private val listState: LazyListState,
    private val scope: CoroutineScope,
    private val itemHeightPx: () -> Float,
    private val currentTrackId: () -> String?,
) {
    var visibleQueue by mutableStateOf(initialQueue)
    var draggedTrackId by mutableStateOf<String?>(null)
        private set
    var settlingTrackId by mutableStateOf<String?>(null)
        private set
    private var originalIndex by mutableIntStateOf(-1)
    private var dragOffsetSync by mutableFloatStateOf(0f)
    private var dragVisualTop by mutableFloatStateOf(0f)
    private var dragDirection by mutableIntStateOf(0)
    private val dragOffset = Animatable(0f)

    val isDragging: Boolean
        get() = draggedTrackId != null

    val isReordering: Boolean
        get() = draggedTrackId != null || settlingTrackId != null

    fun translationOf(trackId: String): Float = when (trackId) {
        draggedTrackId -> dragOffsetSync
        settlingTrackId -> dragOffset.value
        else -> 0f
    }

    fun startDrag(trackId: String) {
        settlingTrackId = null
        draggedTrackId = trackId
        originalIndex = visibleQueue.indexOfFirst { it.id == trackId }
        dragOffsetSync = 0f
        dragDirection = 0
        dragVisualTop = listState.layoutInfo.visibleItemsInfo
            .firstOrNull { it.key == trackId }
            ?.offset
            ?.toFloat()
            ?: 0f
        scope.launch { dragOffset.snapTo(0f) }
    }

    fun dragBy(trackId: String, amount: Float) {
        if (amount != 0f) dragDirection = sign(amount).toInt()
        val layoutInfo = listState.layoutInfo
        val minTop = layoutInfo.viewportStartOffset.toFloat()
        val maxTop = (layoutInfo.viewportEndOffset - itemHeightPx()).coerceAtLeast(minTop)
        val targetTop = (dragVisualTop + amount).coerceIn(minTop, maxTop)
        dragOffsetSync += targetTop - dragVisualTop
        dragVisualTop = targetTop
        applyDragSwaps(trackId)

        scope.launch { dragOffset.snapTo(dragOffsetSync) }
    }

    fun endDrag(trackId: String, onMove: (fromIndex: Int, toIndex: Int) -> Unit) {
        val finalIndex = visibleQueue.indexOfFirst { it.id == trackId }
        if (originalIndex >= 0 && finalIndex >= 0 && originalIndex != finalIndex) {
            onMove(originalIndex, finalIndex)
        }
        val residualOffset = dragOffsetSync
        dragOffsetSync = 0f
        settlingTrackId = trackId
        draggedTrackId = null
        originalIndex = -1
        scope.launch {
            dragOffset.snapTo(residualOffset)
            dragOffset.animateTo(targetValue = 0f, animationSpec = dragSettleSpec)
            settlingTrackId = null
        }
    }

    fun cancelDrag(queue: List<Track>) {
        visibleQueue = queue
        draggedTrackId = null
        settlingTrackId = null
        originalIndex = -1
        dragOffsetSync = 0f
        scope.launch { dragOffset.snapTo(0f) }
    }

    /** Desplaza la lista mientras la fila arrastrada toca un borde; termina al soltarla. */
    suspend fun autoScrollWhileDragging() {
        val trackId = draggedTrackId ?: return
        val itemHeight = itemHeightPx()
        val maxStepPx = itemHeight * QUEUE_AUTO_SCROLL_MAX_STEP_FRACTION
        while (true) {
            withFrameNanos { }
            val layoutInfo = listState.layoutInfo
            val delta = queueAutoScrollDelta(
                itemTopPx = dragVisualTop,
                itemBottomPx = dragVisualTop + itemHeight,
                viewportStartPx = layoutInfo.viewportStartOffset.toFloat(),
                viewportEndPx = layoutInfo.viewportEndOffset.toFloat(),
                edgePx = itemHeight,
                maxStepPx = maxStepPx,
            )
            if (delta == 0f || sign(delta).toInt() != dragDirection) continue
            val draggedIndex = visibleQueue.indexOfFirst { it.id == trackId }
            val allowedRange = queueDragIndexRange(
                draggedIndex = draggedIndex,
                currentIndex = visibleQueue.indexOfFirst { it.id == currentTrackId() },
                size = visibleQueue.size,
            )
            if (draggedIndex == allowedRange.first && delta < 0f) continue
            if (draggedIndex == allowedRange.last && delta > 0f) continue
            val consumed = listState.dispatchRawDelta(delta)
            if (consumed != 0f) {
                dragOffsetSync += consumed
                applyDragSwaps(trackId)
            }
        }
    }

    private fun applyDragSwaps(trackId: String) {
        val itemHeight = itemHeightPx()
        val swapThreshold = itemHeight * QUEUE_SWAP_THRESHOLD_FRACTION
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
                currentIndex = visibleQueue.indexOfFirst { it.id == currentTrackId() },
                size = visibleQueue.size,
            )
            val toIndex = fromIndex + direction
            if (toIndex !in allowedRange) {
                val limitedOffset = dragOffsetSync.coerceIn(-swapThreshold, swapThreshold)
                dragVisualTop += limitedOffset - dragOffsetSync
                dragOffsetSync = limitedOffset
                return
            }
            val firstVisibleKey = listState.layoutInfo.visibleItemsInfo.firstOrNull()?.key
            val swapsFirstVisible = firstVisibleKey == trackId ||
                firstVisibleKey == visibleQueue[toIndex].id
            val firstVisibleIndex = listState.firstVisibleItemIndex
            val firstVisibleOffset = listState.firstVisibleItemScrollOffset
            visibleQueue = visibleQueue.moved(fromIndex, toIndex)
            if (swapsFirstVisible) {
                listState.requestScrollToItem(firstVisibleIndex, firstVisibleOffset)
            }
            dragOffsetSync -= itemHeight * direction
        }
    }

    private companion object {
        val dragSettleSpec = spring<Float>(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow,
        )
    }
}

@Composable
internal fun rememberQueueReorderState(
    queue: List<Track>,
    listState: LazyListState,
    currentTrackId: String?,
): QueueReorderState {
    val scope = rememberCoroutineScope()
    val itemHeightPx = with(LocalDensity.current) { QueueItemHeight.toPx() }
    val latestItemHeightPx by rememberUpdatedState(itemHeightPx)
    val latestCurrentTrackId by rememberUpdatedState(currentTrackId)
    val state = remember(listState) {
        QueueReorderState(
            initialQueue = queue,
            listState = listState,
            scope = scope,
            itemHeightPx = { latestItemHeightPx },
            currentTrackId = { latestCurrentTrackId },
        )
    }
    LaunchedEffect(state, state.draggedTrackId) {
        state.autoScrollWhileDragging()
    }
    return state
}

private const val QUEUE_AUTO_SCROLL_MAX_STEP_FRACTION = 0.2f
private const val QUEUE_SWAP_THRESHOLD_FRACTION = 0.55f
internal fun queueDragIndexRange(draggedIndex: Int, currentIndex: Int, size: Int): IntRange = when {
    size <= 0 -> IntRange.EMPTY
    currentIndex !in 0 until size -> 0 until size
    draggedIndex == currentIndex -> currentIndex..currentIndex
    draggedIndex > currentIndex -> (currentIndex + 1) until size
    else -> 0 until currentIndex
}

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
