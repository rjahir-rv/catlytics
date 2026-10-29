package com.catlytics.core.widget

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.nowPlayingWidgetDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "now_playing_widget",
)

@Singleton
class NowPlayingWidgetStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun observe(): Flow<NowPlayingWidgetState> = context.nowPlayingWidgetDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            NowPlayingWidgetState(
                hasTrack = preferences[HAS_TRACK] ?: false,
                title = preferences[TITLE].orEmpty(),
                artist = preferences[ARTIST].orEmpty(),
                artworkUri = preferences[ARTWORK_URI],
                isPlaying = preferences[IS_PLAYING] ?: false,
            )
        }

    suspend fun current(): NowPlayingWidgetState = observe().first()

    suspend fun save(state: NowPlayingWidgetState) {
        context.nowPlayingWidgetDataStore.edit { preferences ->
            preferences[HAS_TRACK] = state.hasTrack
            preferences[TITLE] = state.title
            preferences[ARTIST] = state.artist
            preferences[IS_PLAYING] = state.isPlaying
            if (state.artworkUri != null) {
                preferences[ARTWORK_URI] = state.artworkUri
            } else {
                preferences.remove(ARTWORK_URI)
            }
        }
    }

    private companion object {
        val HAS_TRACK = booleanPreferencesKey("has_track")
        val TITLE = stringPreferencesKey("title")
        val ARTIST = stringPreferencesKey("artist")
        val ARTWORK_URI = stringPreferencesKey("artwork_uri")
        val IS_PLAYING = booleanPreferencesKey("is_playing")
    }
}
