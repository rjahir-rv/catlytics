package com.catlytics.core.data.repository

import android.content.Context
import com.catlytics.core.data.local.AudioTagWriter
import com.catlytics.core.data.local.AudioTags
import com.catlytics.core.data.local.InMemoryLocalDataSource
import com.catlytics.core.data.local.ManagedImageStore
import com.catlytics.core.data.local.room.CatlyticsDatabase

internal fun metadataRepository(
    context: Context,
    database: CatlyticsDatabase,
    localDataSource: InMemoryLocalDataSource = InMemoryLocalDataSource(),
    clock: () -> Long = { 1L },
) = RoomTrackMetadataRepository(
    dao = database.trackMetadataOverrideDao(),
    localDataSource = localDataSource,
    tagWriter = object : AudioTagWriter {
        override suspend fun write(mediaUri: String, tags: AudioTags) = Unit
    },
    artworkStore = ManagedImageStore(context, RoomTrackMetadataRepository.ARTWORK_DIRECTORY),
    clock = clock,
)
