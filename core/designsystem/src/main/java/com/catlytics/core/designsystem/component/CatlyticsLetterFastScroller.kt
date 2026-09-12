package com.catlytics.core.designsystem.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ScrollAxisRange
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.verticalScrollAxisRange
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

const val LETTER_FAST_SCROLLER_TAG = "letter-fast-scroller"
private const val LETTER_OVERLAY_HIDE_DELAY_MS = 400L

@Composable
fun CatlyticsLetterFastScroller(
    listState: LazyListState,
    itemCount: Int,
    letterForVisibleTrackIndex: (Int) -> Char,
    modifier: Modifier = Modifier,
    headerItemCount: Int = 0,
    minItemCount: Int = FAST_SCROLL_MIN_ITEM_COUNT,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    if (!shouldShowLetterFastScroller(itemCount, minItemCount)) return

    val coroutineScope = rememberCoroutineScope()
    val latestItemCount by rememberUpdatedState(itemCount)
    val latestHeaderItemCount by rememberUpdatedState(headerItemCount)
    var isDragging by remember { mutableStateOf(false) }
    var railHeightPx by remember { mutableFloatStateOf(0f) }

    val visibleIndex by remember(headerItemCount, itemCount) {
        derivedStateOf {
            visibleTrackIndex(
                firstVisibleItemIndex = listState.firstVisibleItemIndex,
                headerItemCount = headerItemCount,
                itemCount = itemCount,
            )
        }
    }
    val revealed by remember(headerItemCount) {
        derivedStateOf {
            isLetterFastScrollerRevealed(
                firstVisibleItemIndex = listState.firstVisibleItemIndex,
                firstVisibleItemScrollOffset = listState.firstVisibleItemScrollOffset,
                headerItemCount = headerItemCount,
            )
        }
    }
    val letter = letterForVisibleTrackIndex(visibleIndex)
    val thumbFraction = scrollFractionForTrackIndex(visibleIndex, itemCount)
    val showRail = revealed || isDragging

    var showLetter by remember { mutableStateOf(false) }
    LaunchedEffect(isDragging) {
        if (isDragging) {
            showLetter = true
        } else {
            delay(LETTER_OVERLAY_HIDE_DELAY_MS.milliseconds)
            showLetter = false
        }
    }

    val onSurface = MaterialTheme.colorScheme.onSurface
    val trackColor by animateColorAsState(
        targetValue = onSurface.copy(alpha = if (isDragging) 0.42f else 0.18f),
        label = "fast-scroll-track-color",
    )
    val thumbColor by animateColorAsState(
        targetValue = onSurface.copy(alpha = if (isDragging) 0.88f else 0.42f),
        label = "fast-scroll-thumb-color",
    )

    AnimatedVisibility(
        visible = showRail,
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding),
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .width(FastScrollTouchWidth)
                    .testTag(LETTER_FAST_SCROLLER_TAG)
                    .onSizeChanged { railHeightPx = it.height.toFloat() }
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitFirstDown()
                            isDragging = true
                            val height = size.height.toFloat().coerceAtLeast(1f)
                            val index = trackIndexForScrollFraction(
                                fraction = down.position.y / height,
                                itemCount = latestItemCount,
                            )
                            coroutineScope.launch {
                                listState.scrollToItem(latestHeaderItemCount + index)
                            }
                            down.consume()
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.first()
                                if (!change.pressed) {
                                    isDragging = false
                                    break
                                }
                                val dragIndex = trackIndexForScrollFraction(
                                    fraction = change.position.y / height,
                                    itemCount = latestItemCount,
                                )
                                coroutineScope.launch {
                                    listState.scrollToItem(latestHeaderItemCount + dragIndex)
                                }
                                change.consume()
                            }
                        }
                    }
                    .semantics {
                        contentDescription = "Índice alfabético, letra $letter"
                        verticalScrollAxisRange = ScrollAxisRange(
                            value = { thumbFraction },
                            maxValue = { 1f },
                        )
                    },
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .width(FastScrollTrackWidth)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(trackColor),
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset {
                            val travel = (railHeightPx - FastScrollThumbHeight.toPx()).coerceAtLeast(0f)
                            IntOffset(0, (thumbFraction * travel).toInt())
                        }
                        .size(width = FastScrollThumbWidth, height = FastScrollThumbHeight)
                        .clip(CircleShape)
                        .background(thumbColor),
                )
            }

            AnimatedVisibility(
                visible = showLetter,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = FastScrollTouchWidth + LetterOverlayDragEndGap)
                    .offset {
                        val overlaySize = LetterOverlaySize.toPx()
                        val thumbHeight = FastScrollThumbHeight.toPx()
                        val travel = (railHeightPx - thumbHeight).coerceAtLeast(0f)
                        val thumbCenter = (thumbFraction * travel) + thumbHeight / 2f
                        val lift = LetterOverlayFingerLift.toPx()
                        val maxY = (railHeightPx - overlaySize).coerceAtLeast(0f)
                        IntOffset(
                            x = 0,
                            y = (thumbCenter - overlaySize / 2f - lift)
                                .toInt()
                                .coerceIn(0, maxY.toInt()),
                        )
                    }
                    .clearAndSetSemantics { },
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Surface(
                    modifier = Modifier.size(LetterOverlaySize),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f),
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp,
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = letter.toString(),
                            style = MaterialTheme.typography.titleLarge,
                        )
                    }
                }
            }
        }
    }
}

private val FastScrollTouchWidth = 32.dp
private val FastScrollTrackWidth = 3.dp
private val FastScrollThumbWidth = 8.dp
private val FastScrollThumbHeight = 28.dp
private val LetterOverlaySize = 44.dp
private val LetterOverlayDragEndGap = 56.dp
private val LetterOverlayFingerLift = 40.dp
