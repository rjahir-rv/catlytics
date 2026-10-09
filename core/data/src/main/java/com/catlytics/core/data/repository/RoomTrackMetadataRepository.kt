package com.catlytics.core.data.repository

import android.content.Context
import com.catlytics.core.data.local.AudioTagWriter
import com.catlytics.core.data.local.AudioTags
import com.catlytics.core.data.local.LocalDataSource
import com.catlytics.core.data.local.ManagedImageStore
import com.catlytics.core.data.local.room.TrackMetadataOverrideDao
import com.catlytics.core.data.local.room.TrackMetadataOverrideEntity
import com.catlytics.core.data.model.TrackEntity
import com.catlytics.core.data.model.toDomain
import com.catlytics.core.domain.repository.TrackMetadataRepository
import com.catlytics.core.model.ArtworkEdit
import com.catlytics.core.model.Track
import com.catlytics.core.model.TrackMetadataEdit
import com.catlytics.core.model.TrackMetadataOverride
import com.catlytics.core.model.artistIdentityKey
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

@Singleton
class RoomTrackMetadataRepository internal constructor(
    private val dao: TrackMetadataOverrideDao,
    private val localDataSource: LocalDataSource,
    private val tagWriter: AudioTagWriter,
    private val artworkStore: ManagedImageStore,
    private val clock: () -> Long = System::currentTimeMillis,
) : TrackMetadataRepository {
    @Inject
    constructor(
        dao: TrackMetadataOverrideDao,
        localDataSource: LocalDataSource,
        tagWriter: AudioTagWriter,
        @ApplicationContext context: Context,
    ) : this(dao, localDataSource, tagWriter, ManagedImageStore(context, ARTWORK_DIRECTORY))

    override fun observeOverrides(): Flow<List<TrackMetadataOverride>> = dao.observeAll()
        .map { overrides -> overrides.map(TrackMetadataOverrideEntity::toDomain) }

    override fun observeOriginalTrack(trackId: String): Flow<Track?> = localDataSource
        .observeTracks()
        .map { tracks -> tracks.firstOrNull { it.id == trackId }?.toDomain() }
        .distinctUntilChanged()

    override suspend fun saveEdit(trackId: String, edit: TrackMetadataEdit) =
        withContext(Dispatchers.IO) {
            val tracks = localDataSource.observeTracks().first()
            val original = checkNotNull(tracks.firstOrNull { it.id == trackId }) {
                "La canción ya no está en la biblioteca."
            }
            val existing = dao.getByTrackId(trackId)
            val artworkUri = when (val artwork = edit.artwork) {
                ArtworkEdit.Keep -> existing?.artworkUri
                ArtworkEdit.Restore -> null
                is ArtworkEdit.Replace -> checkNotNull(artworkStore.copyFrom(artwork.sourceUri, trackId)) {
                    "No se pudo copiar la carátula."
                }
            }
            if (artworkUri != existing?.artworkUri) artworkStore.delete(trackId, existing?.artworkUri)

            val override = TrackMetadataOverride(
                trackId = trackId,
                fileKey = original.fileKey,
                title = edit.title.overriding(original.title),
                artistName = edit.artistName.overriding(original.artistName),
                albumTitle = edit.albumTitle.overriding(original.albumTitle),
                artworkUri = artworkUri,
                originalTitle = original.title,
                originalArtistName = original.artistName,
                originalAlbumTitle = original.albumTitle,
                durationMillis = original.durationMillis,
                updatedAtMillis = clock(),
            )
            if (override.isEmpty) {
                dao.delete(trackId)
            } else {
                dao.upsertReplacingFileKey(override.toEntity(OverrideIdResolver(tracks), original))
            }
        }

    override suspend fun reset(trackId: String) = withContext(Dispatchers.IO) {
        val existing = dao.getByTrackId(trackId) ?: return@withContext
        artworkStore.delete(trackId, existing.artworkUri)
        dao.delete(trackId)
    }

    override suspend fun writeTagsToFile(trackId: String) = withContext(Dispatchers.IO) {
        val original = checkNotNull(
            localDataSource.observeTracks().first().firstOrNull { it.id == trackId },
        ) { "La canción ya no está en la biblioteca." }
        val override = dao.getByTrackId(trackId)
        tagWriter.write(
            mediaUri = original.mediaUri,
            tags = AudioTags(
                title = override?.title ?: original.title,
                artist = override?.artistName ?: original.artistName,
                album = override?.albumTitle ?: original.albumTitle,
                frontCover = artworkStore.read(override?.artworkUri, MAX_ARTWORK_BYTES),
            ),
        )
    }

    /** Re-points overrides whose MediaStore id changed (reindex) to the track with the same file. */
    internal suspend fun reconcile(tracks: List<TrackEntity>) = withContext(Dispatchers.IO) {
        val trackIds = tracks.mapTo(HashSet(), TrackEntity::id)
        val trackIdsByFileKey = tracks
            .mapNotNull { track -> track.fileKey?.let { it to track.id } }
            .toMap()
        val moves = dao.getAll().mapNotNull { override ->
            if (override.trackId in trackIds) return@mapNotNull null
            val newTrackId = override.fileKey?.let(trackIdsByFileKey::get) ?: return@mapNotNull null
            override.trackId to newTrackId
        }
        if (moves.isNotEmpty()) dao.moveTrackIds(moves)
    }

    internal suspend fun exportForBackup(): List<TrackMetadataOverrideDto> =
        withContext(Dispatchers.IO) {
            dao.getAll().map { override ->
                TrackMetadataOverrideDto(
                    trackId = override.trackId,
                    fileKey = override.fileKey,
                    title = override.title,
                    artistName = override.artistName,
                    albumTitle = override.albumTitle,
                    artworkBase64 = artworkStore.read(override.artworkUri, MAX_ARTWORK_BYTES)
                        ?.let(Base64.getEncoder()::encodeToString),
                    originalTitle = override.originalTitle,
                    originalArtistName = override.originalArtistName,
                    originalAlbumTitle = override.originalAlbumTitle,
                    durationMillis = override.durationMillis,
                    updatedAtMillis = override.updatedAtMillis,
                )
            }
        }

    internal suspend fun prepareBackupRestore(
        dtos: List<TrackMetadataOverrideDto>,
    ): List<TrackMetadataOverrideEntity> = withContext(Dispatchers.IO) {
        val tracks = localDataSource.observeTracks().first()
        val tracksByFileKey = tracks
            .mapNotNull { track -> track.fileKey?.let { it to track } }
            .toMap()
        val reconciler = TrackReconciler(tracks)
        val resolver = OverrideIdResolver(tracks)
        dtos.filter(TrackMetadataOverrideDto::hasEdits).map { dto ->
            val track = dto.fileKey?.let(tracksByFileKey::get)
                ?: reconciler.reconcile(
                    PlaylistTrackBackupDto(
                        trackId = dto.trackId,
                        title = dto.originalTitle,
                        artistName = dto.originalArtistName,
                        albumTitle = dto.originalAlbumTitle,
                        durationMillis = dto.durationMillis,
                    ),
                )
            val trackId = track?.id ?: dto.trackId
            val artworkUri = dto.artworkBase64
                ?.let { runCatching { Base64.getDecoder().decode(it) }.getOrNull() }
                ?.takeIf { it.size in 1..MAX_ARTWORK_BYTES }
                ?.let { artworkStore.write(it, trackId) }
            TrackMetadataOverride(
                trackId = trackId,
                fileKey = track?.fileKey ?: dto.fileKey,
                title = dto.title,
                artistName = dto.artistName,
                albumTitle = dto.albumTitle,
                artworkUri = artworkUri,
                originalTitle = dto.originalTitle,
                originalArtistName = dto.originalArtistName,
                originalAlbumTitle = dto.originalAlbumTitle,
                durationMillis = dto.durationMillis,
                updatedAtMillis = dto.updatedAtMillis,
            ).toEntity(resolver, track)
        }.distinctBy(TrackMetadataOverrideEntity::trackId)
    }

    /** Must run after the restore's database transaction, never inside it, since it deletes files. */
    internal suspend fun deleteUnreferencedArtwork() = withContext(Dispatchers.IO) {
        val referenced = dao.getAll().mapNotNullTo(HashSet(), TrackMetadataOverrideEntity::artworkUri)
        artworkStore.deleteAllExcept(referenced)
    }

    internal suspend fun replaceFromBackup(overrides: List<TrackMetadataOverrideEntity>) {
        dao.replaceAll(overrides)
    }

    /** Keeps the most recently edited version of each track; returns how many were written. */
    internal suspend fun mergeFromBackup(overrides: List<TrackMetadataOverrideEntity>): Int {
        var imported = 0
        overrides.forEach { incoming ->
            val existing = dao.getByTrackId(incoming.trackId)
                ?: incoming.fileKey?.let { dao.getByFileKey(it) }
            if (existing != null && existing.updatedAtMillis >= incoming.updatedAtMillis) {
                return@forEach
            }
            dao.upsertReplacingFileKey(incoming)
            imported++
        }
        return imported
    }

    internal companion object {
        const val ARTWORK_DIRECTORY = "track_artwork"
        const val MAX_ARTWORK_BYTES = 2L * 1024 * 1024
    }
}

