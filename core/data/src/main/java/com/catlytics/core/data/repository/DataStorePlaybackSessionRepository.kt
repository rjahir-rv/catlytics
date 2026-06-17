package com.catlytics.core.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.catlytics.core.domain.repository.PlaybackSessionRepository
import com.catlytics.core.model.PlaybackQueueSource
import com.catlytics.core.model.PlaybackRepeatMode
import com.catlytics.core.model.PlaybackSessionSnapshot
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.playbackSessionDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "playback_session",
)

@Singleton
class DataStorePlaybackSessionRepository internal constructor(
    private val dataStore: DataStore<Preferences>,
) : PlaybackSessionRepository {
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) : this(context.playbackSessionDataStore)

    override fun observeSession(): Flow<PlaybackSessionSnapshot?> = dataStore.data
        .catch { error ->
            if (error is IOException) {
                emit(emptyPreferences())
            } else {
                throw error
            }
        }
        .map { preferences -> preferences.toPlaybackSessionSnapshot() }

    override suspend fun saveSession(snapshot: PlaybackSessionSnapshot) {
        dataStore.edit { preferences ->
            preferences[QUEUE_TRACK_IDS] = snapshot.queueTrackIds.joinToString(TRACK_ID_SEPARATOR)
            snapshot.currentTrackId?.let { trackId ->
                preferences[CURRENT_TRACK_ID] = trackId
            } ?: preferences.remove(CURRENT_TRACK_ID)
            preferences[CURRENT_INDEX] = snapshot.currentIndex
            preferences[POSITION_MILLIS] = snapshot.positionMillis
            preferences[SHUFFLE_ENABLED] = snapshot.isShuffleEnabled
            preferences[REPEAT_MODE] = snapshot.repeatMode.name
            preferences[QUEUE_SOURCE_TYPE] = snapshot.queueSource.toPreferenceType()
            when (val source = snapshot.queueSource) {
                is PlaybackQueueSource.Playlist -> {
                    preferences[QUEUE_SOURCE_PLAYLIST_ID] = source.playlistId
                }
                PlaybackQueueSource.Static -> {
                    preferences.remove(QUEUE_SOURCE_PLAYLIST_ID)
                }
            }
        }
    }

    override suspend fun clearSession() {
        dataStore.edit { preferences ->
            preferences.remove(QUEUE_TRACK_IDS)
            preferences.remove(CURRENT_TRACK_ID)
            preferences.remove(CURRENT_INDEX)
            preferences.remove(POSITION_MILLIS)
            preferences.remove(SHUFFLE_ENABLED)
            preferences.remove(REPEAT_MODE)
            preferences.remove(QUEUE_SOURCE_TYPE)
            preferences.remove(QUEUE_SOURCE_PLAYLIST_ID)
        }
    }

    private fun Preferences.toPlaybackSessionSnapshot(): PlaybackSessionSnapshot? {
        val queueTrackIds = this[QUEUE_TRACK_IDS]
            ?.split(TRACK_ID_SEPARATOR)
            ?.filter { it.isNotBlank() }
            ?: emptyList()
        if (queueTrackIds.isEmpty()) return null

        return PlaybackSessionSnapshot(
            queueTrackIds = queueTrackIds,
            currentTrackId = this[CURRENT_TRACK_ID],
            queueSource = toPlaybackQueueSource(),
            currentIndex = this[CURRENT_INDEX] ?: 0,
            positionMillis = this[POSITION_MILLIS] ?: 0L,
            isShuffleEnabled = this[SHUFFLE_ENABLED] ?: false,
            repeatMode = this[REPEAT_MODE]?.toPlaybackRepeatMode() ?: PlaybackRepeatMode.Off,
        )
    }

    private fun String.toPlaybackRepeatMode(): PlaybackRepeatMode =
        runCatching { PlaybackRepeatMode.valueOf(this) }.getOrDefault(PlaybackRepeatMode.Off)

    private fun Preferences.toPlaybackQueueSource(): PlaybackQueueSource =
        when (this[QUEUE_SOURCE_TYPE]) {
            QUEUE_SOURCE_TYPE_PLAYLIST -> this[QUEUE_SOURCE_PLAYLIST_ID]
                ?.takeIf(String::isNotBlank)
                ?.let(PlaybackQueueSource::Playlist)
                ?: PlaybackQueueSource.Static
            else -> PlaybackQueueSource.Static
        }

    private fun PlaybackQueueSource.toPreferenceType(): String = when (this) {
        is PlaybackQueueSource.Playlist -> QUEUE_SOURCE_TYPE_PLAYLIST
        PlaybackQueueSource.Static -> QUEUE_SOURCE_TYPE_STATIC
    }

    private companion object {
        const val TRACK_ID_SEPARATOR = "|"
        const val QUEUE_SOURCE_TYPE_STATIC = "static"
        const val QUEUE_SOURCE_TYPE_PLAYLIST = "playlist"
        val QUEUE_TRACK_IDS = stringPreferencesKey("queue_track_ids")
        val CURRENT_TRACK_ID = stringPreferencesKey("current_track_id")
        val QUEUE_SOURCE_TYPE = stringPreferencesKey("queue_source_type")
        val QUEUE_SOURCE_PLAYLIST_ID = stringPreferencesKey("queue_source_playlist_id")
        val CURRENT_INDEX = intPreferencesKey("current_index")
        val POSITION_MILLIS = longPreferencesKey("position_millis")
        val SHUFFLE_ENABLED = booleanPreferencesKey("shuffle_enabled")
        val REPEAT_MODE = stringPreferencesKey("repeat_mode")
    }
}
