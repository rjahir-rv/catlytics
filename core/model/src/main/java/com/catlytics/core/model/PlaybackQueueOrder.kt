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
