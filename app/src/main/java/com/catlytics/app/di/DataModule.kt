package com.catlytics.app.di

import com.catlytics.core.data.local.CatlyticsLocalDataSource
import com.catlytics.core.data.local.InMemoryCatlyticsLocalDataSource
import com.catlytics.core.data.mediator.CatlyticsDataMediator
import com.catlytics.core.data.mediator.OfflineFirstCatlyticsDataMediator
import com.catlytics.core.data.remote.CatlyticsRemoteDataSource
import com.catlytics.core.data.remote.NoOpCatlyticsRemoteDataSource
import com.catlytics.core.data.repository.DefaultStatisticsRepository
import com.catlytics.core.data.repository.OfflineFirstLibraryRepository
import com.catlytics.core.data.repository.OfflineFirstPlaylistRepository
import com.catlytics.core.domain.repository.LibraryRepository
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
    fun bindLocalDataSource(
        dataSource: InMemoryCatlyticsLocalDataSource,
    ): CatlyticsLocalDataSource

    @Binds
    fun bindRemoteDataSource(
        dataSource: NoOpCatlyticsRemoteDataSource,
    ): CatlyticsRemoteDataSource

    @Binds
    fun bindDataMediator(
        mediator: OfflineFirstCatlyticsDataMediator,
    ): CatlyticsDataMediator

    @Binds
    fun bindLibraryRepository(
        repository: OfflineFirstLibraryRepository,
    ): LibraryRepository

    @Binds
    fun bindPlaylistRepository(
        repository: OfflineFirstPlaylistRepository,
    ): PlaylistRepository

    @Binds
    fun bindStatisticsRepository(
        repository: DefaultStatisticsRepository,
    ): StatisticsRepository
}
