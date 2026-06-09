package com.catlytics.app.di

import com.catlytics.core.data.local.AndroidMediaStoreLibraryDataSource
import com.catlytics.core.data.local.InMemoryLocalDataSource
import com.catlytics.core.data.local.LocalDataSource
import com.catlytics.core.data.local.MediaStoreLibraryDataSource
import com.catlytics.core.data.mediator.DataMediator
import com.catlytics.core.data.mediator.OfflineFirstDataMediator
import com.catlytics.core.data.remote.NoOpRemoteDataSource
import com.catlytics.core.data.remote.RemoteDataSource
import com.catlytics.core.data.repository.DefaultStatisticsRepository
import com.catlytics.core.data.repository.DataStoreAppPreferencesRepository
import com.catlytics.core.data.repository.DataStoreLibraryPreferencesRepository
import com.catlytics.core.data.repository.DataStorePlaybackSessionRepository
import com.catlytics.core.data.repository.OfflineFirstLibraryRepository
import com.catlytics.core.data.repository.OfflineFirstPlaylistRepository
import com.catlytics.core.domain.repository.AppPreferencesRepository
import com.catlytics.core.domain.repository.LibraryRepository
import com.catlytics.core.domain.repository.LibraryPreferencesRepository
import com.catlytics.core.domain.repository.PlaybackSessionRepository
import com.catlytics.core.domain.repository.PlaylistRepository
import com.catlytics.core.domain.repository.StatisticsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface DataModule {
    @Binds
    @Singleton
    fun bindAppPreferencesRepository(
        repository: DataStoreAppPreferencesRepository,
    ): AppPreferencesRepository

    @Binds
    @Singleton
    fun bindLibraryPreferencesRepository(
        repository: DataStoreLibraryPreferencesRepository,
    ): LibraryPreferencesRepository

    @Binds
    @Singleton
    fun bindLocalDataSource(
        dataSource: InMemoryLocalDataSource,
    ): LocalDataSource

    @Binds
    fun bindMediaStoreLibraryDataSource(
        dataSource: AndroidMediaStoreLibraryDataSource,
    ): MediaStoreLibraryDataSource

    @Binds
    fun bindRemoteDataSource(
        dataSource: NoOpRemoteDataSource,
    ): RemoteDataSource

    @Binds
    fun bindDataMediator(
        mediator: OfflineFirstDataMediator,
    ): DataMediator

    @Binds
    fun bindLibraryRepository(
        repository: OfflineFirstLibraryRepository,
    ): LibraryRepository

    @Binds
    fun bindPlaylistRepository(
        repository: OfflineFirstPlaylistRepository,
    ): PlaylistRepository

    @Binds
    @Singleton
    fun bindPlaybackSessionRepository(
        repository: DataStorePlaybackSessionRepository,
    ): PlaybackSessionRepository

    @Binds
    fun bindStatisticsRepository(
        repository: DefaultStatisticsRepository,
    ): StatisticsRepository
}
