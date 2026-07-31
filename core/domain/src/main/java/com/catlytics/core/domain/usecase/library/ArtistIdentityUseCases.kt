package com.catlytics.core.domain.usecase.library

import com.catlytics.core.domain.repository.ArtistIdentityRepository
import com.catlytics.core.model.Artist

class ObserveArtistAliasesUseCase(
    private val repository: ArtistIdentityRepository,
) {
    operator fun invoke() = repository.observeAliases()
}

class MergeArtistsUseCase(
    private val repository: ArtistIdentityRepository,
) {
    suspend operator fun invoke(source: Artist, target: Artist) = repository.merge(source, target)
}

class UnmergeArtistUseCase(
    private val repository: ArtistIdentityRepository,
) {
    suspend operator fun invoke(source: Artist) = repository.unmerge(source)
}
