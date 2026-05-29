package com.catlytics.app.di

import com.catlytics.core.domain.repository.LibraryRepository
import com.catlytics.core.domain.repository.PlaylistRepository
import com.catlytics.core.domain.repository.StatisticsRepository
import com.catlytics.core.domain.usecase.ObserveLibraryUseCase
import com.catlytics.core.domain.usecase.ObserveListeningStatsUseCase
import com.catlytics.core.domain.usecase.ObservePlaylistsUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object DomainModule {
    @Provides
    fun provideObserveLibraryUseCase(
        libraryRepository: LibraryRepository,
    ) = ObserveLibraryUseCase(libraryRepository)

    @Provides
    fun provideObservePlaylistsUseCase(
        playlistRepository: PlaylistRepository,
    ) = ObservePlaylistsUseCase(playlistRepository)

    @Provides
    fun provideObserveListeningStatsUseCase(
        statisticsRepository: StatisticsRepository,
    ) = ObserveListeningStatsUseCase(statisticsRepository)
}
