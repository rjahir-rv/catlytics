package com.catlytics.core.data.repository

import com.catlytics.core.data.local.LocalDataSource
import com.catlytics.core.data.mediator.DataMediator
import com.catlytics.core.data.model.TrackEntity
import com.catlytics.core.data.model.toDomain
import com.catlytics.core.domain.repository.ArtistArtworkRepository
import com.catlytics.core.domain.repository.ArtistIdentityRepository
import com.catlytics.core.domain.repository.LibraryPreferencesRepository
import com.catlytics.core.domain.repository.LibraryRepository
import com.catlytics.core.domain.repository.TrackMetadataRepository
import com.catlytics.core.model.Album
import com.catlytics.core.model.AlbumContent
import com.catlytics.core.model.Artist
import com.catlytics.core.model.ArtistAlias
import com.catlytics.core.model.ArtistContent
import com.catlytics.core.model.ArtistSummary
import com.catlytics.core.model.LibraryFolder
import com.catlytics.core.model.LibraryFolderContent
import com.catlytics.core.model.PlaylistSource
import com.catlytics.core.model.Track
import com.catlytics.core.model.TrackMetadataOverride
import com.catlytics.core.model.artistIdentityKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OfflineFirstLibraryRepository @Inject constructor(
    private val localDataSource: LocalDataSource,
    private val mediator: DataMediator,
    private val preferencesRepository: LibraryPreferencesRepository,
    private val artistIdentityRepository: ArtistIdentityRepository,
    private val trackMetadataRepository: TrackMetadataRepository,
    private val artistArtworkRepository: ArtistArtworkRepository,
) : LibraryRepository {
    private val resolvedTracksCache = ResolvedTracksCache()

    override fun observeAlbums(): Flow<List<Album>> = observeVisibleTracks()
        .map { tracks -> tracks.toAlbums() }
        .offMain()

    override fun observeAlbumContent(albumId: String): Flow<AlbumContent?> = observeVisibleTracks()
        .map { tracks -> tracks.toAlbumContent(albumId) }
        .offMain()

    override fun observeArtists(): Flow<List<ArtistSummary>> = combine(
        observeVisibleTracks(),
        observeArtistArtwork(),
    ) { tracks, artwork -> tracks.toArtists(artwork) }
        .offMain()

    override fun observeArtistContent(artistId: String): Flow<ArtistContent?> = combine(
        observeVisibleTracks(),
        artistIdentityRepository.observeAliases(),
        artistArtworkRepository.observeOverrides(),
    ) { tracks, aliases, artworkOverrides ->
        val resolvedArtistId = aliases
            .firstOrNull { it.source.id == artistId }
            ?.let(tracks::resolveTargetArtist)
            ?.id
            ?: artistId
        tracks.toArtistContent(resolvedArtistId, ArtistArtworkLookup(artworkOverrides, aliases))
    }.offMain()

    override fun observeTracks(): Flow<List<Track>> = observeVisibleTracks()
        .map { tracks -> tracks.map(TrackEntity::toDomain) }
        .offMain()

    override fun observeAllTracks(): Flow<List<Track>> = observeResolvedTracks()
        .map { tracks -> tracks.map(TrackEntity::toDomain) }
        .offMain()

    override fun observeFolders(): Flow<List<LibraryFolder>> = combine(
        observeResolvedTracks(),
        preferencesRepository.observeHiddenFolderIds(),
    ) { canonicalTracks, hiddenFolderIds ->
        val rootFolderIds = canonicalTracks.mapNotNull(TrackEntity::toBaseFolder)
            .map(FolderTrack::folderId)
            .toSet()
        canonicalTracks.toLibraryFolders(hiddenFolderIds).filter { it.id in rootFolderIds }
    }.offMain()

    override fun observeFolderContent(folderId: String): Flow<LibraryFolderContent?> = combine(
        observeResolvedTracks(),
        preferencesRepository.observeHiddenFolderIds(),
    ) { tracks, hiddenFolderIds ->
        tracks.toLibraryFolderContent(folderId, hiddenFolderIds)
    }.offMain()

    override suspend fun resolvePlaylistSource(source: PlaylistSource): List<Track> {
        val aliases = artistIdentityRepository.observeAliases().first()
        val tracks = observeResolvedTracks().first()
        return when (source) {
            is PlaylistSource.TrackSource -> tracks.filter { it.id == source.trackId }
            is PlaylistSource.AlbumSource -> tracks.filter { it.albumId == source.albumId }
                .sortedWith(compareBy({ it.trackNumber ?: Int.MAX_VALUE }, { it.title.lowercase() }))
            is PlaylistSource.ArtistSource -> {
                val resolvedArtistId = aliases
                    .firstOrNull { it.source.id == source.artistId }
                    ?.let(tracks::resolveTargetArtist)
                    ?.id
                    ?: source.artistId
                tracks.filter { it.artistId == resolvedArtistId }
                    .sortedBy { it.title.lowercase() }
            }
            is PlaylistSource.FolderSource -> tracks.filter { entity ->
                entity.toFolderAncestors().any { it.folderId == source.folderId }
            }.sortedBy { it.title.lowercase() }
            is PlaylistSource.TrackCollectionSource -> {
                val tracksById = tracks.associateBy(TrackEntity::id)
                source.trackIds.mapNotNull(tracksById::get)
            }
        }.map(TrackEntity::toDomain)
    }

    /** Scanned tracks with metadata edits applied first, then artist aliases on top. */
    private fun observeResolvedTracks(): Flow<List<TrackEntity>> = combine(
        localDataSource.observeTracks(),
        trackMetadataRepository.observeOverrides(),
        artistIdentityRepository.observeAliases(),
        resolvedTracksCache::resolve,
    ).flowOn(Dispatchers.Default)

    /** Artist images chosen by the user, applied after aliases (see [ArtistArtworkLookup]). */
    private fun observeArtistArtwork(): Flow<ArtistArtworkLookup> = combine(
        artistArtworkRepository.observeOverrides(),
        artistIdentityRepository.observeAliases(),
        ::ArtistArtworkLookup,
    )

    private fun observeVisibleTracks(): Flow<List<TrackEntity>> = combine(
        observeResolvedTracks(),
        preferencesRepository.observeHiddenFolderIds(),
    ) { tracks, hiddenFolderIds -> tracks.filterVisible(hiddenFolderIds) }

    override suspend fun refreshTracks(): Int {
        val previousTrackIds = localDataSource.observeTracks().first()
            .mapTo(mutableSetOf(), TrackEntity::id)
        mediator.syncLibrary()
        return localDataSource.observeTracks().first()
            .count { track -> track.id !in previousTrackIds }
    }

    override suspend fun setFolderVisible(folderId: String, visible: Boolean) {
        preferencesRepository.setFolderVisible(folderId, visible)
    }
}

