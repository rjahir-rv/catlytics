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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    private val playbackSessionRepository: PlaybackSessionRepository,
) : PlaybackController {
    private val _playbackState = MutableStateFlow(PlaybackState())
    override val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val playbackScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val controllerFuture: ListenableFuture<MediaController>
    private var queue: List<Track> = emptyList()
    private var progressUpdatesJob: Job? = null
    private var lastSessionSaveTimeMillis = 0L

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            updatePlaybackState(player)
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

    override suspend fun play(track: Track, queue: List<Track>, startIndex: Int) {
        val playbackQueue = queue.ifEmpty { listOf(track) }
        val selectedIndex = startIndex.coerceIn(0, max(playbackQueue.lastIndex, 0))
        this.queue = playbackQueue
        withController { controller ->
            controller.setMediaItems(playbackQueue.map { it.toMediaItem() }, selectedIndex, 0L)
            controller.prepare()
            controller.play()
            updatePlaybackState(controller, playbackQueue, forcePersist = true)
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
        val availableTracksById = libraryRepository.observeTracks().first().associateBy { it.id }
        val restoredQueue = snapshot.queueTrackIds.mapNotNull(availableTracksById::get)
        if (restoredQueue.isEmpty()) return

        val restoredIndex = restoredQueue.indexOfFirst { it.id == snapshot.currentTrackId }
            .takeUnless { it < 0 }
            ?: snapshot.currentIndex.coerceIn(0, restoredQueue.lastIndex)
        queue = restoredQueue

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
    }

    override suspend fun stop() {
        withController { controller ->
            controller.stop()
            updatePlaybackState(controller, forcePersist = true)
        }
        playbackSessionRepository.clearSession()
    }

    private fun updatePlaybackState(
        player: Player,
        playbackQueue: List<Track> = queue,
        forcePersist: Boolean = false,
    ) {
        val state = player.toPlaybackState(playbackQueue)
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
            currentIndex = currentIndex,
            positionMillis = positionMillis,
            isShuffleEnabled = isShuffleEnabled,
            repeatMode = repeatMode,
        )
    }

    private suspend fun withController(block: (MediaController) -> Unit) {
        block(controllerFuture.await())
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
