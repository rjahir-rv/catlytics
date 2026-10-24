package com.catlytics.app.di

import com.catlytics.core.domain.repository.HomePreferencesRepository
import com.catlytics.core.domain.repository.LibraryRepository
import com.catlytics.core.domain.repository.PlaybackController
import com.catlytics.core.domain.repository.PlaybackEventRepository
import com.catlytics.core.domain.usecase.home.GenerateDailyPlaylistUseCase
import com.catlytics.core.domain.usecase.library.ObserveRecentlyAddedTracksUseCase
import com.catlytics.core.domain.usecase.playback.PlayShuffledQueueUseCase
import com.catlytics.core.domain.usecase.playback.PlayTrackUseCase
import com.catlytics.core.domain.usecase.statistics.ObserveListeningStreakUseCase
import com.catlytics.core.domain.usecase.statistics.ObservePeriodStatsUseCase
import com.catlytics.core.domain.usecase.statistics.ObserveWeeklyStatsUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Casos de uso con parámetros por defecto (reloj, aleatoriedad, fecha), que Dagger no sabe
 * rellenar. El resto se inyecta con `@Inject constructor` y no necesita estar aquí.
 */
@Module
@InstallIn(SingletonComponent::class)
object DomainModule {
    @Provides
    fun provideGenerateDailyPlaylistUseCase() = GenerateDailyPlaylistUseCase()

    @Provides
    fun provideObserveRecentlyAddedTracksUseCase(
        libraryRepository: LibraryRepository,
        homePreferencesRepository: HomePreferencesRepository,
    ) = ObserveRecentlyAddedTracksUseCase(libraryRepository, homePreferencesRepository)

    @Provides
    fun provideObserveWeeklyStatsUseCase(
        playbackEventRepository: PlaybackEventRepository,
    ) = ObserveWeeklyStatsUseCase(playbackEventRepository)

    @Provides
    fun provideObservePeriodStatsUseCase(
        playbackEventRepository: PlaybackEventRepository,
    ) = ObservePeriodStatsUseCase(playbackEventRepository)

    @Provides
    fun provideObserveListeningStreakUseCase(
        playbackEventRepository: PlaybackEventRepository,
    ) = ObserveListeningStreakUseCase(playbackEventRepository)

    @Provides
    fun providePlayTrackUseCase(
        playbackController: PlaybackController,
    ) = PlayTrackUseCase(playbackController)

    @Provides
    fun providePlayShuffledQueueUseCase(
        playbackController: PlaybackController,
    ) = PlayShuffledQueueUseCase(playbackController)
}
