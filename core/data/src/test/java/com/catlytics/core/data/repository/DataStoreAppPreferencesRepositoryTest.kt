package com.catlytics.core.data.repository

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import com.catlytics.core.domain.repository.PlaybackPreferencesRepository
import com.catlytics.core.model.ThemeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DataStoreAppPreferencesRepositoryTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `theme mode defaults to system`() = runTest {
        val repository = repository(backgroundScope)

        assertEquals(ThemeMode.System, repository.observeThemeMode().first())
    }

    @Test
    fun `setThemeMode persists every available mode`() = runTest {
        val repository = repository(backgroundScope)

        ThemeMode.entries.forEach { themeMode ->
            repository.setThemeMode(themeMode)

            assertEquals(themeMode, repository.observeThemeMode().first())
        }
    }

    @Test
    fun `invalid theme mode falls back to system`() = runTest {
        val dataStore = dataStore(backgroundScope)
        val repository = DataStoreAppPreferencesRepository(dataStore)
        dataStore.edit { preferences ->
            preferences[stringPreferencesKey("theme_mode")] = "Invalid"
        }

        assertEquals(ThemeMode.System, repository.observeThemeMode().first())
    }

    @Test
    fun `crossfade duration defaults to disabled`() = runTest {
        val repository = repository(backgroundScope)

        assertEquals(0, repository.observeCrossfadeDurationSeconds().first())
    }

    @Test
    fun `crossfade duration is persisted and clamped`() = runTest {
        val repository = repository(backgroundScope)

        repository.setCrossfadeDurationSeconds(8)
        assertEquals(8, repository.observeCrossfadeDurationSeconds().first())

        repository.setCrossfadeDurationSeconds(99)
        assertEquals(
            PlaybackPreferencesRepository.MAX_CROSSFADE_DURATION_SECONDS,
            repository.observeCrossfadeDurationSeconds().first(),
        )
    }

    @Test
    fun `corrupt crossfade duration is clamped when read`() = runTest {
        val dataStore = dataStore(backgroundScope)
        val repository = DataStoreAppPreferencesRepository(dataStore)
        dataStore.edit { preferences ->
            preferences[intPreferencesKey("crossfade_duration_seconds")] = -5
        }

        assertEquals(0, repository.observeCrossfadeDurationSeconds().first())
    }

    private fun repository(scope: CoroutineScope): DataStoreAppPreferencesRepository =
        DataStoreAppPreferencesRepository(dataStore(scope))

    private fun dataStore(scope: CoroutineScope) = PreferenceDataStoreFactory.create(
        scope = scope,
        produceFile = {
            temporaryFolder.newFile("app-preferences-${System.nanoTime()}.preferences_pb")
        },
    )
}
