package com.catlytics.core.designsystem.component

import java.text.Normalizer
import kotlin.math.roundToInt

const val FAST_SCROLL_MIN_ITEM_COUNT = 30

fun String.sectionLetter(): Char {
    val trimmed = trimStart()
    if (trimmed.isEmpty()) return '#'

    val stripped = buildString {
        Normalizer.normalize(trimmed.take(1), Normalizer.Form.NFD).forEach { ch ->
            if (Character.getType(ch) != Character.NON_SPACING_MARK.toInt()) {
                append(ch)
            }
        }
    }
    val letter = stripped.firstOrNull()?.uppercaseChar() ?: return '#'
    return if (letter in 'A'..'Z') letter else '#'
}

fun visibleTrackIndex(
    firstVisibleItemIndex: Int,
    headerItemCount: Int,
    itemCount: Int,
): Int {
    if (itemCount <= 0) return 0
    return (firstVisibleItemIndex - headerItemCount).coerceIn(0, itemCount - 1)
}

fun trackIndexForScrollFraction(fraction: Float, itemCount: Int): Int {
    if (itemCount <= 1) return 0
    return (fraction.coerceIn(0f, 1f) * (itemCount - 1)).roundToInt()
}

fun scrollFractionForTrackIndex(trackIndex: Int, itemCount: Int): Float {
    if (itemCount <= 1) return 0f
    return trackIndex.coerceIn(0, itemCount - 1) / (itemCount - 1).toFloat()
}

fun shouldShowLetterFastScroller(
    itemCount: Int,
    minItemCount: Int = FAST_SCROLL_MIN_ITEM_COUNT,
): Boolean = itemCount >= minItemCount

fun isLetterFastScrollerRevealed(
    firstVisibleItemIndex: Int,
    firstVisibleItemScrollOffset: Int,
    headerItemCount: Int,
): Boolean {
    if (headerItemCount <= 0) {
        return firstVisibleItemIndex > 0 || firstVisibleItemScrollOffset > 0
    }
    return firstVisibleItemIndex >= headerItemCount ||
        (firstVisibleItemIndex == headerItemCount - 1 && firstVisibleItemScrollOffset > 0)
}
