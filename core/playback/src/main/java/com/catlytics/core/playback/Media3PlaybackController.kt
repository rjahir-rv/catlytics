package com.catlytics.core.playback

import android.content.ComponentName
import android.content.Context
import android.content.res.Resources
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.catlytics.core.designsystem.R as DesignSystemR
import com.catlytics.core.designsystem.locale.resolveLocale
import com.catlytics.core.designsystem.locale.withAppLocale
import com.catlytics.core.domain.repository.AppPreferencesRepository
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
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds

/**
 * Traslada [PlaybackQueueModel] al `MediaController` del servicio. Todo corre en el hilo principal,
 * que es el que exige `MediaController`, así que cada operación pública lee y aplica la cola sin
 * intercalarse con otra.
 */
@Singleton
class Media3PlaybackController @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val libraryRepository: LibraryRepository,
    private val playlistRepository: PlaylistRepository,
    private val playbackSessionRepository: PlaybackSessionRepository,
    private val playbackTracker: PlaybackTracker,
    private val appPreferencesRepository: AppPreferencesRepository,
) : PlaybackController {
    private val _playbackState = MutableStateFlow(PlaybackState())
    override val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val playbackScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mediaController = CompletableDeferred<MediaController>()
    private val queueModel = PlaybackQueueModel()
    private var progressUpdatesJob: Job? = null
    private var queueSyncJob: Job? = null
    private var lastSessionSaveTimeMillis = 0L
    private var lastSavedSnapshot: PlaybackSessionSnapshot? = null
    private var suppressPlaybackStateUpdates = false
    private var missingMetadataLabels = MissingMetadataLabels()

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            if (suppressPlaybackStateUpdates) return
            updatePlaybackState(player)
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            val currentTrack = _playbackState.value.currentTrack
            playbackTracker.onPlayingChanged(isPlaying, currentTrack)
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            if (suppressPlaybackStateUpdates) return
            val newTrack = mediaItem?.mediaId?.let { id -> queueModel.queue.find { it.id == id } }
            playbackTracker.onTrackTransition(newTrack)
        }

        override fun onPlayerError(error: PlaybackException) {
            playbackTracker.onSessionEnd()
            stopProgressUpdates()
            _playbackState.value = _playbackState.value.copy(status = PlaybackStatus.Error)
        }
    }

    init {
        val sessionToken = SessionToken(
            context,
            ComponentName(context, CatlyticsPlaybackService::class.java),
        )
        val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture.addListener(
            {
                runCatching { controllerFuture.get() }
                    .onSuccess { controller ->
                        controller.addListener(listener)
                        updatePlaybackState(controller)
                        mediaController.complete(controller)
                    }
                    // Callers then end as cancelled, as with any lost connection, instead of crashing.
                    .onFailure { error ->
                        mediaController.cancel(CancellationException("MediaController connection failed", error))
                    }
            },
            ContextCompat.getMainExecutor(context),
        )
        playbackScope.launch {
            appPreferencesRepository.observeAppLanguage()
                .map { language ->
                    context.withAppLocale(language.resolveLocale()).resources.missingMetadataLabels()
                }
                .distinctUntilChanged()
                .collect { labels ->
                    missingMetadataLabels = labels
                    refreshMissingMetadataLabels()
                }
        }
    }

    override suspend fun play(
        track: Track,
        queue: List<Track>,
        startIndex: Int,
        queueSource: PlaybackQueueSource,
    ) = onMain {
        val controller = controller()
        val replacement = queueModel.play(track, queue, startIndex, queueSource)
        controller.loadQueue(replacement, positionMillis = 0L, playWhenReady = true)
        restartQueueSync()
    }

    override suspend fun playQueueItem(index: Int) = onMain {
        val controller = controller()
        val trackId = _playbackState.value.queue.getOrNull(index)?.id ?: return@onMain
        val queueIndex = queueModel.queue.indexOfFirst { it.id == trackId }
        if (queueIndex < 0) return@onMain

        controller.seekTo(queueIndex, 0L)
        controller.play()
        updatePlaybackState(controller, forcePersist = true)
    }

    override suspend fun addQueueItem(track: Track) = addQueueItems(listOf(track))

    override suspend fun playNext(track: Track) = playNextItems(listOf(track))

    override suspend fun addQueueItems(tracks: List<Track>) = insertQueueItems(tracks, playNext = false)

    override suspend fun playNextItems(tracks: List<Track>) = insertQueueItems(tracks, playNext = true)

    private suspend fun insertQueueItems(tracks: List<Track>, playNext: Boolean) = onMain {
        val controller = controller()
        val currentTrackId = _playbackState.value.currentTrack?.id ?: return@onMain
        val previousQueue = queueModel.queue
        val updatedQueue = queueModel.insert(tracks, currentTrackId, playNext) ?: return@onMain

        mutateQueue(controller, expectedCurrentId = currentTrackId) {
            controller.alignMediaItems(previousQueue, updatedQueue)
        }
        restartQueueSync()
    }

    /**
     * Moves or adds media items so the player matches [target], position by position. Earlier
     * positions are already aligned when a later one is fixed, so each item is touched once.
     */
    private fun Player.alignMediaItems(current: List<Track>, target: List<Track>) {
        val working = current.toMutableList()
        target.forEachIndexed { index, track ->
            if (working.getOrNull(index)?.id == track.id) return@forEachIndexed
            val existingIndex = working.indexOfFirst { it.id == track.id }
            if (existingIndex > index) {
                moveMediaItem(existingIndex, index)
                working.add(index, working.removeAt(existingIndex))
            } else {
                addMediaItem(index, track.toMediaItem(missingMetadataLabels))
                working.add(index, track)
            }
        }
    }

    override suspend fun moveQueueItem(fromIndex: Int, toIndex: Int) = onMain {
        val controller = controller()
        val currentTrackId = _playbackState.value.currentTrack?.id
        if (!queueModel.move(fromIndex, toIndex, currentTrackId)) return@onMain

        mutateQueue(controller, expectedCurrentId = currentTrackId) {
            controller.moveMediaItem(fromIndex, toIndex)
        }
    }

    override suspend fun removeQueueItem(index: Int) = onMain {
        val controller = controller()
        val trackId = _playbackState.value.queue.getOrNull(index)?.id ?: return@onMain
        val queueIndex = queueModel.remove(trackId) ?: return@onMain

        controller.removeMediaItem(queueIndex)
        if (queueModel.queue.isEmpty()) {
            controller.stop()
            queueModel.clear()
        }
        updatePlaybackState(controller, forcePersist = true)
    }

    override suspend fun togglePlayPause() = onMain {
        val controller = controller()
        if (controller.isPlaying) {
            controller.pause()
        } else {
            controller.play()
        }
        updatePlaybackState(controller, forcePersist = true)
    }

    override suspend fun pause() = onMain {
        val controller = controller()
        controller.pause()
        updatePlaybackState(controller, forcePersist = true)
    }

    override suspend fun skipNext() = onMain {
        val controller = controller()
        controller.seekToNextMediaItem()
        updatePlaybackState(controller, forcePersist = true)
    }

    override suspend fun skipPrevious() = onMain {
        val controller = controller()
        if (shouldSkipToPreviousMediaItem(
                positionMillis = controller.currentPosition,
                hasPreviousMediaItem = controller.hasPreviousMediaItem(),
            )
        ) {
            controller.seekToPreviousMediaItem()
        } else {
            controller.seekTo(0L)
        }
        updatePlaybackState(controller, forcePersist = true)
    }

    override suspend fun seekTo(positionMillis: Long) = onMain {
        val controller = controller()
        controller.seekTo(positionMillis)
        val current = _playbackState.value
        val patched = current.copy(
            positionMillis = controller.currentPosition.coerceAtLeast(0L),
            bufferedPositionMillis = controller.bufferedPosition.coerceAtLeast(0L),
            durationMillis = controller.duration.takeIf { it > 0L } ?: current.durationMillis,
        )
        _playbackState.value = patched
        persistPlaybackSession(patched, force = true)
    }

    override suspend fun setShuffleEnabled(enabled: Boolean) = onMain {
        val controller = controller()
        val replacement = queueModel.setShuffleEnabled(enabled, _playbackState.value.currentTrack)
        if (replacement == null) {
            updatePlaybackState(controller, forcePersist = true)
        } else {
            controller.loadQueue(replacement, positionMillis = controller.currentPosition.coerceAtLeast(0L))
        }
    }

    override suspend fun setRepeatMode(mode: PlaybackRepeatMode) = onMain {
        val controller = controller()
        controller.repeatMode = mode.toMedia3RepeatMode()
        updatePlaybackState(controller, forcePersist = true)
    }

    override suspend fun restoreLastSession() = onMain {
        if (_playbackState.value.currentTrack != null) return@onMain

        val snapshot = playbackSessionRepository.observeSession().first() ?: return@onMain
        try {
            libraryRepository.refreshTracks()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            // A failed rescan still lets the last known library restore the session.
        }
        val availableTracksById = libraryRepository.observeAllTracks().first().associateBy { it.id }
        val controller = controller()
        // Something started playing while the library was being refreshed.
        if (_playbackState.value.currentTrack != null) return@onMain
        val replacement = queueModel.restore(snapshot, availableTracksById) ?: return@onMain

        controller.repeatMode = snapshot.repeatMode.toMedia3RepeatMode()
        controller.loadQueue(
            replacement,
            positionMillis = snapshot.positionMillis.coerceAtLeast(0L),
            playWhenReady = false,
        )
        restartQueueSync()
    }

    override suspend fun stop() = onMain {
        val controller = controller()
        playbackTracker.onSessionEnd()
        stopQueueSync()
        controller.stop()
        queueModel.clear()
        updatePlaybackState(controller, forcePersist = true)
        lastSavedSnapshot = null
        playbackSessionRepository.clearSession()
    }

    /**
     * Loads the whole [replacement] queue, keeping the current play/pause state unless
     * [playWhenReady] says otherwise. An empty queue stops playback and clears the saved session.
     */
    private fun MediaController.loadQueue(
        replacement: QueueReplacement,
        positionMillis: Long,
        playWhenReady: Boolean = this.playWhenReady,
    ) {
        if (replacement.queue.isEmpty()) {
            stop()
            queueModel.clear()
            updatePlaybackState(this, forcePersist = true)
            lastSavedSnapshot = null
            playbackScope.launch { playbackSessionRepository.clearSession() }
            return
        }
        shuffleModeEnabled = false
        setMediaItems(
            replacement.queue.map { it.toMediaItem(missingMetadataLabels) },
            replacement.startIndex,
            positionMillis,
        )
        prepare()
        this.playWhenReady = playWhenReady
        updatePlaybackState(this, forcePersist = true)
    }

    private fun mutateQueue(
        player: Player,
        expectedCurrentId: String?,
        block: () -> Unit,
    ) {
        suppressPlaybackStateUpdates = true
        try {
            block()
            restoreCurrentMediaItem(player, expectedCurrentId)
        } finally {
            suppressPlaybackStateUpdates = false
            updatePlaybackState(player, forcePersist = true)
        }
    }

    private fun restoreCurrentMediaItem(player: Player, expectedCurrentId: String?) {
        if (expectedCurrentId == null) return
        if (player.currentMediaItem?.mediaId == expectedCurrentId) return

        val restoreIndex = (0 until player.mediaItemCount).firstOrNull { index ->
            player.getMediaItemAt(index).mediaId == expectedCurrentId
        } ?: return
        player.seekTo(restoreIndex, _playbackState.value.positionMillis.coerceAtLeast(0L))
    }

    private fun updatePlaybackState(player: Player, forcePersist: Boolean = false) {
        if (suppressPlaybackStateUpdates) return
        val isShuffleEnabled = queueModel.isShuffleEnabled
        val state = player.toPlaybackState(queueModel.queue, queueModel.source, isShuffleEnabled)
        val previousTrack = _playbackState.value.currentTrack
        val publishedState = if (
            state.currentTrack == null && previousTrack != null && state.queue.isNotEmpty()
        ) {
            state.copy(
                currentTrack = previousTrack,
                currentIndex = state.queue.indexOfFirst { it.id == previousTrack.id }
                    .takeUnless { it < 0 }
                    ?: state.currentIndex,
            )
        } else {
            state
        }
        _playbackState.value = publishedState
        val publishedCurrentId = publishedState.currentTrack?.id
        if (publishedCurrentId != null && publishedCurrentId != previousTrack?.id) {
            queueModel.pruneManualQueue(publishedCurrentId)
        }
        persistPlaybackSession(publishedState, forcePersist)
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
        val now = System.currentTimeMillis()
        if (!force && now - lastSessionSaveTimeMillis < SESSION_SAVE_INTERVAL_MILLIS) return
        val snapshot = state.toPlaybackSessionSnapshot() ?: return
        if (snapshot == lastSavedSnapshot) return

        lastSessionSaveTimeMillis = now
        lastSavedSnapshot = snapshot
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
            originalQueueTrackIds = if (isShuffleEnabled) queueModel.originalQueue.map(Track::id) else emptyList(),
        )
    }

    /** `MediaController` must be used from the main thread; callers may come from any dispatcher. */
    private suspend fun onMain(block: suspend () -> Unit) = withContext(Dispatchers.Main.immediate) { block() }

    private suspend fun controller(): MediaController = mediaController.await()

    private fun restartQueueSync() {
        stopQueueSync()
        queueSyncJob = playbackScope.launch {
            launch { libraryRepository.observeAllTracks().collect(::refreshQueueMetadata) }
            when (val source = queueModel.source) {
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

    private fun Flow<List<Track>>.mapToAvailableTrackIds() =
        map { tracks -> tracks.map(Track::id).toSet() }.distinctUntilChanged()

    private suspend fun reconcileQueue(sourceQueue: List<Track>) {
        val controller = controller()
        val replacement = queueModel.reconcileWithSource(sourceQueue, _playbackState.value.currentTrack?.id)
            ?: return
        controller.loadQueue(replacement, positionMillis = controller.positionFor(replacement))
    }

    private suspend fun removeUnavailableQueueItems(availableTrackIds: Set<String>) {
        val controller = controller()
        val replacement = queueModel.removeUnavailable(availableTrackIds, _playbackState.value.currentTrack?.id)
            ?: return
        controller.loadQueue(replacement, positionMillis = controller.positionFor(replacement))
    }

    /** Keeps the position when the replacement still starts on the current track. */
    private fun Player.positionFor(replacement: QueueReplacement): Long {
        val startTrackId = replacement.queue.getOrNull(replacement.startIndex)?.id
        return if (startTrackId == _playbackState.value.currentTrack?.id) currentPosition.coerceAtLeast(0L) else 0L
    }

    private suspend fun refreshQueueMetadata(tracks: List<Track>) {
        val controller = controller()
        val changedTracks = queueModel.refreshMetadata(tracks.associateBy(Track::id))
        if (changedTracks.isEmpty()) return
        controller.replaceMediaItemsIn(changedTracks)
    }

    /** Vuelve a publicar los elementos con metadatos ausentes con los textos del idioma actual. */
    private suspend fun refreshMissingMetadataLabels() {
        val controller = controller()
        val tracksToRelabel = queueModel.queue.filter(Track::hasMissingMetadata)
        if (tracksToRelabel.isEmpty()) return
        controller.replaceMediaItemsIn(tracksToRelabel.associateBy(Track::id))
    }

    /** Replaces, in place, the media items whose id is in [tracksById]. */
    private fun Player.replaceMediaItemsIn(tracksById: Map<String, Track>) {
        mutateQueue(this, currentMediaItem?.mediaId) {
            for (index in 0 until mediaItemCount) {
                val track = tracksById[getMediaItemAt(index).mediaId] ?: continue
                replaceMediaItem(index, track.toMediaItem(missingMetadataLabels))
            }
        }
    }

    private companion object {
        const val PROGRESS_UPDATE_INTERVAL_MILLIS = 1_000L
        const val SESSION_SAVE_INTERVAL_MILLIS = 5_000L
    }
}

internal fun shouldSkipToPreviousMediaItem(
    positionMillis: Long,
    hasPreviousMediaItem: Boolean,
): Boolean = positionMillis < PREVIOUS_TRACK_THRESHOLD_MILLIS && hasPreviousMediaItem

private const val PREVIOUS_TRACK_THRESHOLD_MILLIS = 5_000L

private fun Resources.missingMetadataLabels() = MissingMetadataLabels(
    untitledTrack = getString(DesignSystemR.string.ds_untitled_track),
    unknownArtist = getString(DesignSystemR.string.ds_unknown_artist),
    unknownAlbum = getString(DesignSystemR.string.ds_unknown_album),
)
