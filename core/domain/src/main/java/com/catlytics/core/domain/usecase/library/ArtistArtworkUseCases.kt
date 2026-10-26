package com.catlytics.core.domain.usecase.library

import com.catlytics.core.domain.repository.ArtistArtworkRepository
import com.catlytics.core.model.Artist
import javax.inject.Inject

class ObserveArtistArtworkOverridesUseCase @Inject constructor(
    private val repository: ArtistArtworkRepository,
) {
    operator fun invoke() = repository.observeOverrides()
}

class SetArtistArtworkUseCase @Inject constructor(
    private val repository: ArtistArtworkRepository,
) {
    suspend operator fun invoke(artist: Artist, sourceUri: String) = repository.setArtwork(artist, sourceUri)
}

class RestoreArtistArtworkUseCase @Inject constructor(
    private val repository: ArtistArtworkRepository,
) {
    suspend operator fun invoke(artist: Artist) = repository.restoreArtwork(artist)
}

class RemoveArtistArtworkUseCase @Inject constructor(
    private val repository: ArtistArtworkRepository,
) {
    suspend operator fun invoke(artistKey: String) = repository.removeArtwork(artistKey)
}
