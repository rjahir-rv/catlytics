package com.catlytics.core.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.catlytics.core.data.local.ManagedImageStore
import com.catlytics.core.domain.repository.PlaylistRepository
import com.catlytics.core.model.LIKED_PLAYLIST_ID
import com.catlytics.core.model.LIKED_PLAYLIST_NAME
import com.catlytics.core.model.Playlist
import com.catlytics.core.model.StatisticsImportMode
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray

private val Context.playlistsDataStore: DataStore<Preferences> by preferencesDataStore("playlists")

@Singleton
class DataStorePlaylistRepository internal constructor(
    private val dataStore: DataStore<Preferences>,
    private val context: Context? = null,
) : PlaylistRepository {
    @Inject
    constructor(@ApplicationContext context: Context) : this(context.playlistsDataStore, context)

    private val coverStore = ManagedImageStore(context, COVER_DIRECTORY)

    override fun observePlaylists(): Flow<List<Playlist>> = dataStore.observe { preferences ->
        preferences[PLAYLISTS]
            ?.decodePlaylists()
            .orEmpty()
            .withLikedPlaylist()
    }

    override suspend fun createPlaylist(name: String, trackIds: List<String>): Playlist {
        val playlist = Playlist(UUID.randomUUID().toString(), name, trackIds.distinct())
        update { playlists ->
            playlists.requireUniqueName(name)
            playlists + playlist
        }
        return playlist
    }

    override suspend fun renamePlaylist(playlistId: String, name: String) = update { playlists ->
        if (playlistId == LIKED_PLAYLIST_ID) return@update playlists.withLikedPlaylist()
        playlists.requireUniqueName(name, exceptPlaylistId = playlistId)
        playlists.map { if (it.id == playlistId) it.copy(name = name) else it }
    }

    override suspend fun updatePlaylistDetails(
        playlistId: String,
        name: String,
        description: String,
    ) = update { playlists ->
        if (playlistId == LIKED_PLAYLIST_ID) return@update playlists.withLikedPlaylist()
        playlists.requireUniqueName(name, exceptPlaylistId = playlistId)
        playlists.map { playlist ->
            if (playlist.id == playlistId) {
                playlist.copy(name = name, description = description)
            } else {
                playlist
            }
        }
    }

    override suspend fun deletePlaylist(playlistId: String) {
        if (playlistId == LIKED_PLAYLIST_ID) return
        var artworkUri: String? = null
        update { playlists ->
            artworkUri = playlists.firstOrNull { it.id == playlistId }?.artworkUri
            playlists.filterNot { it.id == playlistId }
        }
        deleteManagedCover(playlistId, artworkUri)
    }

    override suspend fun addTracks(playlistId: String, trackIds: List<String>): Int =
        addTracksToPlaylists(listOf(playlistId), trackIds)[playlistId] ?: 0

    override suspend fun addTracksToPlaylists(
        playlistIds: Collection<String>,
        trackIds: List<String>,
    ): Map<String, Int> {
        if (playlistIds.isEmpty() || trackIds.isEmpty()) return emptyMap()
        val distinctTrackIds = trackIds.distinct()
        val addedByPlaylist = mutableMapOf<String, Int>()
        update { playlists ->
            playlists.withLikedPlaylist().map { playlist ->
                if (playlist.id !in playlistIds) return@map playlist
                val newIds = distinctTrackIds.filterNot(playlist.trackIds::contains)
                addedByPlaylist[playlist.id] = newIds.size
                playlist.copy(trackIds = playlist.trackIds + newIds)
            }
        }
        return addedByPlaylist
    }

    override suspend fun removeTrack(playlistId: String, trackId: String) {
        removeTracks(playlistId, listOf(trackId))
    }

    override suspend fun removeTracks(playlistId: String, trackIds: Collection<String>): Int {
        val toRemove = trackIds.toSet()
        if (toRemove.isEmpty()) return 0
        var removed = 0
        update { playlists ->
            playlists.withLikedPlaylist().map { playlist ->
                if (playlist.id != playlistId) return@map playlist
                val remaining = playlist.trackIds.filterNot(toRemove::contains)
                removed = playlist.trackIds.size - remaining.size
                playlist.copy(trackIds = remaining)
            }
        }
        return removed
    }

    override suspend fun reorderTracks(playlistId: String, orderedTrackIds: List<String>) =
        update { playlists ->
            playlists.withLikedPlaylist().map { playlist ->
                if (playlist.id != playlistId) return@map playlist
                require(orderedTrackIds.all(playlist.trackIds::contains)) {
                    "El orden contiene canciones que no pertenecen a la playlist."
                }
                val requested = orderedTrackIds.toSet()
                playlist.copy(trackIds = orderedTrackIds + playlist.trackIds.filterNot(requested::contains))
            }
        }

    override suspend fun setPlaylistArtwork(playlistId: String, artworkUri: String?) {
        val persisted = if (artworkUri != null) {
            copyCoverToInternalIfPossible(artworkUri, playlistId)
        } else {
            null
        }
        var previousArtworkUri: String? = null
        update { playlists ->
            playlists.withLikedPlaylist()
                .map { playlist ->
                    if (playlist.id != playlistId) return@map playlist
                    previousArtworkUri = playlist.artworkUri
                    playlist.copy(artworkUri = persisted)
                }
        }
        if (previousArtworkUri != persisted) {
            deleteManagedCover(playlistId, previousArtworkUri)
        }
    }

    override suspend fun restorePlaylists(playlists: List<Playlist>, mode: StatisticsImportMode) {
        val incomingLiked = playlists.firstOrNull { it.id == LIKED_PLAYLIST_ID }
        val incomingOther = playlists.filterNot { it.id == LIKED_PLAYLIST_ID }
        // Covers are deleted only after the new list is committed, so a failed write keeps them.
        val coversToDelete = mutableListOf<Pair<String, String?>>()

        update { currentPlaylists ->
            coversToDelete.clear()
            when (mode) {
                StatisticsImportMode.Replace -> {
                    val currentNonLiked = currentPlaylists.filterNot { it.id == LIKED_PLAYLIST_ID }
                    currentNonLiked.forEach { coversToDelete += it.id to it.artworkUri }

                    val newLiked = (incomingLiked ?: currentPlaylists.firstOrNull { it.id == LIKED_PLAYLIST_ID })
                        ?.copy(name = LIKED_PLAYLIST_NAME)
                        ?: emptyLikedPlaylist()

                    listOf(newLiked) + incomingOther
                }
                StatisticsImportMode.Merge -> {
                    val currentLiked = currentPlaylists.firstOrNull { it.id == LIKED_PLAYLIST_ID }
                    val mergedLiked = if (incomingLiked != null && currentLiked != null) {
                        currentLiked.copy(trackIds = (currentLiked.trackIds + incomingLiked.trackIds).distinct())
                    } else {
                        currentLiked ?: incomingLiked ?: emptyLikedPlaylist()
                    }

                    val resultList = mutableListOf<Playlist>()
                    resultList.add(mergedLiked.copy(name = LIKED_PLAYLIST_NAME))

                    val currentNonLiked = currentPlaylists.filterNot { it.id == LIKED_PLAYLIST_ID }.toMutableList()

                    for (incoming in incomingOther) {
                        val existingIndex = currentNonLiked.indexOfFirst {
                            it.name.equals(incoming.name, ignoreCase = true)
                        }
                        if (existingIndex >= 0) {
                            val existing = currentNonLiked[existingIndex]
                            if (existing.artworkUri != null) {
                                coversToDelete += incoming.id to incoming.artworkUri
                            }
                            val merged = existing.copy(
                                trackIds = (existing.trackIds + incoming.trackIds).distinct(),
                                description = existing.description.ifBlank { incoming.description },
                                artworkUri = existing.artworkUri ?: incoming.artworkUri,
                            )
                            currentNonLiked[existingIndex] = merged
                        } else {
                            currentNonLiked.add(incoming)
                        }
                    }
                    resultList.addAll(currentNonLiked)
                    resultList
                }
            }
        }
        coversToDelete.forEach { (playlistId, artworkUri) -> deleteManagedCover(playlistId, artworkUri) }
    }

    private suspend fun update(transform: (List<Playlist>) -> List<Playlist>) {
        dataStore.edit { preferences ->
            val stored = preferences[PLAYLISTS]
            val current = stored?.let { checkNotNull(it.decodePlaylists()) { "Stored playlists are unreadable." } }
            preferences[PLAYLISTS] = transform(current.orEmpty()).encodePlaylists()
        }
    }

    private fun copyCoverToInternalIfPossible(sourceUri: String, playlistId: String): String =
        coverStore.copyFrom(sourceUri, playlistId) ?: sourceUri

    private fun deleteManagedCover(playlistId: String, artworkUri: String?) {
        coverStore.delete(playlistId, artworkUri)
    }

    internal companion object {
        const val COVER_DIRECTORY = "playlist_covers"
        private val PLAYLISTS = stringPreferencesKey("playlists_json")
    }
}

