package com.catlytics.core.domain.usecase.library

import com.catlytics.core.domain.repository.LibraryPreferencesRepository
import com.catlytics.core.model.MusicScanDurationFilter
import com.catlytics.core.model.MusicScanSizeFilter
import javax.inject.Inject

class ObserveMusicScanSettingsUseCase @Inject constructor(
    private val preferencesRepository: LibraryPreferencesRepository,
) {
    operator fun invoke() = preferencesRepository.observeMusicScanSettings()
}

class SetMusicScanDurationFilterUseCase @Inject constructor(
    private val preferencesRepository: LibraryPreferencesRepository,
) {
    suspend operator fun invoke(filter: MusicScanDurationFilter) {
        preferencesRepository.setMusicScanDurationFilter(filter)
    }
}

class SetMusicScanSizeFilterUseCase @Inject constructor(
    private val preferencesRepository: LibraryPreferencesRepository,
) {
    suspend operator fun invoke(filter: MusicScanSizeFilter) {
        preferencesRepository.setMusicScanSizeFilter(filter)
    }
}
