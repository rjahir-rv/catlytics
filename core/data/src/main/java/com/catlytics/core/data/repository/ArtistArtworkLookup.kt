package com.catlytics.core.data.repository

import com.catlytics.core.model.ArtistAlias
import com.catlytics.core.model.ArtistArtworkOverride
import com.catlytics.core.model.artistIdentityKey

internal class ArtistArtworkLookup(
    overrides: List<ArtistArtworkOverride>,
    aliases: List<ArtistAlias>,
) {
    private val overridesByKey = overrides.associateBy(ArtistArtworkOverride::artistKey)
    private val sourceKeysByTargetKey = aliases.groupBy(
        keySelector = { artistIdentityKey(it.target.name) },
        valueTransform = { artistIdentityKey(it.source.name) },
    )

    fun artworkFor(artistName: String): ArtistArtworkOverride? {
        if (overridesByKey.isEmpty()) return null
        val key = artistIdentityKey(artistName)
        return overridesByKey[key]
            ?: sourceKeysByTargetKey[key].orEmpty()
                .mapNotNull(overridesByKey::get)
                .maxByOrNull(ArtistArtworkOverride::updatedAtMillis)
    }

    companion object {
        val Empty = ArtistArtworkLookup(emptyList(), emptyList())
    }
}
