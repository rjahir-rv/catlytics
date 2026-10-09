package com.catlytics.core.data.local

import android.content.Context
import android.graphics.BitmapFactory
import android.media.MediaScannerConnection
import android.provider.MediaStore
import androidx.core.net.toUri
import com.kyant.taglib.Picture
import com.kyant.taglib.TagLib
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AudioTags(
    val title: String,
    val artist: String,
    val album: String?,
    val frontCover: ByteArray?,
)

interface AudioTagWriter {
    /** Writes [tags] into the file behind [mediaUri]; throws when the file can't be written. */
    suspend fun write(mediaUri: String, tags: AudioTags)
}

class TagLibAudioTagWriter @Inject constructor(
    @ApplicationContext private val context: Context,
) : AudioTagWriter {
    override suspend fun write(mediaUri: String, tags: AudioTags) = withContext(Dispatchers.IO) {
        val uri = mediaUri.toUri()
        val descriptor = checkNotNull(context.contentResolver.openFileDescriptor(uri, "rw")) {
            "No se pudo abrir el archivo."
        }
        descriptor.use { pfd ->
            // TagLib takes ownership of (and closes) each fd it receives, so hand it duplicates.
            val propertyMap = TagLib.getMetadata(pfd.dup().detachFd(), false)?.propertyMap
                ?: HashMap()
            propertyMap["TITLE"] = arrayOf(tags.title)
            propertyMap["ARTIST"] = arrayOf(tags.artist)
            if (tags.album != null) propertyMap["ALBUM"] = arrayOf(tags.album)
            check(TagLib.savePropertyMap(pfd.dup().detachFd(), propertyMap)) {
                "Formato no compatible para escribir etiquetas."
            }
            tags.frontCover?.let { cover ->
                val otherPictures = TagLib.getPictures(pfd.dup().detachFd())
                    .filterNot { it.pictureType == FRONT_COVER }
                val frontCover = Picture(cover, "", FRONT_COVER, cover.imageMimeType())
                check(
                    TagLib.savePictures(
                        pfd.dup().detachFd(),
                        (listOf(frontCover) + otherPictures).toTypedArray(),
                    ),
                ) { "No se pudo guardar la carátula en el archivo." }
            }
        }
        rescan(mediaUri)
    }

    private fun rescan(mediaUri: String) {
        @Suppress("DEPRECATION")
        val path = context.contentResolver.query(
            mediaUri.toUri(),
            arrayOf(MediaStore.Audio.Media.DATA),
            null,
            null,
            null,
        )?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
        if (path != null) MediaScannerConnection.scanFile(context, arrayOf(path), null, null)
    }

    private fun ByteArray.imageMimeType(): String {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(this, 0, size, options)
        return options.outMimeType ?: "image/jpeg"
    }

    private companion object {
        const val FRONT_COVER = "Front Cover"
    }
}