private fun <T> Flow<T>.offMain(): Flow<T> = distinctUntilChanged().flowOn(Dispatchers.Default)

private class ResolvedTracksCache {
    private var tracks: List<TrackEntity>? = null
    private var overrides: List<TrackMetadataOverride>? = null
    private var aliases: List<ArtistAlias>? = null
    private var resolved: List<TrackEntity> = emptyList()

    @Synchronized
    fun resolve(
        tracks: List<TrackEntity>,
        overrides: List<TrackMetadataOverride>,
        aliases: List<ArtistAlias>,
    ): List<TrackEntity> {
        if (tracks.sameAs(this.tracks) && overrides.sameAs(this.overrides) && aliases.sameAs(this.aliases)) {
            return resolved
        }
        resolved = tracks.applyMetadataOverrides(overrides).canonicalizeArtists(aliases)
        this.tracks = tracks
        this.overrides = overrides
        this.aliases = aliases
        return resolved
    }

    private fun List<*>.sameAs(other: List<*>?) = this === other || this == other
}

private fun List<TrackEntity>.canonicalizeArtists(
    aliases: List<ArtistAlias>,
): List<TrackEntity> {
    if (aliases.isEmpty()) return this
    val (displayRenames, merges) = aliases.partition(ArtistAlias::isDisplayRename)
    val aliasesBySourceKey = merges.associateBy { artistIdentityKey(it.source.name) }
    val displayNamesByKey = displayRenames.associate { artistIdentityKey(it.target.name) to it.target.name }
    val currentArtistsByKey = associate { track ->
        artistIdentityKey(track.artistName) to Artist(track.artistId, track.artistName)
    }
    return map { track ->
        val merged = aliasesBySourceKey[artistIdentityKey(track.artistName)]?.let { alias ->
            currentArtistsByKey[artistIdentityKey(alias.target.name)] ?: alias.target
        } ?: Artist(track.artistId, track.artistName)
        val name = displayNamesByKey[artistIdentityKey(merged.name)] ?: merged.name
        if (merged.id == track.artistId && name == track.artistName) {
            track
        } else {
            track.copy(artistId = merged.id, artistName = name)
        }
    }
}

