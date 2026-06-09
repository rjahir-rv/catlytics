package com.catlytics.core.data.repository

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
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

    private fun repository(scope: CoroutineScope): DataStoreAppPreferencesRepository =
        DataStoreAppPreferencesRepository(dataStore(scope))

    private fun dataStore(scope: CoroutineScope) = PreferenceDataStoreFactory.create(
        scope = scope,
        produceFile = {
            temporaryFolder.newFile("app-preferences-${System.nanoTime()}.preferences_pb")
        },
    )
}
