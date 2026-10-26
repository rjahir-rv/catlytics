package com.catlytics.core.data.repository

import androidx.room.withTransaction
import com.catlytics.core.data.local.room.ArtistAliasDao
import com.catlytics.core.data.local.room.ArtistAliasEntity
import com.catlytics.core.data.local.room.CatlyticsDatabase
import com.catlytics.core.domain.repository.ArtistIdentityRepository
import com.catlytics.core.model.Artist
import com.catlytics.core.model.ArtistAlias
import com.catlytics.core.model.artistIdentityKey
import com.catlytics.core.model.isRenamed
import com.catlytics.core.model.renamedArtistId
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

    override suspend fun rename(source: Artist, newName: String) = withContext(Dispatchers.IO) {
        val name = newName.trim()
        require(name.isNotEmpty()) { "El nombre no puede estar vacío." }
        val sourceKey = artistIdentityKey(source.name)
        val newKey = artistIdentityKey(name)

        database.withTransaction {
            val ownAlias = dao.getBySourceKey(sourceKey)?.takeIf { it.targetKey == sourceKey }
            val sourceName = ownAlias?.sourceArtistName ?: source.name
            if (newKey == sourceKey) {
                val targetId = ownAlias?.targetArtistId ?: source.id
                dao.moveGroup(sourceKey, sourceKey, targetId, name)
                when {
                    source.isRenamed -> Unit
                    name == sourceName -> dao.deleteBySourceKey(sourceKey)
                    else -> dao.upsert(
                        ArtistAliasEntity(sourceKey, source.id, sourceName, sourceKey, targetId, name),
                    )
                }
                return@withTransaction
            }

            val target = dao.getBySourceKey(newKey)
                ?.takeIf { it.targetKey != newKey }
                ?.let { Artist(it.targetArtistId, it.targetArtistName) }
                ?: Artist(renamedArtistId(name), name)
            val targetKey = artistIdentityKey(target.name)
            require(targetKey != sourceKey) { "El nombre nuevo es el mismo artista." }
            dao.moveGroup(sourceKey, targetKey, target.id, target.name)
            if (!source.isRenamed) {
                dao.upsert(
                    ArtistAliasEntity(sourceKey, source.id, sourceName, targetKey, target.id, target.name),
                )
            }
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
        if (alias.isDisplayRename) return@mapNotNull alias.takeIf { it.source.name != it.target.name }
        var target = alias.target
        val visited = mutableSetOf(artistIdentityKey(alias.source.name))
        while (true) {
            val targetKey = artistIdentityKey(target.name)
            if (!visited.add(targetKey)) return@mapNotNull null
            val next = bySource[targetKey] ?: break
            target = next.target
            // A display rename ends the chain with the target's new spelling.
            if (next.isDisplayRename) break
        }
        alias.copy(target = target).takeIf {
            artistIdentityKey(it.source.name) != artistIdentityKey(it.target.name)
        }
    }
}

internal fun ArtistAliasEntity.toDomain() = ArtistAlias(
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
