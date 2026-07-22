package com.catlytics.core.domain.usecase.library

import com.catlytics.core.domain.repository.LibraryPreferencesRepository
import com.catlytics.core.model.MusicScanDurationFilter
import com.catlytics.core.model.MusicScanSizeFilter

class ObserveMusicScanSettingsUseCase(
    private val preferencesRepository: LibraryPreferencesRepository,
) {
    operator fun invoke() = preferencesRepository.observeMusicScanSettings()
}

class SetMusicScanDurationFilterUseCase(
    private val preferencesRepository: LibraryPreferencesRepository,
) {
    suspend operator fun invoke(filter: MusicScanDurationFilter) {
        preferencesRepository.setMusicScanDurationFilter(filter)
    }
}

class SetMusicScanSizeFilterUseCase(
    private val preferencesRepository: LibraryPreferencesRepository,
) {
    suspend operator fun invoke(filter: MusicScanSizeFilter) {
        preferencesRepository.setMusicScanSizeFilter(filter)
    }
}
