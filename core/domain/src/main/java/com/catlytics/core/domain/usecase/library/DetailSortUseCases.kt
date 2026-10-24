package com.catlytics.core.domain.usecase.library

import com.catlytics.core.domain.repository.LibraryPreferencesRepository
import com.catlytics.core.domain.repository.PlaybackEventRepository
import com.catlytics.core.model.AlbumTrackSort
import com.catlytics.core.model.ArtistAlbumSort
import com.catlytics.core.model.ArtistTrackSort
import com.catlytics.core.model.DetailSortSettings
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveDetailSortSettingsUseCase @Inject constructor(
    private val preferencesRepository: LibraryPreferencesRepository,
) {
    operator fun invoke(): Flow<DetailSortSettings> = preferencesRepository.observeDetailSortSettings()
}

class SetAlbumTrackSortUseCase @Inject constructor(
    private val preferencesRepository: LibraryPreferencesRepository,
) {
    suspend operator fun invoke(sort: AlbumTrackSort) = preferencesRepository.setAlbumTrackSort(sort)
}

class SetArtistTrackSortUseCase @Inject constructor(
    private val preferencesRepository: LibraryPreferencesRepository,
) {
    suspend operator fun invoke(sort: ArtistTrackSort) = preferencesRepository.setArtistTrackSort(sort)
}

class SetArtistAlbumSortUseCase @Inject constructor(
    private val preferencesRepository: LibraryPreferencesRepository,
) {
    suspend operator fun invoke(sort: ArtistAlbumSort) = preferencesRepository.setArtistAlbumSort(sort)
}

class ObserveTrackPlayCountsUseCase @Inject constructor(
    private val playbackEventRepository: PlaybackEventRepository,
) {
    operator fun invoke(): Flow<Map<String, Int>> = playbackEventRepository.observeTrackPlayCounts()
}
