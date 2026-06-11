package com.catlytics.core.data.local

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.os.Bundle
import android.os.Build
import android.provider.MediaStore
import com.catlytics.core.data.model.TrackEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
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
            MediaStoreAudioMapper.projection(),
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
            val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val trackNumberColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val isMusicColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.IS_MUSIC)
            val relativePathColumn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.RELATIVE_PATH)
            } else {
                -1
            }
            val volumeNameColumn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.VOLUME_NAME)
            } else {
                -1
            }
            @Suppress("DEPRECATION")
            val dataColumn = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            } else {
                -1
            }

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val mediaUri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    id,
                ).toString()
                MediaStoreAudioMapper.toTrackEntity(
                    id = id,
                    title = cursor.getString(titleColumn),
                    artist = cursor.getString(artistColumn),
                    artistId = cursor.getLong(artistIdColumn),
                    albumId = cursor.getLong(albumIdColumn),
                    album = cursor.getString(albumColumn),
                    trackNumber = cursor.getInt(trackNumberColumn),
                    durationMillis = cursor.getLong(durationColumn),
                    isMusic = cursor.getInt(isMusicColumn),
                    mediaUri = mediaUri,
                    folder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        MediaStoreAudioMapper.folderFromRelativePath(
                            volumeName = cursor.getString(volumeNameColumn),
                            relativePath = cursor.getString(relativePathColumn),
                        )
                    } else {
                        MediaStoreAudioMapper.folderFromAbsolutePath(
                            absolutePath = cursor.getString(dataColumn),
                        )
                    },
                )?.let(tracks::add)
            }
        }
        tracks
    }
}

internal object MediaStoreAudioMapper {
    fun projection(): Array<String> = buildList {
        add(MediaStore.Audio.Media._ID)
        add(MediaStore.Audio.Media.TITLE)
        add(MediaStore.Audio.Media.ARTIST)
        add(MediaStore.Audio.Media.ARTIST_ID)
        add(MediaStore.Audio.Media.ALBUM_ID)
        add(MediaStore.Audio.Media.ALBUM)
        add(MediaStore.Audio.Media.TRACK)
        add(MediaStore.Audio.Media.DURATION)
        add(MediaStore.Audio.Media.IS_MUSIC)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            add(MediaStore.Audio.Media.RELATIVE_PATH)
            add(MediaStore.Audio.Media.VOLUME_NAME)
        } else {
            @Suppress("DEPRECATION")
            add(MediaStore.Audio.Media.DATA)
        }
    }.toTypedArray()

    fun toTrackEntity(
        id: Long,
        title: String?,
        artist: String?,
        artistId: Long,
        albumId: Long,
        album: String? = null,
        trackNumber: Int = 0,
        durationMillis: Long,
        isMusic: Int,
        mediaUri: String,
        folder: MediaFolderMetadata? = null,
    ): TrackEntity? {
        if (isMusic == 0 || durationMillis <= 0L) return null

        val normalizedArtist = artist
            ?.takeUnless { it.isBlank() || it == UNKNOWN_ARTIST }
            ?: "Artista desconocido"
        val normalizedAlbum = album
            ?.takeUnless { it.isBlank() || it == UNKNOWN_ALBUM }
            ?: "Álbum desconocido"
        val normalizedAlbumId = if (albumId > 0L) {
            "mediastore-album-$albumId"
        } else {
            "mediastore-album-${normalizedArtist.normalizedIdPart()}-${normalizedAlbum.normalizedIdPart()}"
        }

        return TrackEntity(
            id = "mediastore-$id",
            title = title?.takeUnless { it.isBlank() } ?: "Cancion sin titulo",
            artistId = "mediastore-artist-$artistId",
            artistName = normalizedArtist,
            durationMillis = durationMillis,
            mediaUri = mediaUri,
            artworkUri = albumId.toArtworkUri(),
            albumId = normalizedAlbumId,
            albumTitle = normalizedAlbum,
            trackNumber = trackNumber
                .rem(TRACK_NUMBER_DISC_MULTIPLIER)
                .takeIf { it > 0 },
            folderId = folder?.id,
            folderName = folder?.name,
            folderPath = folder?.path,
        )
    }

    fun folderFromRelativePath(
        volumeName: String?,
        relativePath: String?,
    ): MediaFolderMetadata? {
        val normalizedPath = relativePath.normalizePath() ?: return null
        val normalizedVolume = volumeName?.takeUnless(String::isBlank) ?: "external"
        return MediaFolderMetadata(
            id = "$normalizedVolume:$normalizedPath",
            name = normalizedPath.substringAfterLast('/'),
            path = normalizedPath,
        )
    }

    fun folderFromAbsolutePath(absolutePath: String?): MediaFolderMetadata? {
        val parentPath = absolutePath
            ?.takeUnless(String::isBlank)
            ?.let(::File)
            ?.parent
            .normalizePath()
            ?: return null
        return MediaFolderMetadata(
            id = "external:$parentPath",
            name = parentPath.substringAfterLast('/'),
            path = parentPath,
        )
    }

    private const val UNKNOWN_ARTIST = "<unknown>"
    private const val UNKNOWN_ALBUM = "<unknown>"
    private const val TRACK_NUMBER_DISC_MULTIPLIER = 1_000
    private const val ARTWORK_BASE_URI = "content://media/external/audio/albumart"

    private fun Long.toArtworkUri(): String? = takeIf { it > 0L }
        ?.let { "$ARTWORK_BASE_URI/$it" }

    private fun String.normalizedIdPart(): String = lowercase()
        .replace(Regex("[^a-z0-9]+"), "-")
        .trim('-')
        .ifBlank { "unknown" }

    private fun String?.normalizePath(): String? = this
        ?.replace('\\', '/')
        ?.split('/')
        ?.filter(String::isNotBlank)
        ?.joinToString("/")
        ?.takeUnless(String::isBlank)
}

internal data class MediaFolderMetadata(
    val id: String,
    val name: String,
    val path: String,
)
