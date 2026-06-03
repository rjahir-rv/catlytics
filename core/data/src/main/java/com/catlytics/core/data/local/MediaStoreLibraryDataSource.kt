package com.catlytics.core.data.local

import android.content.ContentResolver
import android.content.Context
import android.os.Bundle
import android.provider.MediaStore
import com.catlytics.core.data.model.TrackEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface MediaStoreLibraryDataSource {
    suspend fun loadTracks(): List<TrackEntity>
}

class AndroidMediaStoreLibraryDataSource @Inject constructor(
    @ApplicationContext context: Context,
) : MediaStoreLibraryDataSource {
    private val contentResolver = context.contentResolver

    override suspend fun loadTracks(): List<TrackEntity> = withContext(Dispatchers.IO) {
        val tracks = mutableListOf<TrackEntity>()
        contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            MediaStoreAudioMapper.projection,
            Bundle().apply {
                putStringArray(
                    ContentResolver.QUERY_ARG_SORT_COLUMNS,
                    arrayOf(MediaStore.Audio.Media.TITLE),
                )
                putInt(
                    ContentResolver.QUERY_ARG_SORT_DIRECTION,
                    ContentResolver.QUERY_SORT_DIRECTION_ASCENDING,
                )
            },
            null,
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val artistIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST_ID)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val isMusicColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.IS_MUSIC)

            while (cursor.moveToNext()) {
                MediaStoreAudioMapper.toTrackEntity(
                    id = cursor.getLong(idColumn),
                    title = cursor.getString(titleColumn),
                    artist = cursor.getString(artistColumn),
                    artistId = cursor.getLong(artistIdColumn),
                    durationMillis = cursor.getLong(durationColumn),
                    isMusic = cursor.getInt(isMusicColumn),
                )?.let(tracks::add)
            }
        }
        tracks
    }
}

internal object MediaStoreAudioMapper {
    val projection = arrayOf(
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.TITLE,
        MediaStore.Audio.Media.ARTIST,
        MediaStore.Audio.Media.ARTIST_ID,
        MediaStore.Audio.Media.DURATION,
        MediaStore.Audio.Media.IS_MUSIC,
    )

    fun toTrackEntity(
        id: Long,
        title: String?,
        artist: String?,
        artistId: Long,
        durationMillis: Long,
        isMusic: Int,
    ): TrackEntity? {
        if (isMusic == 0 || durationMillis <= 0L) return null

        val normalizedArtist = artist
            ?.takeUnless { it.isBlank() || it == UNKNOWN_ARTIST }
            ?: "Artista desconocido"

        return TrackEntity(
            id = "mediastore-$id",
            title = title?.takeUnless { it.isBlank() } ?: "Cancion sin titulo",
            artistId = "mediastore-artist-$artistId",
            artistName = normalizedArtist,
            durationMillis = durationMillis,
        )
    }

    private const val UNKNOWN_ARTIST = "<unknown>"
}
