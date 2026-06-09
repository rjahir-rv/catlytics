package com.catlytics.app.di

import com.catlytics.core.domain.repository.PlaybackController
import com.catlytics.core.domain.repository.LibraryRepository
import com.catlytics.core.domain.repository.PlaylistRepository
import com.catlytics.core.domain.repository.StatisticsRepository
import com.catlytics.core.domain.usecase.CycleRepeatModeUseCase
import com.catlytics.core.domain.usecase.ObserveLibraryUseCase
import com.catlytics.core.domain.usecase.ObserveLibraryFoldersUseCase
import com.catlytics.core.domain.usecase.ObserveListeningStatsUseCase
import com.catlytics.core.domain.usecase.ObservePlaybackStateUseCase
import com.catlytics.core.domain.usecase.ObservePlaylistsUseCase
import com.catlytics.core.domain.usecase.PlayTrackUseCase
import com.catlytics.core.domain.usecase.RefreshLibraryUseCase
import com.catlytics.core.domain.usecase.RestorePlaybackSessionUseCase
import com.catlytics.core.domain.usecase.SeekPlaybackUseCase
import com.catlytics.core.domain.usecase.SetFolderVisibilityUseCase
import com.catlytics.core.domain.usecase.SkipPlaybackUseCase
import com.catlytics.core.domain.usecase.ToggleShuffleUseCase
import com.catlytics.core.domain.usecase.TogglePlaybackUseCase
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
    fun provideObserveLibraryFoldersUseCase(
        libraryRepository: LibraryRepository,
    ) = ObserveLibraryFoldersUseCase(libraryRepository)

    @Provides
    fun provideRefreshLibraryUseCase(
        libraryRepository: LibraryRepository,
    ) = RefreshLibraryUseCase(libraryRepository)

    @Provides
    fun provideSetFolderVisibilityUseCase(
        libraryRepository: LibraryRepository,
    ) = SetFolderVisibilityUseCase(libraryRepository)

    @Provides
    fun provideObservePlaylistsUseCase(
        playlistRepository: PlaylistRepository,
    ) = ObservePlaylistsUseCase(playlistRepository)

    @Provides
    fun provideObserveListeningStatsUseCase(
        statisticsRepository: StatisticsRepository,
    ) = ObserveListeningStatsUseCase(statisticsRepository)

    @Provides
    fun provideObservePlaybackStateUseCase(
        playbackController: PlaybackController,
    ) = ObservePlaybackStateUseCase(playbackController)

    @Provides
    fun providePlayTrackUseCase(
        playbackController: PlaybackController,
    ) = PlayTrackUseCase(playbackController)

    @Provides
    fun provideTogglePlaybackUseCase(
        playbackController: PlaybackController,
    ) = TogglePlaybackUseCase(playbackController)

    @Provides
    fun provideSeekPlaybackUseCase(
        playbackController: PlaybackController,
    ) = SeekPlaybackUseCase(playbackController)

    @Provides
    fun provideSkipPlaybackUseCase(
        playbackController: PlaybackController,
    ) = SkipPlaybackUseCase(playbackController)

    @Provides
    fun provideToggleShuffleUseCase(
        playbackController: PlaybackController,
    ) = ToggleShuffleUseCase(playbackController)

    @Provides
    fun provideCycleRepeatModeUseCase(
        playbackController: PlaybackController,
    ) = CycleRepeatModeUseCase(playbackController)

    @Provides
    fun provideRestorePlaybackSessionUseCase(
        playbackController: PlaybackController,
    ) = RestorePlaybackSessionUseCase(playbackController)
}
