package com.catlytics.app.di

import com.catlytics.core.domain.repository.PlaybackController
import com.catlytics.core.domain.repository.LibraryRepository
import com.catlytics.core.domain.repository.PlaylistRepository
import com.catlytics.core.domain.repository.StatisticsRepository
import com.catlytics.core.domain.usecase.playback.AddQueueItemUseCase
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
import com.catlytics.core.domain.usecase.playlist.AddToPlaylistUseCase
import com.catlytics.core.domain.usecase.playlist.CreatePlaylistUseCase
import com.catlytics.core.domain.usecase.playlist.DeletePlaylistUseCase
import com.catlytics.core.domain.usecase.playlist.ObserveIsTrackLikedUseCase
import com.catlytics.core.domain.usecase.playlist.ObservePlaylistContentUseCase
import com.catlytics.core.domain.usecase.playlist.ObservePlaylistViewModeUseCase
import com.catlytics.core.domain.usecase.playlist.RemoveTrackFromPlaylistUseCase
import com.catlytics.core.domain.usecase.playlist.RenamePlaylistUseCase
import com.catlytics.core.domain.usecase.playlist.ResolvePlaylistSourcePreviewUseCase
import com.catlytics.core.domain.usecase.playlist.SetPlaylistCoverUseCase
import com.catlytics.core.domain.usecase.playlist.SetPlaylistViewModeUseCase
import com.catlytics.core.domain.usecase.playlist.ToggleLikedTrackUseCase
import com.catlytics.core.domain.usecase.playback.MoveQueueItemUseCase
import com.catlytics.core.domain.usecase.playback.PlayQueueItemUseCase
import com.catlytics.core.domain.usecase.playback.RemoveQueueItemUseCase
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
    fun provideObservePlaylistViewModeUseCase(
        preferencesRepository: LibraryPreferencesRepository,
    ) = ObservePlaylistViewModeUseCase(preferencesRepository)

    @Provides
    fun provideSetPlaylistViewModeUseCase(
        preferencesRepository: LibraryPreferencesRepository,
    ) = SetPlaylistViewModeUseCase(preferencesRepository)

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
    fun provideObservePlaylistContentUseCase(
        playlistRepository: PlaylistRepository,
        libraryRepository: LibraryRepository,
    ) = ObservePlaylistContentUseCase(playlistRepository, libraryRepository)

    @Provides
    fun provideCreatePlaylistUseCase(repository: PlaylistRepository) = CreatePlaylistUseCase(repository)

    @Provides
    fun provideRenamePlaylistUseCase(repository: PlaylistRepository) = RenamePlaylistUseCase(repository)

    @Provides
    fun provideDeletePlaylistUseCase(repository: PlaylistRepository) = DeletePlaylistUseCase(repository)

    @Provides
    fun provideAddToPlaylistUseCase(
        playlistRepository: PlaylistRepository,
        libraryRepository: LibraryRepository,
    ) = AddToPlaylistUseCase(playlistRepository, libraryRepository)

    @Provides
    fun provideResolvePlaylistSourcePreviewUseCase(
        libraryRepository: LibraryRepository,
    ) = ResolvePlaylistSourcePreviewUseCase(libraryRepository)

    @Provides
    fun provideToggleLikedTrackUseCase(repository: PlaylistRepository) =
        ToggleLikedTrackUseCase(repository)

    @Provides
    fun provideObserveIsTrackLikedUseCase(repository: PlaylistRepository) =
        ObserveIsTrackLikedUseCase(repository)

    @Provides
    fun provideRemoveTrackFromPlaylistUseCase(repository: PlaylistRepository) =
        RemoveTrackFromPlaylistUseCase(repository)

    @Provides
    fun provideSetPlaylistCoverUseCase(repository: PlaylistRepository) =
        SetPlaylistCoverUseCase(repository)

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
    fun provideAddQueueItemUseCase(
        playbackController: PlaybackController,
    ) = AddQueueItemUseCase(playbackController)

    @Provides
    fun provideMoveQueueItemUseCase(
        playbackController: PlaybackController,
    ) = MoveQueueItemUseCase(playbackController)

    @Provides
    fun provideRemoveQueueItemUseCase(
        playbackController: PlaybackController,
    ) = RemoveQueueItemUseCase(playbackController)

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
