package com.catlytics.core.data.repository

import androidx.room.withTransaction
import com.catlytics.core.data.local.room.ArtistAliasDao
import com.catlytics.core.data.local.room.ArtistAliasEntity
import com.catlytics.core.data.local.room.CatlyticsDatabase
import com.catlytics.core.domain.repository.ArtistIdentityRepository
import com.catlytics.core.model.Artist
import com.catlytics.core.model.ArtistAlias
import com.catlytics.core.model.artistIdentityKey
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class RoomArtistIdentityRepository @Inject constructor(
    private val database: CatlyticsDatabase,
    private val dao: ArtistAliasDao,
) : ArtistIdentityRepository {
    override fun observeAliases(): Flow<List<ArtistAlias>> = dao.observeAll()
        .map { aliases -> aliases.map(ArtistAliasEntity::toDomain) }

    override suspend fun getAliases(): List<ArtistAlias> = withContext(Dispatchers.IO) {
        dao.getAll().map(ArtistAliasEntity::toDomain)
    }

    override suspend fun merge(source: Artist, target: Artist) = withContext(Dispatchers.IO) {
        val sourceKey = artistIdentityKey(source.name)
        val requestedTargetKey = artistIdentityKey(target.name)
        require(sourceKey != requestedTargetKey) { "Los artistas ya pertenecen al mismo grupo." }

        database.withTransaction {
            val resolvedTarget = dao.getBySourceKey(requestedTargetKey)
            val targetArtist = resolvedTarget?.let {
                Artist(it.targetArtistId, it.targetArtistName)
            } ?: target
            val targetKey = artistIdentityKey(targetArtist.name)
            require(sourceKey != targetKey) { "La fusión produciría un ciclo." }

            dao.moveGroup(
                sourceKey = sourceKey,
                targetKey = targetKey,
                targetArtistId = targetArtist.id,
                targetArtistName = targetArtist.name,
            )
            dao.upsert(
                ArtistAliasEntity(
                    sourceKey = sourceKey,
                    sourceArtistId = source.id,
                    sourceArtistName = source.name,
                    targetKey = targetKey,
                    targetArtistId = targetArtist.id,
                    targetArtistName = targetArtist.name,
                ),
            )
        }
    }

    override suspend fun unmerge(source: Artist) = withContext(Dispatchers.IO) {
        dao.deleteBySourceKey(artistIdentityKey(source.name))
    }

    override suspend fun replaceAliases(aliases: List<ArtistAlias>) = withContext(Dispatchers.IO) {
        dao.replaceAll(aliases.flattened().map(ArtistAlias::toEntity))
    }

    override suspend fun mergeAliases(aliases: List<ArtistAlias>): Int =
        withContext(Dispatchers.IO) {
            aliases.flattened().count { alias ->
                dao.insertIfAbsent(alias.toEntity()) != -1L
            }
        }
}

private fun List<ArtistAlias>.flattened(): List<ArtistAlias> {
    val bySource = associateBy { artistIdentityKey(it.source.name) }
    return mapNotNull { alias ->
        var target = alias.target
        val visited = mutableSetOf(artistIdentityKey(alias.source.name))
        while (true) {
            val targetKey = artistIdentityKey(target.name)
            if (!visited.add(targetKey)) return@mapNotNull null
            val next = bySource[targetKey] ?: break
            target = next.target
        }
        alias.copy(target = target).takeIf {
            artistIdentityKey(it.source.name) != artistIdentityKey(it.target.name)
        }
    }
}

private fun ArtistAliasEntity.toDomain() = ArtistAlias(
    source = Artist(sourceArtistId, sourceArtistName),
    target = Artist(targetArtistId, targetArtistName),
)

private fun ArtistAlias.toEntity() = ArtistAliasEntity(
    sourceKey = artistIdentityKey(source.name),
    sourceArtistId = source.id,
    sourceArtistName = source.name,
    targetKey = artistIdentityKey(target.name),
    targetArtistId = target.id,
    targetArtistName = target.name,
)
