package com.catlytics.core.playback

import com.catlytics.core.model.PlaybackQueueSource
import com.catlytics.core.model.PlaybackSessionSnapshot
import com.catlytics.core.model.Track
import com.catlytics.core.model.reorderedForShuffle
import com.catlytics.core.model.restoredOriginalQueue
import kotlin.random.Random

/**
 * Estado de la cola sin Media3: el orden que se reproduce, el orden original (para salir del modo
 * aleatorio), lo añadido a mano y la fuente. Cada operación actualiza el estado y devuelve lo que el
 * reproductor debe aplicar; [Media3PlaybackController] solo lo traslada al `MediaController`.
 *
 */
internal class PlaybackQueueModel(
    private val random: Random = Random.Default,
) {
    var queue: List<Track> = emptyList()
        private set
    var originalQueue: List<Track> = emptyList()
        private set
    var manualQueue: List<Track> = emptyList()
        private set
    var source: PlaybackQueueSource = PlaybackQueueSource.Static
        private set
    var isShuffleEnabled: Boolean = false
        private set

    /**
     * Carga [tracks] en su orden original empezando por [track]. Con aleatorio activo se mezcla aquí,
     * con [track] primero, para que al desactivarlo vuelva el orden de [tracks].
     */
    fun play(
        track: Track,
        tracks: List<Track>,
        startIndex: Int,
        source: PlaybackQueueSource,
    ): QueueReplacement {
        val distinct = tracks.distinctBy(Track::id).ifEmpty { listOf(track) }
        val index = distinct.indexOfFirst { it.id == track.id }
            .takeUnless { it < 0 }
            ?: startIndex.coerceIn(0, distinct.lastIndex)
        originalQueue = distinct
        manualQueue = emptyList()
        this.source = source
        return if (isShuffleEnabled && distinct.size > 1) {
            queue = distinct.reorderedForShuffle(distinct[index], random)
            QueueReplacement(queue, startIndex = 0)
        } else {
            queue = distinct
            QueueReplacement(queue, startIndex = index)
        }
    }

    /**
     * Pone [tracks] tras la actual: al final de lo añadido a mano o, con [playNext], delante.
     * Devuelve la cola nueva, o null si no cambia.
     */
    fun insert(tracks: List<Track>, currentTrackId: String, playNext: Boolean): List<Track>? {
        val inserted = tracks.distinctBy(Track::id).filterNot { it.id == currentTrackId }
        if (inserted.isEmpty()) return null
        val insertedIds = inserted.mapTo(HashSet(), Track::id)
        pruneManualQueue(currentTrackId)
        val remaining = manualQueue.filterNot { it.id in insertedIds }
        manualQueue = if (playNext) inserted + remaining else remaining + inserted
        val updated = queue.withManualQueueAfterCurrent(currentTrackId, manualQueue)
        if (updated.map(Track::id) == queue.map(Track::id)) return null
        queue = updated
        source = PlaybackQueueSource.Static
        return updated
    }

    fun move(fromIndex: Int, toIndex: Int, currentTrackId: String?): Boolean {
        if (fromIndex !in queue.indices || toIndex !in queue.indices || fromIndex == toIndex) return false
        queue = queue.toMutableList().apply { add(toIndex, removeAt(fromIndex)) }
        if (currentTrackId != null) pruneManualQueue(currentTrackId)
        return true
    }

    /** Quita [trackId] y devuelve la posición que tenía, o null si no estaba. */
    fun remove(trackId: String): Int? {
        val index = queue.indexOfFirst { it.id == trackId }
        if (index < 0) return null
        queue = queue.toMutableList().apply { removeAt(index) }
        manualQueue = manualQueue.filterNot { it.id == trackId }
        return index
    }

    /**
     * Cambia el modo aleatorio. Devuelve la cola reordenada, con [currentTrack] en su posición, o
     * null si no hay que tocar el reproductor.
     */
    fun setShuffleEnabled(enabled: Boolean, currentTrack: Track?): QueueReplacement? {
        if (enabled == isShuffleEnabled) return null
        isShuffleEnabled = enabled
        if (currentTrack == null || queue.size <= 1) return null

        val base = if (enabled) {
            originalQueue.takeIf { it.isNotEmpty() && it.hasSameTracksAs(queue) } ?: queue
        } else {
            originalQueue.takeIf { it.isNotEmpty() && it.hasSameTracksAs(queue) } ?: return null
        }.distinctBy(Track::id)
        originalQueue = base
        val ordered = if (enabled) base.reorderedForShuffle(currentTrack, random) else base
        queue = if (manualQueue.isNotEmpty()) {
            ordered.withManualQueueAfterCurrent(currentTrack.id, manualQueue)
        } else {
            ordered
        }
        val startIndex = queue.indexOfFirst { it.id == currentTrack.id }.coerceAtLeast(0)
        return QueueReplacement(queue, startIndex)
    }

    /** Recupera la sesión guardada con las canciones que siguen existiendo; null si no queda ninguna. */
    fun restore(snapshot: PlaybackSessionSnapshot, tracksById: Map<String, Track>): QueueReplacement? {
        val restored = snapshot.queueTrackIds.mapNotNull(tracksById::get)
        if (restored.isEmpty()) return null
        val index = restored.indexOfFirst { it.id == snapshot.currentTrackId }
            .takeUnless { it < 0 }
            ?: snapshot.currentIndex.coerceIn(0, restored.lastIndex)
        queue = restored
        originalQueue = restoredOriginalQueue(
            restoredQueue = restored,
            persistedOriginalQueue = snapshot.originalQueueTrackIds.mapNotNull(tracksById::get),
            isShuffleEnabled = snapshot.isShuffleEnabled,
        )
        manualQueue = emptyList()
        source = snapshot.queueSource
        isShuffleEnabled = snapshot.isShuffleEnabled
        return QueueReplacement(queue, startIndex = index)
    }

    fun clear() {
        queue = emptyList()
        originalQueue = emptyList()
        manualQueue = emptyList()
        source = PlaybackQueueSource.Static
    }

    /** Aplica los metadatos más recientes y devuelve, por id, las canciones que cambiaron. */
    fun refreshMetadata(tracksById: Map<String, Track>): Map<String, Track> {
        val refreshed = queue.withLatestMetadata(tracksById)
        if (refreshed == queue) return emptyMap()
        val changed = refreshed.filterIndexed { index, track -> track != queue[index] }.associateBy(Track::id)
        queue = refreshed
        originalQueue = originalQueue.withLatestMetadata(tracksById)
        manualQueue = manualQueue.withLatestMetadata(tracksById)
        return changed
    }

    /** La playlist de origen cambió: la cola pasa a ser su contenido actual. */
    fun reconcileWithSource(sourceQueue: List<Track>, currentTrackId: String?): QueueReplacement? {
        if (queue.isEmpty()) return null
        if (sourceQueue.map(Track::id) == originalQueue.map(Track::id)) return null
        val startTrackId = (currentTrackId ?: queue.first().id)
            .takeIf { id -> sourceQueue.any { it.id == id } }
            ?: sourceQueue.firstOrNull()?.id
        return replace(sourceQueue, startTrackId)
    }

    /** Quita de la cola lo que ya no está en la biblioteca. */
    fun removeUnavailable(availableTrackIds: Set<String>, currentTrackId: String?): QueueReplacement? {
        if (queue.isEmpty()) return null
        manualQueue = manualQueue.filter { it.id in availableTrackIds }
        val updated = queue.filter { it.id in availableTrackIds }
        if (updated.map(Track::id) == queue.map(Track::id)) return null
        val startTrackId = currentTrackId?.takeIf { id -> updated.any { it.id == id } }
            ?: updated.firstOrNull()?.id
        return replace(updated, startTrackId)
    }

    /** Lo añadido a mano deja de serlo cuando ya se reprodujo o salió de la cola. */
    fun pruneManualQueue(currentTrackId: String) {
        val currentIndex = queue.indexOfFirst { it.id == currentTrackId }
        if (currentIndex < 0) {
            manualQueue = emptyList()
            return
        }
        val upcomingIds = queue.drop(currentIndex + 1).mapTo(HashSet(), Track::id)
        manualQueue = manualQueue.filter { it.id in upcomingIds }
    }

    private fun replace(updated: List<Track>, startTrackId: String?): QueueReplacement {
        originalQueue = updated
        val startTrack = startTrackId?.let { id -> updated.firstOrNull { it.id == id } }
        if (startTrack == null) {
            queue = emptyList()
            manualQueue = emptyList()
            return QueueReplacement(emptyList(), startIndex = 0)
        }
        queue = if (isShuffleEnabled && updated.size > 1) {
            updated.reorderedForShuffle(startTrack, random)
        } else {
            updated
        }
        return QueueReplacement(queue, startIndex = queue.indexOfFirst { it.id == startTrack.id })
    }
}

/** Cola que el reproductor debe cargar entera; vacía significa detener la reproducción. */
internal data class QueueReplacement(
    val queue: List<Track>,
    val startIndex: Int,
)

private fun List<Track>.hasSameTracksAs(other: List<Track>): Boolean =
    mapTo(HashSet(), Track::id) == other.mapTo(HashSet(), Track::id)

internal fun List<Track>.withLatestMetadata(tracksById: Map<String, Track>): List<Track> =
    map { track -> tracksById[track.id] ?: track }

internal fun List<Track>.withManualQueueAfterCurrent(
    currentTrackId: String,
    manualQueue: List<Track>,
): List<Track> {
    val currentIndex = indexOfFirst { it.id == currentTrackId }
    if (currentIndex < 0) return this

    val current = this[currentIndex]
    val reservedIds = buildSet {
        add(currentTrackId)
        manualQueue.forEach { add(it.id) }
    }
    val prefix = take(currentIndex).filterNot { it.id in reservedIds }
    val suffix = drop(currentIndex + 1).filterNot { it.id in reservedIds }
    return prefix + current + manualQueue + suffix
}