private fun List<TrackEntity>.resolveTargetArtist(alias: ArtistAlias): Artist =
    firstOrNull { artistIdentityKey(it.artistName) == artistIdentityKey(alias.target.name) }
        ?.let { Artist(it.artistId, it.artistName) }
        ?: alias.target

private fun List<TrackEntity>.filterVisible(hiddenFolderIds: Set<String>) = filter { track ->
    val baseFolderId = track.toBaseFolder()?.folderId
    baseFolderId == null || baseFolderId !in hiddenFolderIds
}

private fun List<TrackEntity>.toAlbums(): List<Album> = mapNotNull { track ->
    val albumId = track.albumId ?: return@mapNotNull null
    val albumTitle = track.albumTitle ?: return@mapNotNull null
    AlbumTrack(
        albumId = albumId,
        albumTitle = albumTitle,
        artistId = track.artistId,
        artistName = track.artistName,
        artworkUri = track.artworkUri,
        hasArtworkOverride = track.hasArtworkOverride,
    )
}.groupBy(AlbumTrack::albumId)
    .map { (albumId, tracks) ->
        val album = tracks.first()
        Album(
            id = albumId,
            title = album.albumTitle,
            artist = Artist(album.artistId, album.artistName),
            artworkUri = tracks.firstOrNull(AlbumTrack::hasArtworkOverride)?.artworkUri
                ?: tracks.firstNotNullOfOrNull(AlbumTrack::artworkUri),
            trackCount = tracks.size,
        )
    }
    .sortedWith(compareBy({ it.title.lowercase() }, { it.artist.name.lowercase() }))

private fun List<TrackEntity>.toArtists(
    artwork: ArtistArtworkLookup,
): List<ArtistSummary> = groupBy(TrackEntity::artistId)
    .map { (artistId, tracks) ->
        val artistName = tracks.first().artistName
        val customArtworkUri = artwork.artworkFor(artistName)?.artworkUri
        ArtistSummary(
            artist = Artist(artistId, artistName),
            artworkUri = customArtworkUri ?: tracks.groupArtworkUri(),
            albumCount = tracks.mapNotNull(TrackEntity::albumId).distinct().size,
            trackCount = tracks.size,
            hasCustomArtwork = customArtworkUri != null,
        )
    }
    .sortedBy { it.artist.name.lowercase() }

private fun List<TrackEntity>.toArtistContent(
    artistId: String,
    artwork: ArtistArtworkLookup,
): ArtistContent? {
    val artistTracks = filter { it.artistId == artistId }
    if (artistTracks.isEmpty()) return null

    val summary = artistTracks.toArtists(artwork).single()
    return ArtistContent(
        summary = summary,
        albums = artistTracks.toAlbums(),
        tracks = artistTracks
            .sortedWith(
                compareBy<TrackEntity>(
                    { it.albumTitle?.lowercase().orEmpty() },
                    { it.trackNumber == null },
                    { it.trackNumber ?: Int.MAX_VALUE },
                    { it.title.lowercase() },
                ),
            )
            .map(TrackEntity::toDomain),
    )
}

