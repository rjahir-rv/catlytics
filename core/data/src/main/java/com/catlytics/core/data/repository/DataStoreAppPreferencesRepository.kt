package com.catlytics.core.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.catlytics.core.domain.repository.AppPreferencesRepository
import com.catlytics.core.domain.repository.EqualizerPreferencesRepository
import com.catlytics.core.domain.repository.HomePreferencesRepository
import com.catlytics.core.domain.repository.PlaybackPreferencesRepository
import com.catlytics.core.domain.repository.PlaybackPreferencesRepository.Companion.DEFAULT_CROSSFADE_DURATION_SECONDS
import com.catlytics.core.domain.repository.PlaybackPreferencesRepository.Companion.MAX_CROSSFADE_DURATION_SECONDS
import com.catlytics.core.domain.repository.PlaybackPreferencesRepository.Companion.MIN_CROSSFADE_DURATION_SECONDS
import com.catlytics.core.model.AppLanguage
import com.catlytics.core.model.EqualizerMode
import com.catlytics.core.model.HomeRecommendationsSettings
import com.catlytics.core.model.RecentAddedWindow
import com.catlytics.core.model.ThemeMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.appPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "app_preferences",
)

@Singleton
class DataStoreAppPreferencesRepository internal constructor(
    private val dataStore: DataStore<Preferences>,
) : AppPreferencesRepository,
    EqualizerPreferencesRepository,
    PlaybackPreferencesRepository,
    HomePreferencesRepository {
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) : this(context.appPreferencesDataStore)

    override fun observeThemeMode(): Flow<ThemeMode> = dataStore.observe { preferences ->
        preferences[THEME_MODE].toEnumOr(ThemeMode.System)
    }

    override suspend fun setThemeMode(themeMode: ThemeMode) {
        dataStore.edit { preferences ->
            preferences[THEME_MODE] = themeMode.name
        }
    }

    override fun observeAppLanguage(): Flow<AppLanguage> = dataStore.observe { preferences ->
        preferences[APP_LANGUAGE].toEnumOr(AppLanguage.System)
    }

    override suspend fun setAppLanguage(language: AppLanguage) {
        dataStore.edit { preferences ->
            preferences[APP_LANGUAGE] = language.name
        }
    }

    override fun observeCrossfadeDurationSeconds(): Flow<Int> = dataStore.observe { preferences ->
        (preferences[CROSSFADE_DURATION_SECONDS] ?: DEFAULT_CROSSFADE_DURATION_SECONDS)
            .coerceIn(MIN_CROSSFADE_DURATION_SECONDS, MAX_CROSSFADE_DURATION_SECONDS)
    }

    override suspend fun setCrossfadeDurationSeconds(seconds: Int) {
        dataStore.edit { preferences ->
            preferences[CROSSFADE_DURATION_SECONDS] = seconds.coerceIn(
                MIN_CROSSFADE_DURATION_SECONDS,
                MAX_CROSSFADE_DURATION_SECONDS,
            )
        }
    }

    override fun observeEqualizerEnabled(): Flow<Boolean> =
        dataStore.observe { preferences -> preferences[EQUALIZER_ENABLED] ?: false }

    override fun observeEqualizerPresetName(): Flow<String?> =
        dataStore.observe { preferences -> preferences[EQUALIZER_PRESET_NAME] }

    override fun observeEqualizerMode(): Flow<EqualizerMode> = dataStore.observe { preferences ->
        preferences[EQUALIZER_MODE].toEnumOr(EqualizerMode.Preset)
    }

    override fun observeCustomBandLevels(): Flow<Map<Short, Int>> = dataStore.observe { preferences ->
        preferences[EQUALIZER_CUSTOM_BANDS].toBandLevels()
    }

    override fun observeHomeRecommendationsSettings(): Flow<HomeRecommendationsSettings> =
        dataStore.observe { preferences ->
            HomeRecommendationsSettings(
                showRecommendedPlaylists = preferences[SHOW_RECOMMENDED_PLAYLISTS] ?: true,
                recentAddedWindow = preferences[RECENT_ADDED_WINDOW].toEnumOr(RecentAddedWindow.Days21),
                showNewTrackBadge = preferences[SHOW_NEW_TRACK_BADGE] ?: true,
            )
        }

    override suspend fun setShowRecommendedPlaylists(show: Boolean) {
        dataStore.edit { preferences ->
            preferences[SHOW_RECOMMENDED_PLAYLISTS] = show
        }
    }

    override suspend fun setRecentAddedWindow(window: RecentAddedWindow) {
        dataStore.edit { preferences ->
            preferences[RECENT_ADDED_WINDOW] = window.name
        }
    }

    override suspend fun setShowNewTrackBadge(show: Boolean) {
        dataStore.edit { preferences ->
            preferences[SHOW_NEW_TRACK_BADGE] = show
        }
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
            val map = currentBands.toBandLevels().toMutableMap()
            map[bandId] = level
            preferences[EQUALIZER_CUSTOM_BANDS] = map.map { "${it.key}:${it.value}" }.toSet()
        }
    }

    private fun Set<String>?.toBandLevels(): Map<Short, Int> = orEmpty()
        .mapNotNull { entry ->
            val bandId = entry.substringBefore(':').toShortOrNull() ?: return@mapNotNull null
            val level = entry.substringAfter(':', missingDelimiterValue = "").toIntOrNull()
                ?: return@mapNotNull null
            bandId to level
        }
        .toMap()

    private companion object {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val APP_LANGUAGE = stringPreferencesKey("app_language")
        val CROSSFADE_DURATION_SECONDS = intPreferencesKey("crossfade_duration_seconds")
        val EQUALIZER_ENABLED = booleanPreferencesKey("equalizer_enabled")
        val EQUALIZER_PRESET_NAME = stringPreferencesKey("equalizer_preset_name")
        val EQUALIZER_MODE = stringPreferencesKey("equalizer_mode")
        val EQUALIZER_CUSTOM_BANDS = stringSetPreferencesKey("equalizer_custom_bands")
        val SHOW_RECOMMENDED_PLAYLISTS = booleanPreferencesKey("show_recommended_playlists")
        val RECENT_ADDED_WINDOW = stringPreferencesKey("recent_added_window")
        val SHOW_NEW_TRACK_BADGE = booleanPreferencesKey("show_new_track_badge")
    }
}