private fun String.overriding(original: String?): String? =
    trim().takeUnless { it.isEmpty() || it == original }

internal fun TrackMetadataOverrideEntity.toDomain() = TrackMetadataOverride(
    trackId = trackId,
    fileKey = fileKey,
    title = title,
    artistName = artistName,
    albumTitle = albumTitle,
    artworkUri = artworkUri,
    originalTitle = originalTitle,
    originalArtistName = originalArtistName,
    originalAlbumTitle = originalAlbumTitle,
    durationMillis = durationMillis,
    updatedAtMillis = updatedAtMillis,
)

internal fun TrackMetadataOverride.toEntity(
    resolver: OverrideIdResolver,
    original: TrackEntity?,
): TrackMetadataOverrideEntity {
    val effectiveArtistName = artistName ?: original?.artistName ?: originalArtistName
    return TrackMetadataOverrideEntity(
        trackId = trackId,
        fileKey = fileKey,
        title = title,
        artistName = artistName,
        artistKey = artistName?.let(::artistIdentityKey),
        artistId = artistName?.let(resolver::artistId),
        albumTitle = albumTitle,
        albumId = albumTitle?.let { resolver.albumId(effectiveArtistName, it) },
        artworkUri = artworkUri,
        originalTitle = originalTitle,
        originalArtistName = originalArtistName,
        originalAlbumTitle = originalAlbumTitle,
        durationMillis = durationMillis,
        updatedAtMillis = updatedAtMillis,
    )
}