@Serializable
private data class StoredPlaylist(
    val id: String,
    val name: String,
    val trackIds: List<String> = emptyList(),
    val artworkUri: String? = null,
    val description: String = "",
)

private val playlistJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
}

private fun List<Playlist>.encodePlaylists(): String = playlistJson.encodeToString(
    map { playlist ->
        StoredPlaylist(
            id = playlist.id,
            name = playlist.name,
            trackIds = playlist.trackIds,
            artworkUri = playlist.artworkUri,
            description = playlist.description,
        )
    },
)

private fun emptyLikedPlaylist() =
    Playlist(id = LIKED_PLAYLIST_ID, name = LIKED_PLAYLIST_NAME, trackIds = emptyList())

/** Los nombres se comparan sin distinguir mayúsculas; "Me gusta" cuenta aunque no esté guardada. */
private fun List<Playlist>.requireUniqueName(name: String, exceptPlaylistId: String? = null) {
    val isTaken = withLikedPlaylist().any {
        it.id != exceptPlaylistId && it.name.equals(name, ignoreCase = true)
    }
    require(!isTaken) {
        "Ya existe una playlist con ese nombre."
    }
}

private fun List<Playlist>.withLikedPlaylist(): List<Playlist> {
    val liked = firstOrNull { it.id == LIKED_PLAYLIST_ID }
        ?.copy(name = LIKED_PLAYLIST_NAME)
        ?: emptyLikedPlaylist()
    return listOf(liked) + filterNot { it.id == LIKED_PLAYLIST_ID }
}


private fun String.decodePlaylists(): List<Playlist>? {
    val elements = runCatching { playlistJson.parseToJsonElement(this).jsonArray }.getOrNull() ?: return null
    return elements.mapNotNull { element ->
        runCatching { playlistJson.decodeFromJsonElement<StoredPlaylist>(element) }.getOrNull()
            ?.let { stored ->
                Playlist(
                    id = stored.id,
                    name = stored.name,
                    trackIds = stored.trackIds,
                    artworkUri = stored.artworkUri?.takeIf(String::isNotBlank),
                    description = stored.description,
                )
            }
    }
}
