package com.catlytics.core.domain.repository

import com.catlytics.core.model.BulkEditResult
import com.catlytics.core.model.BulkTrackMetadataEdit
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

    /** Applies [edit] field by field to every track, keeping the fields it leaves as Keep. */
    suspend fun saveBulkEdit(trackIds: List<String>, edit: BulkTrackMetadataEdit): BulkEditResult

    suspend fun reset(trackId: String)

    suspend fun resetMany(trackIds: List<String>)

    /** Writes the edited title/artist/album/artwork into the audio file's tags. */
    suspend fun writeTagsToFile(trackId: String)

    /** Writes each track's tags; returns how many files could not be written. */
    suspend fun writeTagsToFiles(trackIds: List<String>): Int
}
