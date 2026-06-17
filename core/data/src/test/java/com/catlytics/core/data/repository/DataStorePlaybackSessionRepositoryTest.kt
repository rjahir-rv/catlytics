package com.catlytics.core.data.repository

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.catlytics.core.model.PlaybackQueueSource
import com.catlytics.core.model.PlaybackRepeatMode
import com.catlytics.core.model.PlaybackSessionSnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DataStorePlaybackSessionRepositoryTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `saveSession persists queue order position shuffle and repeat`() = runTest {
        val repository = repository(backgroundScope)
        val snapshot = PlaybackSessionSnapshot(
            queueTrackIds = listOf("track-2", "track-1", "track-3"),
            currentTrackId = "track-1",
            queueSource = PlaybackQueueSource.Playlist("playlist-1"),
            currentIndex = 1,
            positionMillis = 42_000L,
            isShuffleEnabled = true,
            repeatMode = PlaybackRepeatMode.All,
        )

        repository.saveSession(snapshot)

        assertEquals(snapshot, repository.observeSession().first())
    }

    @Test
    fun `clearSession removes persisted session`() = runTest {
        val repository = repository(backgroundScope)
        repository.saveSession(
            PlaybackSessionSnapshot(
                queueTrackIds = listOf("track-1"),
                currentTrackId = "track-1",
            ),
        )

        repository.clearSession()

        assertNull(repository.observeSession().first())
    }

    @Test
    fun `invalid repeat mode falls back to off`() = runTest {
        val dataStore = dataStore(backgroundScope)
        val repository = DataStorePlaybackSessionRepository(dataStore)
        dataStore.edit { preferences ->
            preferences[stringPreferencesKey("queue_track_ids")] = "track-1|track-2"
            preferences[stringPreferencesKey("repeat_mode")] = "Invalid"
        }

        val snapshot = repository.observeSession().first()

        assertEquals(PlaybackRepeatMode.Off, snapshot?.repeatMode)
    }

    @Test
    fun `missing queue source falls back to static`() = runTest {
        val dataStore = dataStore(backgroundScope)
        val repository = DataStorePlaybackSessionRepository(dataStore)
        dataStore.edit { preferences ->
            preferences[stringPreferencesKey("queue_track_ids")] = "track-1|track-2"
        }

        val snapshot = repository.observeSession().first()

        assertEquals(PlaybackQueueSource.Static, snapshot?.queueSource)
    }

    private fun repository(scope: CoroutineScope): DataStorePlaybackSessionRepository =
        DataStorePlaybackSessionRepository(dataStore(scope))

    private fun dataStore(scope: CoroutineScope) = PreferenceDataStoreFactory.create(
        scope = scope,
        produceFile = {
            temporaryFolder.newFile("playback-session-${System.nanoTime()}.preferences_pb")
        },
    )
}
