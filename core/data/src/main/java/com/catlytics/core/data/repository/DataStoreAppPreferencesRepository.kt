package com.catlytics.core.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.catlytics.core.domain.repository.AppPreferencesRepository
import com.catlytics.core.domain.repository.EqualizerPreferencesRepository
import com.catlytics.core.model.EqualizerMode
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
) : AppPreferencesRepository, EqualizerPreferencesRepository {
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

    override fun observeEqualizerEnabled(): Flow<Boolean> = dataStore.data
        .catch { error ->
            if (error is IOException) {
                emit(emptyPreferences())
            } else {
                throw error
            }
        }
        .map { preferences -> preferences[EQUALIZER_ENABLED] ?: false }

    override fun observeEqualizerPresetName(): Flow<String?> = dataStore.data
        .catch { error ->
            if (error is IOException) {
                emit(emptyPreferences())
            } else {
                throw error
            }
        }
        .map { preferences -> preferences[EQUALIZER_PRESET_NAME] }

    override fun observeEqualizerMode(): Flow<EqualizerMode> = dataStore.data
        .catch { error ->
            if (error is IOException) {
                emit(emptyPreferences())
            } else {
                throw error
            }
        }
        .map { preferences ->
            preferences[EQUALIZER_MODE]?.let { modeStr ->
                runCatching { EqualizerMode.valueOf(modeStr) }.getOrDefault(EqualizerMode.Preset)
            } ?: EqualizerMode.Preset
        }

    override fun observeCustomBandLevels(): Flow<Map<Short, Int>> = dataStore.data
        .catch { error ->
            if (error is IOException) {
                emit(emptyPreferences())
            } else {
                throw error
            }
        }
        .map { preferences ->
            preferences[EQUALIZER_CUSTOM_BANDS]?.let { bandsSet ->
                bandsSet.associate { bandStr ->
                    val parts = bandStr.split(":")
                    parts[0].toShort() to parts[1].toInt()
                }
            } ?: emptyMap()
        }

    override suspend fun setEqualizerEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[EQUALIZER_ENABLED] = enabled
        }
    }

    override suspend fun setEqualizerPresetName(presetName: String?) {
        dataStore.edit { preferences ->
            if (presetName == null) {
                preferences.remove(EQUALIZER_PRESET_NAME)
            } else {
                preferences[EQUALIZER_PRESET_NAME] = presetName
            }
        }
    }

    override suspend fun setEqualizerMode(mode: EqualizerMode) {
        dataStore.edit { preferences ->
            preferences[EQUALIZER_MODE] = mode.name
        }
    }

    override suspend fun setCustomBandLevel(bandId: Short, level: Int) {
        dataStore.edit { preferences ->
            val currentBands = preferences[EQUALIZER_CUSTOM_BANDS] ?: emptySet()
            val map = currentBands.associate { bandStr ->
                val parts = bandStr.split(":")
                parts[0].toShort() to parts[1].toInt()
            }.toMutableMap()
            map[bandId] = level
            preferences[EQUALIZER_CUSTOM_BANDS] = map.map { "${it.key}:${it.value}" }.toSet()
        }
    }

    private fun String.toThemeMode(): ThemeMode =
        runCatching { ThemeMode.valueOf(this) }.getOrDefault(ThemeMode.System)

    private companion object {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val EQUALIZER_ENABLED = booleanPreferencesKey("equalizer_enabled")
        val EQUALIZER_PRESET_NAME = stringPreferencesKey("equalizer_preset_name")
        val EQUALIZER_MODE = stringPreferencesKey("equalizer_mode")
        val EQUALIZER_CUSTOM_BANDS = stringSetPreferencesKey("equalizer_custom_bands")
    }
}
