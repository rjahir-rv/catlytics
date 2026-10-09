package com.catlytics.core.data.di

import android.content.Context
import androidx.room.Room
import com.catlytics.core.data.local.room.CatlyticsDatabase
import com.catlytics.core.data.local.room.PlaybackEventDao
import com.catlytics.core.data.local.room.ArtistAliasDao
import com.catlytics.core.data.local.room.TrackMetadataOverrideDao
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
            ).addMigrations(
                CatlyticsDatabase.MIGRATION_1_2,
                CatlyticsDatabase.MIGRATION_2_3,
                CatlyticsDatabase.MIGRATION_3_4,
            )
        .build()
    }

    @Provides
    @Singleton
    fun providePlaybackEventDao(
        database: CatlyticsDatabase
    ): PlaybackEventDao {
        return database.playbackEventDao()
    }

    @Provides
    @Singleton
    fun provideArtistAliasDao(database: CatlyticsDatabase): ArtistAliasDao =
        database.artistAliasDao()

    @Provides
    @Singleton
    fun provideTrackMetadataOverrideDao(database: CatlyticsDatabase): TrackMetadataOverrideDao =
        database.trackMetadataOverrideDao()
}
