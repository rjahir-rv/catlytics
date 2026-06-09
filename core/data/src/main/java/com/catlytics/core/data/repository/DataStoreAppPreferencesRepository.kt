package com.catlytics.core.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.catlytics.core.domain.repository.AppPreferencesRepository
import com.catlytics.core.model.ThemeMode
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.appPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "app_preferences",
)

@Singleton
class DataStoreAppPreferencesRepository internal constructor(
    private val dataStore: DataStore<Preferences>,
) : AppPreferencesRepository {
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) : this(context.appPreferencesDataStore)

    override fun observeThemeMode(): Flow<ThemeMode> = dataStore.data
        .catch { error ->
            if (error is IOException) {
                emit(emptyPreferences())
            } else {
                throw error
            }
        }
        .map { preferences ->
            preferences[THEME_MODE]?.toThemeMode() ?: ThemeMode.System
        }

    override suspend fun setThemeMode(themeMode: ThemeMode) {
        dataStore.edit { preferences ->
            preferences[THEME_MODE] = themeMode.name
        }
    }

    private fun String.toThemeMode(): ThemeMode =
        runCatching { ThemeMode.valueOf(this) }.getOrDefault(ThemeMode.System)

    private companion object {
        val THEME_MODE = stringPreferencesKey("theme_mode")
    }
}
