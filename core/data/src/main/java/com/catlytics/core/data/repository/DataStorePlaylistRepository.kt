package com.catlytics.core.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.catlytics.core.domain.repository.PlaylistRepository
import com.catlytics.core.model.Playlist
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private val Context.playlistsDataStore: DataStore<Preferences> by preferencesDataStore("playlists")

@Singleton
class DataStorePlaylistRepository internal constructor(
    private val dataStore: DataStore<Preferences>,
) : PlaylistRepository {
    @Inject
    constructor(@ApplicationContext context: Context) : this(context.playlistsDataStore)

    override fun observePlaylists(): Flow<List<Playlist>> = dataStore.data
        .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
        .map { preferences -> preferences[PLAYLISTS]?.decodePlaylists().orEmpty() }

    override suspend fun createPlaylist(name: String, trackIds: List<String>): Playlist {
        val playlist = Playlist(UUID.randomUUID().toString(), name, trackIds.distinct())
        update { playlists ->
            require(playlists.none { it.name.equals(name, ignoreCase = true) }) {
                "Ya existe una playlist con ese nombre."
            }
            playlists + playlist
        }
        return playlist
    }

    override suspend fun renamePlaylist(playlistId: String, name: String) = update { playlists ->
        require(playlists.none { it.id != playlistId && it.name.equals(name, ignoreCase = true) }) {
            "Ya existe una playlist con ese nombre."
        }
        playlists.map { if (it.id == playlistId) it.copy(name = name) else it }
    }

    override suspend fun deletePlaylist(playlistId: String) = update { playlists ->
        playlists.filterNot { it.id == playlistId }
    }

    override suspend fun addTracks(playlistId: String, trackIds: List<String>): Int {
        var added = 0
        update { playlists ->
            playlists.map { playlist ->
                if (playlist.id != playlistId) return@map playlist
                val newIds = trackIds.distinct().filterNot(playlist.trackIds::contains)
                added = newIds.size
                playlist.copy(trackIds = playlist.trackIds + newIds)
            }
        }
        return added
    }

    override suspend fun removeTrack(playlistId: String, trackId: String) = update { playlists ->
        playlists.map { playlist ->
            if (playlist.id == playlistId) playlist.copy(trackIds = playlist.trackIds - trackId)
            else playlist
        }
    }

    private suspend fun update(transform: (List<Playlist>) -> List<Playlist>) {
        dataStore.edit { preferences ->
            preferences[PLAYLISTS] = transform(preferences[PLAYLISTS]?.decodePlaylists().orEmpty())
                .encodePlaylists()
        }
    }

    private companion object {
        val PLAYLISTS = stringPreferencesKey("playlists_json")
    }
}

private fun List<Playlist>.encodePlaylists(): String = buildJsonArray {
    forEach { playlist ->
        add(buildJsonObject {
            put("id", JsonPrimitive(playlist.id))
            put("name", JsonPrimitive(playlist.name))
            put("trackIds", buildJsonArray { playlist.trackIds.forEach { add(JsonPrimitive(it)) } })
        })
    }
}.toString()

private fun String.decodePlaylists(): List<Playlist> = runCatching {
    Json.parseToJsonElement(this).jsonArray.mapNotNull { element ->
        val objectValue = element as? JsonObject ?: return@mapNotNull null
        val id = objectValue["id"]?.jsonPrimitive?.content ?: return@mapNotNull null
        val name = objectValue["name"]?.jsonPrimitive?.content ?: return@mapNotNull null
        val trackIds = (objectValue["trackIds"] as? JsonArray).orEmpty()
            .map { it.jsonPrimitive.content }
        Playlist(id, name, trackIds)
    }
}.getOrDefault(emptyList())
