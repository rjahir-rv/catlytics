package com.catlytics.core.domain.usecase.library

import com.catlytics.core.domain.repository.ArtistIdentityRepository
import com.catlytics.core.model.Artist
import javax.inject.Inject

class ObserveArtistAliasesUseCase @Inject constructor(
    private val repository: ArtistIdentityRepository,
) {
    operator fun invoke() = repository.observeAliases()
}

class MergeArtistsUseCase @Inject constructor(
    private val repository: ArtistIdentityRepository,
) {
    suspend operator fun invoke(source: Artist, target: Artist) = repository.merge(source, target)
}

class UnmergeArtistUseCase @Inject constructor(
    private val repository: ArtistIdentityRepository,
) {
    suspend operator fun invoke(source: Artist) = repository.unmerge(source)
}

class RenameArtistUseCase @Inject constructor(
    private val repository: ArtistIdentityRepository,
) {
    suspend operator fun invoke(source: Artist, newName: String) {
        require(newName.isNotBlank()) { "El nombre no puede estar vacío." }
        repository.rename(source, newName)
    }
}
