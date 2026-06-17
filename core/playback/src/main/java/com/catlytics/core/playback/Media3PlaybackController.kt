package com.catlytics.core.playback

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.catlytics.core.domain.repository.LibraryRepository
import com.catlytics.core.domain.repository.PlaybackController
import com.catlytics.core.domain.repository.PlaybackSessionRepository
import com.catlytics.core.domain.repository.PlaylistRepository
import com.catlytics.core.model.PlaybackQueueSource
import com.catlytics.core.model.PlaybackRepeatMode
import com.catlytics.core.model.PlaybackSessionSnapshot
import com.catlytics.core.model.PlaybackState
import com.catlytics.core.model.PlaybackStatus
import com.catlytics.core.model.Track
import com.catlytics.core.playback.service.CatlyticsPlaybackService
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.math.max
import kotlin.time.Duration.Companion.milliseconds

@Singleton
class Media3PlaybackController @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val libraryRepository: LibraryRepository,
    private val playlistRepository: PlaylistRepository,
    private val playbackSessionRepository: PlaybackSessionRepository,
) : PlaybackController {
    private val _playbackState = MutableStateFlow(PlaybackState())
    override val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val playbackScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val controllerFuture: ListenableFuture<MediaController>
    private var queue: List<Track> = emptyList()
    private var queueSource: PlaybackQueueSource = PlaybackQueueSource.Static
    private var progressUpdatesJob: Job? = null
    private var queueSyncJob: Job? = null
    private var lastSessionSaveTimeMillis = 0L

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            updatePlaybackState(player)
        }

        override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
            if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED) return
            playbackScope.launch {
                prunePlayedQueueItems(forcePersist = true)
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            stopProgressUpdates()
            _playbackState.value = _playbackState.value.copy(status = PlaybackStatus.Error)
        }
    }

    init {
        val sessionToken = SessionToken(
            context,
            ComponentName(context, CatlyticsPlaybackService::class.java),
        )
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture.addListener(
            {
                val controller = controllerFuture.get()
                controller.addListener(listener)
                updatePlaybackState(controller)
            },
            ContextCompat.getMainExecutor(context),
        )
    }

    override suspend fun play(
        track: Track,
        queue: List<Track>,
        startIndex: Int,
        queueSource: PlaybackQueueSource,
    ) {
        val playbackQueue = queue.ifEmpty { listOf(track) }
        val selectedIndex = startIndex.coerceIn(0, max(playbackQueue.lastIndex, 0))
        this.queue = playbackQueue
        this.queueSource = queueSource
        withController { controller ->
            controller.setMediaItems(playbackQueue.map { it.toMediaItem() }, selectedIndex, 0L)
            controller.prepare()
            controller.play()
            updatePlaybackState(controller, playbackQueue, forcePersist = true)
        }
        restartQueueSync()
    }

    override suspend fun playQueueItem(index: Int) {
        if (index !in queue.indices) return

        withController { controller ->
            controller.seekTo(index, 0L)
            controller.play()
            updatePlaybackState(controller, forcePersist = true)
        }
    }

    override suspend fun addQueueItem(track: Track) {
        if (_playbackState.value.currentTrack == null || queue.any { it.id == track.id }) return

        queue = queue + track
        withController { controller ->
            controller.addMediaItem(track.toMediaItem())
            updatePlaybackState(controller, forcePersist = true)
        }
    }

    override suspend fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        if (fromIndex !in queue.indices || toIndex !in queue.indices || fromIndex == toIndex) return

        queue = queue.moved(fromIndex, toIndex)
        withController { controller ->
            controller.shuffleModeEnabled = false
            controller.moveMediaItem(fromIndex, toIndex)
            updatePlaybackState(controller, forcePersist = true)
        }
    }

    override suspend fun removeQueueItem(index: Int) {
        if (index !in queue.indices) return

        val updatedQueue = queue.toMutableList().apply { removeAt(index) }
        queue = updatedQueue
        withController { controller ->
            controller.removeMediaItem(index)
            if (updatedQueue.isEmpty()) {
                controller.stop()
                queue = emptyList()
            }
            updatePlaybackState(controller, forcePersist = true)
        }
    }

    override suspend fun togglePlayPause() {
        withController { controller ->
            if (controller.isPlaying) {
                controller.pause()
            } else {
                controller.play()
            }
            updatePlaybackState(controller, forcePersist = true)
        }
    }

    override suspend fun pause() {
        withController { controller ->
            controller.pause()
            updatePlaybackState(controller, forcePersist = true)
        }
    }

    override suspend fun skipNext() {
        withController { controller ->
            controller.seekToNextMediaItem()
            updatePlaybackState(controller, forcePersist = true)
        }
    }

    override suspend fun skipPrevious() {
        withController { controller ->
            controller.seekToPreviousMediaItem()
            updatePlaybackState(controller, forcePersist = true)
        }
    }

    override suspend fun seekTo(positionMillis: Long) {
        withController { controller ->
            controller.seekTo(positionMillis)
            updatePlaybackState(controller, forcePersist = true)
        }
    }

    override suspend fun setShuffleEnabled(enabled: Boolean) {
        withController { controller ->
            controller.shuffleModeEnabled = enabled
            updatePlaybackState(controller, forcePersist = true)
        }
    }

    override suspend fun setRepeatMode(mode: PlaybackRepeatMode) {
        withController { controller ->
            controller.repeatMode = mode.toMedia3RepeatMode()
            updatePlaybackState(controller, forcePersist = true)
        }
    }

    override suspend fun restoreLastSession() {
        if (_playbackState.value.currentTrack != null) return

        val snapshot = playbackSessionRepository.observeSession().first() ?: return
        runCatching { libraryRepository.refreshTracks() }
        val availableTracksById = libraryRepository.observeAllTracks().first().associateBy { it.id }
        val restoredQueue = snapshot.queueTrackIds.mapNotNull(availableTracksById::get)
        if (restoredQueue.isEmpty()) return

        val restoredIndex = restoredQueue.indexOfFirst { it.id == snapshot.currentTrackId }
            .takeUnless { it < 0 }
            ?: snapshot.currentIndex.coerceIn(0, restoredQueue.lastIndex)
        queue = restoredQueue
        queueSource = snapshot.queueSource

        withController { controller ->
            controller.shuffleModeEnabled = snapshot.isShuffleEnabled
            controller.repeatMode = snapshot.repeatMode.toMedia3RepeatMode()
            controller.setMediaItems(
                restoredQueue.map { it.toMediaItem() },
                restoredIndex,
                snapshot.positionMillis.coerceAtLeast(0L),
            )
            controller.prepare()
            controller.pause()
            updatePlaybackState(controller, restoredQueue, forcePersist = true)
        }
        restartQueueSync()
    }

    override suspend fun stop() {
        stopQueueSync()
        queueSource = PlaybackQueueSource.Static
        withController { controller ->
            controller.stop()
            queue = emptyList()
            updatePlaybackState(controller, forcePersist = true)
        }
        playbackSessionRepository.clearSession()
    }

    private fun updatePlaybackState(
        player: Player,
        playbackQueue: List<Track> = queue,
        forcePersist: Boolean = false,
    ) {
        val state = player.toPlaybackState(playbackQueue, queueSource)
        _playbackState.value = state
        persistPlaybackSession(state, forcePersist)
        if (player.isPlaying) {
            startProgressUpdates(player)
        } else {
            stopProgressUpdates()
        }
    }

    private fun startProgressUpdates(player: Player) {
        if (progressUpdatesJob?.isActive == true) return
        progressUpdatesJob = playbackScope.launch {
            while (isActive) {
                delay(PROGRESS_UPDATE_INTERVAL_MILLIS.milliseconds)
                updatePlaybackState(player)
                if (!player.isPlaying) {
                    stopProgressUpdates()
                }
            }
        }
    }

    private fun stopProgressUpdates() {
        progressUpdatesJob?.cancel()
        progressUpdatesJob = null
    }

    private fun persistPlaybackSession(state: PlaybackState, force: Boolean) {
        val snapshot = state.toPlaybackSessionSnapshot() ?: return
        val now = System.currentTimeMillis()
        if (!force && now - lastSessionSaveTimeMillis < SESSION_SAVE_INTERVAL_MILLIS) return

        lastSessionSaveTimeMillis = now
        playbackScope.launch {
            playbackSessionRepository.saveSession(snapshot)
        }
    }

    private fun PlaybackState.toPlaybackSessionSnapshot(): PlaybackSessionSnapshot? {
        val track = currentTrack ?: return null
        if (queue.isEmpty()) return null

        return PlaybackSessionSnapshot(
            queueTrackIds = queue.map { it.id },
            currentTrackId = track.id,
            queueSource = queueSource,
            currentIndex = currentIndex,
            positionMillis = positionMillis,
            isShuffleEnabled = isShuffleEnabled,
            repeatMode = repeatMode,
        )
    }

    private suspend fun withController(block: (MediaController) -> Unit) {
        block(controllerFuture.await())
    }

    private fun restartQueueSync() {
        stopQueueSync()
        queueSyncJob = playbackScope.launch {
            when (val source = queueSource) {
                is PlaybackQueueSource.Playlist -> observePlaylistQueue(source.playlistId)
                    .collect(::reconcileQueue)
                PlaybackQueueSource.Static -> libraryRepository.observeAllTracks()
                    .mapToAvailableTrackIds()
                    .collect(::removeUnavailableQueueItems)
            }
        }
    }

    private fun stopQueueSync() {
        queueSyncJob?.cancel()
        queueSyncJob = null
    }

    private fun observePlaylistQueue(playlistId: String) = combine(
        playlistRepository.observePlaylists(),
        libraryRepository.observeAllTracks(),
    ) { playlists, tracks ->
        val tracksById = tracks.associateBy { it.id }
        playlists
            .firstOrNull { it.id == playlistId }
            ?.trackIds
            .orEmpty()
            .mapNotNull(tracksById::get)
            .distinctBy(Track::id)
    }.distinctUntilChanged()

    private fun kotlinx.coroutines.flow.Flow<List<Track>>.mapToAvailableTrackIds() =
        map { tracks -> tracks.map(Track::id).toSet() }.distinctUntilChanged()

    private suspend fun reconcileQueue(sourceQueue: List<Track>) {
        if (queue.isEmpty()) return
        val currentTrackId = _playbackState.value.currentTrack?.id ?: queue.firstOrNull()?.id
        val currentTrack = currentTrackId?.let { id -> sourceQueue.firstOrNull { it.id == id } }

        if (currentTrack == null) {
            applyQueueReplacement(sourceQueue, startTrackId = sourceQueue.firstOrNull()?.id)
            return
        }

        val sourceCurrentIndex = sourceQueue.indexOfFirst { it.id == currentTrack.id }
        val upcomingTracks = sourceQueue.drop(sourceCurrentIndex)
        applyQueueReplacement(upcomingTracks, startTrackId = currentTrack.id)
    }

    private suspend fun removeUnavailableQueueItems(availableTrackIds: Set<String>) {
        if (queue.isEmpty()) return
        val currentTrackId = _playbackState.value.currentTrack?.id
        val updatedQueue = queue.filter { it.id in availableTrackIds }
        if (updatedQueue.map(Track::id) == queue.map(Track::id)) return
        applyQueueReplacement(
            updatedQueue = updatedQueue,
            startTrackId = currentTrackId?.takeIf { id -> updatedQueue.any { it.id == id } }
                ?: updatedQueue.firstOrNull()?.id,
        )
    }

    private suspend fun applyQueueReplacement(
        updatedQueue: List<Track>,
        startTrackId: String?,
    ) {
        queue = updatedQueue
        withController { controller ->
            if (updatedQueue.isEmpty() || startTrackId == null) {
                controller.stop()
                updatePlaybackState(controller, emptyList(), forcePersist = true)
                playbackScope.launch { playbackSessionRepository.clearSession() }
                return@withController
            }

            val startIndex = updatedQueue.indexOfFirst { it.id == startTrackId }
                .takeUnless { it < 0 }
                ?: 0
            val positionMillis = if (startTrackId == _playbackState.value.currentTrack?.id) {
                controller.currentPosition.coerceAtLeast(0L)
            } else {
                0L
            }
            val shouldPlay = controller.playWhenReady
            controller.setMediaItems(updatedQueue.map { it.toMediaItem() }, startIndex, positionMillis)
            controller.prepare()
            controller.playWhenReady = shouldPlay
            updatePlaybackState(controller, updatedQueue, forcePersist = true)
        }
    }

    private suspend fun prunePlayedQueueItems(forcePersist: Boolean) {
        if (queue.size <= 1) return
        withController { controller ->
            val currentIndex = controller.currentMediaItemIndex
            if (currentIndex <= 0 || currentIndex !in queue.indices) return@withController

            queue = queue.drop(currentIndex)
            controller.removeMediaItems(0, currentIndex)
            updatePlaybackState(controller, queue, forcePersist = forcePersist)
        }
    }

    private suspend fun <T> ListenableFuture<T>.await(): T = suspendCancellableCoroutine { continuation ->
        addListener(
            {
                runCatching { get() }
                    .onSuccess { continuation.resume(it) }
                    .onFailure { continuation.cancel(it) }
            },
            ContextCompat.getMainExecutor(context),
        )
    }

    private companion object {
        const val PROGRESS_UPDATE_INTERVAL_MILLIS = 1_000L
        const val SESSION_SAVE_INTERVAL_MILLIS = 5_000L
    }
}

private fun <T> List<T>.moved(fromIndex: Int, toIndex: Int): List<T> =
    toMutableList().apply {
        add(toIndex, removeAt(fromIndex))
    }
