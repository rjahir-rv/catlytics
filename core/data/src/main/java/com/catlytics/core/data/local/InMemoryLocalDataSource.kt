package com.catlytics.core.data.local

import com.catlytics.core.data.model.TrackEntity
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InMemoryLocalDataSource @Inject constructor() : LocalDataSource {
    private val tracks = MutableStateFlow(emptyList<TrackEntity>())

    override fun observeTracks() = tracks

    override suspend fun replaceTracks(tracks: List<TrackEntity>) {
        this.tracks.value = tracks
    }
}
