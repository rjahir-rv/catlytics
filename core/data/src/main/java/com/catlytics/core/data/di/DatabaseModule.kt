package com.catlytics.core.data.di

import android.content.Context
import androidx.room.Room
import com.catlytics.core.data.local.room.CatlyticsDatabase
import com.catlytics.core.data.local.room.PlaybackEventDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): CatlyticsDatabase {
        return Room.databaseBuilder(
                context,
                CatlyticsDatabase::class.java,
                "catlytics.db"
            ).fallbackToDestructiveMigration(false) // fallback to destructive migration for safety during initial dev/testing
        .build()
    }

    @Provides
    @Singleton
    fun providePlaybackEventDao(
        database: CatlyticsDatabase
    ): PlaybackEventDao {
        return database.playbackEventDao()
    }
}
