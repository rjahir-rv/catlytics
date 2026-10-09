package com.catlytics.core.domain.repository

import com.catlytics.core.model.Track
import com.catlytics.core.model.TrackMetadataEdit
import com.catlytics.core.model.TrackMetadataOverride
import kotlinx.coroutines.flow.Flow

interface TrackMetadataRepository {
    fun observeOverrides(): Flow<List<TrackMetadataOverride>>

    /** The track exactly as scanned from MediaStore, without edits or aliases. */
    fun observeOriginalTrack(trackId: String): Flow<Track?>

    /** Stores [edit] as an override; values equal to the scanned ones are not overridden. */
    suspend fun saveEdit(trackId: String, edit: TrackMetadataEdit)

    suspend fun reset(trackId: String)

    /** Writes the edited title/artist/album/artwork into the audio file's tags. */
    suspend fun writeTagsToFile(trackId: String)
}
