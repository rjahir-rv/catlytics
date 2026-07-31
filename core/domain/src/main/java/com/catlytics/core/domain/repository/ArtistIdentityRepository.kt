package com.catlytics.core.domain.repository

import com.catlytics.core.model.Artist
import com.catlytics.core.model.ArtistAlias
import kotlinx.coroutines.flow.Flow

interface ArtistIdentityRepository {
    fun observeAliases(): Flow<List<ArtistAlias>>

    suspend fun getAliases(): List<ArtistAlias>

    /** Moves the complete source group under target and keeps the graph flat. */
    suspend fun merge(source: Artist, target: Artist)

    suspend fun unmerge(source: Artist)

    suspend fun replaceAliases(aliases: List<ArtistAlias>)

    /** Adds non-conflicting aliases and returns the number inserted. */
    suspend fun mergeAliases(aliases: List<ArtistAlias>): Int
}
