package com.catlytics.core.widget

import com.catlytics.core.domain.repository.AppPreferencesRepository
import com.catlytics.core.domain.usecase.playback.RestorePlaybackSessionUseCase
import com.catlytics.core.domain.usecase.playback.SkipPlaybackUseCase
import com.catlytics.core.domain.usecase.playback.TogglePlaybackUseCase
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface WidgetEntryPoint {
    fun nowPlayingWidgetSync(): NowPlayingWidgetSync
    fun nowPlayingWidgetStore(): NowPlayingWidgetStore
    fun appPreferencesRepository(): AppPreferencesRepository
    fun restorePlaybackSessionUseCase(): RestorePlaybackSessionUseCase
    fun togglePlaybackUseCase(): TogglePlaybackUseCase
    fun skipPlaybackUseCase(): SkipPlaybackUseCase
}
