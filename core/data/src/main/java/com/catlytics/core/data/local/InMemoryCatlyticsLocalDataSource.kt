package com.catlytics.core.data.local

import com.catlytics.core.data.model.PlaylistEntity
import com.catlytics.core.data.model.TrackEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InMemoryCatlyticsLocalDataSource @Inject constructor() : CatlyticsLocalDataSource {
    private val tracks = MutableStateFlow(emptyList<TrackEntity>())
    private val playlists = MutableStateFlow(seedPlaylists)

    override fun observeTracks() = tracks

    override fun observePlaylists() = playlists

    override suspend fun upsertTracks(tracks: List<TrackEntity>) {
        this.tracks.update { current ->
            (current.associateBy { it.id } + tracks.associateBy { it.id }).values.toList()
        }
    }

    override suspend fun replaceTracks(tracks: List<TrackEntity>) {
        this.tracks.value = tracks
    }

    override suspend fun upsertPlaylist(playlist: PlaylistEntity) {
        playlists.update { current ->
            (current.associateBy { it.id } + (playlist.id to playlist)).values.toList()
        }
    }

    private companion object {
        val seedPlaylists = listOf(
            PlaylistEntity(
                id = "playlist-focus",
                name = "Focus",
                trackIds = emptyList(),
            ),
        )
    }
}
