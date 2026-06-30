package com.catlytics.core.playback.di

import com.catlytics.core.domain.repository.EqualizerRepository
import com.catlytics.core.domain.repository.PlaybackController
import com.catlytics.core.playback.AndroidEqualizerRepository
import com.catlytics.core.playback.Media3PlaybackController
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface PlaybackModule {
    @Binds
    @Singleton
    fun bindPlaybackController(
        controller: Media3PlaybackController,
    ): PlaybackController

    @Binds
    @Singleton
    fun bindEqualizerRepository(
        repository: AndroidEqualizerRepository,
    ): EqualizerRepository
}
