package com.catlytics.core.model

import kotlin.random.Random

fun List<Track>.reorderedForShuffle(
    startTrack: Track,
    random: Random = Random.Default,
): List<Track> {
    val distinct = distinctBy(Track::id)
    if (distinct.size <= 1) return distinct

    val start = distinct.firstOrNull { it.id == startTrack.id } ?: startTrack
    val remaining = distinct.filterNot { it.id == start.id }
    val shuffledRemaining = remaining.shuffled(random).let { shuffled ->
        if (shuffled.size > 1 && shuffled == remaining) {
            shuffled.drop(1) + shuffled.first()
        } else {
            shuffled
        }
    }
    return listOf(start) + shuffledRemaining
}

/**
 * Picks the unshuffled order to keep for a restored queue. The persisted original order is used
 * only while shuffle is enabled and it still describes the same tracks as the restored queue;
 * otherwise the restored queue itself is the original order.
 */
fun restoredOriginalQueue(
    restoredQueue: List<Track>,
    persistedOriginalQueue: List<Track>,
    isShuffleEnabled: Boolean,
): List<Track> {
    if (!isShuffleEnabled || persistedOriginalQueue.isEmpty()) return restoredQueue
    val restoredIds = restoredQueue.map(Track::id).toSet()
    val originalIds = persistedOriginalQueue.map(Track::id).toSet()
    return if (restoredIds == originalIds) persistedOriginalQueue else restoredQueue
}
