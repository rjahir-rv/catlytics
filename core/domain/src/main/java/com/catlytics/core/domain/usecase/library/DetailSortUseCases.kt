package com.catlytics.core.domain.usecase.library

import com.catlytics.core.domain.repository.LibraryPreferencesRepository
import com.catlytics.core.domain.repository.PlaybackEventRepository
import com.catlytics.core.model.AlbumTrackSort
import com.catlytics.core.model.ArtistAlbumSort
import com.catlytics.core.model.ArtistTrackSort
import com.catlytics.core.model.DetailSortSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ObserveDetailSortSettingsUseCase(
    private val preferencesRepository: LibraryPreferencesRepository,
) {
    operator fun invoke(): Flow<DetailSortSettings> = preferencesRepository.observeDetailSortSettings()
}

class SetAlbumTrackSortUseCase(
    private val preferencesRepository: LibraryPreferencesRepository,
) {
    suspend operator fun invoke(sort: AlbumTrackSort) = preferencesRepository.setAlbumTrackSort(sort)
}

class SetArtistTrackSortUseCase(
    private val preferencesRepository: LibraryPreferencesRepository,
) {
    suspend operator fun invoke(sort: ArtistTrackSort) = preferencesRepository.setArtistTrackSort(sort)
}

class SetArtistAlbumSortUseCase(
    private val preferencesRepository: LibraryPreferencesRepository,
) {
    suspend operator fun invoke(sort: ArtistAlbumSort) = preferencesRepository.setArtistAlbumSort(sort)
}

class ObserveTrackPlayCountsUseCase(
    private val playbackEventRepository: PlaybackEventRepository,
) {
    operator fun invoke(): Flow<Map<String, Int>> = playbackEventRepository
        .observeTopTracks(startMillis = 0L, endMillis = Long.MAX_VALUE, limit = Int.MAX_VALUE)
        .map { tracks -> tracks.associate { it.trackId to it.playCount } }
}
