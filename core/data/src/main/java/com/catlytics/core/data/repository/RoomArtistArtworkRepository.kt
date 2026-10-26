package com.catlytics.core.data.repository

import android.content.Context
import com.catlytics.core.data.local.ManagedImageStore
import com.catlytics.core.data.local.room.ArtistAliasDao
import com.catlytics.core.data.local.room.ArtistArtworkOverrideDao
import com.catlytics.core.data.local.room.ArtistArtworkOverrideEntity
import com.catlytics.core.domain.repository.ArtistArtworkRepository
import com.catlytics.core.model.Artist
import com.catlytics.core.model.ArtistArtworkOverride
import com.catlytics.core.model.artistIdentityKey
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

@Singleton
class RoomArtistArtworkRepository internal constructor(
    private val dao: ArtistArtworkOverrideDao,
    private val aliasDao: ArtistAliasDao,
    private val artworkStore: ManagedImageStore,
    private val clock: () -> Long = System::currentTimeMillis,
) : ArtistArtworkRepository {
    @Inject
    constructor(
        dao: ArtistArtworkOverrideDao,
        aliasDao: ArtistAliasDao,
        @ApplicationContext context: Context,
    ) : this(dao, aliasDao, ManagedImageStore(context, ARTWORK_DIRECTORY))

    override fun observeOverrides(): Flow<List<ArtistArtworkOverride>> = dao.observeAll()
        .map { overrides -> overrides.map(ArtistArtworkOverrideEntity::toDomain) }

    override suspend fun setArtwork(artist: Artist, sourceUri: String) = withContext(Dispatchers.IO) {
        val key = artistIdentityKey(artist.name)
        val path = checkNotNull(artworkStore.copyFrom(sourceUri, key)) { "No se pudo copiar la imagen." }
        val previous = dao.getByKey(key)
        dao.upsert(
            ArtistArtworkOverrideEntity(
                artistKey = key,
                artistName = artist.name,
                artworkUri = path,
                updatedAtMillis = clock(),
            ),
        )
        previous?.artworkUri?.takeIf { it != path }?.let(artworkStore::deleteManaged)
        Unit
    }

    override suspend fun restoreArtwork(artist: Artist) = withContext(Dispatchers.IO) {
        val key = artistIdentityKey(artist.name)
        removeKeys(listOf(key) + aliasDao.getSourceKeys(key))
    }

    override suspend fun removeArtwork(artistKey: String) = withContext(Dispatchers.IO) {
        removeKeys(listOf(artistKey))
    }

    private suspend fun removeKeys(keys: List<String>) {
        val existing = dao.getByKeys(keys.distinct())
        if (existing.isEmpty()) return
        dao.deleteByKeys(existing.map(ArtistArtworkOverrideEntity::artistKey))
        existing.forEach { artworkStore.deleteManaged(it.artworkUri) }
    }

    internal suspend fun exportForBackup(): List<ArtistArtworkDto> = withContext(Dispatchers.IO) {
        dao.getAll().mapNotNull { override ->
            val bytes = artworkStore.read(override.artworkUri, ManagedImageStore.MAX_IMAGE_BYTES)
                ?: return@mapNotNull null
            ArtistArtworkDto(
                artistName = override.artistName,
                artworkBase64 = Base64.getEncoder().encodeToString(bytes),
                updatedAtMillis = override.updatedAtMillis,
            )
        }
    }

    /** Writes the images of the file to the store; rows are applied by replace/merge later. */
    internal suspend fun prepareBackupRestore(
        dtos: List<ArtistArtworkDto>,
    ): List<ArtistArtworkOverrideEntity> = withContext(Dispatchers.IO) {
        dtos.mapNotNull { dto ->
            val key = artistIdentityKey(dto.artistName)
            val path = runCatching { Base64.getDecoder().decode(dto.artworkBase64) }.getOrNull()
                ?.takeIf { it.size in 1..ManagedImageStore.MAX_IMAGE_BYTES }
                ?.let { artworkStore.write(it, key) }
                ?: return@mapNotNull null
            ArtistArtworkOverrideEntity(
                artistKey = key,
                artistName = dto.artistName,
                artworkUri = path,
                updatedAtMillis = dto.updatedAtMillis,
            )
        }.distinctBy(ArtistArtworkOverrideEntity::artistKey)
    }

    internal suspend fun replaceFromBackup(overrides: List<ArtistArtworkOverrideEntity>) {
        dao.replaceAll(overrides)
    }

    /** Keeps the newest image of each artist and returns the number of images taken from the file. */
    internal suspend fun mergeFromBackup(overrides: List<ArtistArtworkOverrideEntity>): Int =
        overrides.count { incoming ->
            val existing = dao.getByKey(incoming.artistKey)
            val isNewer = existing == null || existing.updatedAtMillis < incoming.updatedAtMillis
            if (isNewer) dao.upsert(incoming)
            isNewer
        }

    internal suspend fun deleteUnreferencedArtwork() = withContext(Dispatchers.IO) {
        artworkStore.deleteAllExcept(dao.getAll().mapTo(HashSet(), ArtistArtworkOverrideEntity::artworkUri))
    }

    internal companion object {
        const val ARTWORK_DIRECTORY = "artist_artwork"
    }
}

internal fun ArtistArtworkOverrideEntity.toDomain() = ArtistArtworkOverride(
    artistKey = artistKey,
    artistName = artistName,
    artworkUri = artworkUri,
    updatedAtMillis = updatedAtMillis,
)