private fun List<TrackEntity>.toAlbumContent(albumId: String): AlbumContent? {
    val albumTracks = filter { it.albumId == albumId }
    if (albumTracks.isEmpty()) return null

    val firstTrack = albumTracks.first()
    val albumTitle = firstTrack.albumTitle ?: return null
    return AlbumContent(
        album = Album(
            id = albumId,
            title = albumTitle,
            artist = Artist(firstTrack.artistId, firstTrack.artistName),
            artworkUri = albumTracks.groupArtworkUri(),
            trackCount = albumTracks.size,
        ),
        tracks = albumTracks
            .sortedWith(
                compareBy<TrackEntity>(
                    { it.trackNumber == null },
                    { it.trackNumber ?: Int.MAX_VALUE },
                    { it.title.lowercase() },
                ),
            )
            .map(TrackEntity::toDomain),
    )
}

private data class AlbumTrack(
    val albumId: String,
    val albumTitle: String,
    val artistId: String,
    val artistName: String,
    val artworkUri: String?,
    val hasArtworkOverride: Boolean,
)

private fun List<TrackEntity>.toLibraryFolders(
    hiddenFolderIds: Set<String>,
): List<LibraryFolder> = flatMap(TrackEntity::toFolderAncestors)
    .groupBy(FolderTrack::folderId)
    .map { (folderId, tracks) ->
        val folder = tracks.first()
        val rootFolderId = folder.rootFolderId
        LibraryFolder(
            id = folderId,
            name = folder.folderName,
            path = folder.folderPath,
            trackCount = tracks.size,
            isVisible = rootFolderId !in hiddenFolderIds,
        )
    }
    .sortedBy { it.path.lowercase() }

private data class FolderTrack(
    val folderId: String,
    val folderName: String,
    val folderPath: String,
    val rootFolderId: String,
)

private fun TrackEntity.toBaseFolder(): FolderTrack? = toFolderAncestors().firstOrNull()

private fun TrackEntity.toFolderAncestors(): List<FolderTrack> {
    val sourceFolderId = folderId ?: return emptyList()
    val sourcePath = folderPath ?: return emptyList()
    val volume = sourceFolderId.substringBefore(':', missingDelimiterValue = "external")
    val pathSegments = sourcePath.split('/').filter(String::isNotBlank)
    val basePath = pathSegments.basePath() ?: return emptyList()
    val baseSegmentCount = basePath.split('/').size
    val rootFolderId = "$volume:$basePath"
    return (baseSegmentCount..pathSegments.size).map { segmentCount ->
        val path = pathSegments.take(segmentCount).joinToString("/")
        FolderTrack(
            folderId = "$volume:$path",
            folderName = path.substringAfterLast('/'),
            folderPath = path,
            rootFolderId = rootFolderId,
        )
    }
}

private fun List<String>.basePath(): String? {
    if (isEmpty()) return null
    return when {
        size >= 4 && take(3) == listOf("storage", "emulated", "0") -> take(4).joinToString("/")
        size >= 3 && first() == "storage" -> take(3).joinToString("/")
        else -> first()
    }
}

private fun List<TrackEntity>.toLibraryFolderContent(
    folderId: String,
    hiddenFolderIds: Set<String>,
): LibraryFolderContent? {
    val folders = toLibraryFolders(hiddenFolderIds)
    val folder = folders.firstOrNull { it.id == folderId } ?: return null
    val subfolders = folders.filter { candidate ->
        candidate.id.substringBeforeLast('/', missingDelimiterValue = "") == folder.id
    }
    val tracks = filter { it.folderId == folderId }
        .map(TrackEntity::toDomain)
        .sortedBy { it.title.lowercase() }
    return LibraryFolderContent(
        folder = folder,
        subfolders = subfolders,
        tracks = tracks,
    )
}
