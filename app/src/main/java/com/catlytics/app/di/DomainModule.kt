package com.catlytics.app.di

import com.catlytics.core.domain.repository.PlaybackController
import com.catlytics.core.domain.repository.LibraryRepository
import com.catlytics.core.domain.repository.PlaylistRepository
import com.catlytics.core.domain.repository.StatisticsRepository
import com.catlytics.core.domain.usecase.playback.CycleRepeatModeUseCase
import com.catlytics.core.domain.usecase.library.ObserveLibraryUseCase
import com.catlytics.core.domain.usecase.library.ObserveAlbumsUseCase
import com.catlytics.core.domain.usecase.library.ObserveAlbumContentUseCase
import com.catlytics.core.domain.usecase.library.ObserveArtistContentUseCase
import com.catlytics.core.domain.usecase.library.ObserveArtistsUseCase
import com.catlytics.core.domain.usecase.library.ObserveArtistViewModeUseCase
import com.catlytics.core.domain.usecase.library.ObserveLibraryFoldersUseCase
import com.catlytics.core.domain.usecase.library.ObserveFolderContentUseCase
import com.catlytics.core.domain.usecase.statistics.ObserveListeningStatsUseCase
import com.catlytics.core.domain.usecase.playback.ObservePlaybackStateUseCase
import com.catlytics.core.domain.usecase.playlist.ObservePlaylistsUseCase
import com.catlytics.core.domain.usecase.playback.MoveQueueItemUseCase
import com.catlytics.core.domain.usecase.playback.PlayQueueItemUseCase
import com.catlytics.core.domain.usecase.playback.PlayTrackUseCase
import com.catlytics.core.domain.usecase.library.RefreshLibraryUseCase
import com.catlytics.core.domain.usecase.playback.RestorePlaybackSessionUseCase
import com.catlytics.core.domain.usecase.playback.SeekPlaybackUseCase
import com.catlytics.core.domain.usecase.library.SetFolderVisibilityUseCase
import com.catlytics.core.domain.usecase.library.SetArtistViewModeUseCase
import com.catlytics.core.domain.repository.LibraryPreferencesRepository
import com.catlytics.core.domain.usecase.playback.SkipPlaybackUseCase
import com.catlytics.core.domain.usecase.playback.ToggleShuffleUseCase
import com.catlytics.core.domain.usecase.playback.TogglePlaybackUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object DomainModule {
    @Provides
    fun provideObserveAlbumContentUseCase(
        libraryRepository: LibraryRepository,
    ) = ObserveAlbumContentUseCase(libraryRepository)

    @Provides
    fun provideObserveAlbumsUseCase(
        libraryRepository: LibraryRepository,
    ) = ObserveAlbumsUseCase(libraryRepository)

    @Provides
    fun provideObserveArtistsUseCase(
        libraryRepository: LibraryRepository,
    ) = ObserveArtistsUseCase(libraryRepository)

    @Provides
    fun provideObserveArtistContentUseCase(
        libraryRepository: LibraryRepository,
    ) = ObserveArtistContentUseCase(libraryRepository)

    @Provides
    fun provideObserveArtistViewModeUseCase(
        preferencesRepository: LibraryPreferencesRepository,
    ) = ObserveArtistViewModeUseCase(preferencesRepository)

    @Provides
    fun provideSetArtistViewModeUseCase(
        preferencesRepository: LibraryPreferencesRepository,
    ) = SetArtistViewModeUseCase(preferencesRepository)

    @Provides
    fun provideObserveLibraryUseCase(
        libraryRepository: LibraryRepository,
    ) = ObserveLibraryUseCase(libraryRepository)

    @Provides
    fun provideObserveLibraryFoldersUseCase(
        libraryRepository: LibraryRepository,
    ) = ObserveLibraryFoldersUseCase(libraryRepository)

    @Provides
    fun provideObserveFolderContentUseCase(
        libraryRepository: LibraryRepository,
    ) = ObserveFolderContentUseCase(libraryRepository)

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
    fun providePlayQueueItemUseCase(
        playbackController: PlaybackController,
    ) = PlayQueueItemUseCase(playbackController)

    @Provides
    fun provideMoveQueueItemUseCase(
        playbackController: PlaybackController,
    ) = MoveQueueItemUseCase(playbackController)

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
