package com.catlytics.core.data.repository

import com.catlytics.core.data.local.LocalDataSource
import com.catlytics.core.data.mediator.DataMediator
import com.catlytics.core.data.model.TrackEntity
import com.catlytics.core.data.model.toDomain
import com.catlytics.core.domain.repository.LibraryPreferencesRepository
import com.catlytics.core.domain.repository.LibraryRepository
import com.catlytics.core.domain.repository.ArtistIdentityRepository
import com.catlytics.core.model.Album
import com.catlytics.core.model.AlbumContent
import com.catlytics.core.model.Artist
import com.catlytics.core.model.ArtistContent
import com.catlytics.core.model.ArtistSummary
import com.catlytics.core.model.LibraryFolder
import com.catlytics.core.model.LibraryFolderContent
import com.catlytics.core.model.PlaylistSource
import com.catlytics.core.model.Track
import com.catlytics.core.model.ArtistAlias
import com.catlytics.core.model.artistIdentityKey
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first

class OfflineFirstLibraryRepository @Inject constructor(
    private val localDataSource: LocalDataSource,
    private val mediator: DataMediator,
    private val preferencesRepository: LibraryPreferencesRepository,
    private val artistIdentityRepository: ArtistIdentityRepository,
) : LibraryRepository {
    override fun observeAlbums(): Flow<List<Album>> = combine(
        localDataSource.observeTracks(),
        preferencesRepository.observeHiddenFolderIds(),
        artistIdentityRepository.observeAliases(),
    ) { tracks, hiddenFolderIds, aliases ->
        tracks
            .filterVisible(hiddenFolderIds)
            .canonicalizeArtists(aliases)
            .toAlbums()
    }

    override fun observeAlbumContent(albumId: String): Flow<AlbumContent?> = combine(
        localDataSource.observeTracks(),
        preferencesRepository.observeHiddenFolderIds(),
        artistIdentityRepository.observeAliases(),
    ) { tracks, hiddenFolderIds, aliases ->
        tracks
            .filterVisible(hiddenFolderIds)
            .canonicalizeArtists(aliases)
            .toAlbumContent(albumId)
    }

    override fun observeArtists(): Flow<List<ArtistSummary>> = combine(
        localDataSource.observeTracks(),
        preferencesRepository.observeHiddenFolderIds(),
        artistIdentityRepository.observeAliases(),
    ) { tracks, hiddenFolderIds, aliases ->
        tracks
            .filterVisible(hiddenFolderIds)
            .canonicalizeArtists(aliases)
            .toArtists()
    }

    override fun observeArtistContent(artistId: String): Flow<ArtistContent?> = combine(
        localDataSource.observeTracks(),
        preferencesRepository.observeHiddenFolderIds(),
        artistIdentityRepository.observeAliases(),
    ) { tracks, hiddenFolderIds, aliases ->
        val resolvedArtistId = aliases
            .firstOrNull { it.source.id == artistId }
            ?.let(tracks::resolveTargetArtist)
            ?.id
            ?: artistId
        tracks
            .filterVisible(hiddenFolderIds)
            .canonicalizeArtists(aliases)
            .toArtistContent(resolvedArtistId)
    }

    override fun observeTracks(): Flow<List<Track>> = combine(
        localDataSource.observeTracks(),
        preferencesRepository.observeHiddenFolderIds(),
        artistIdentityRepository.observeAliases(),
    ) { tracks, hiddenFolderIds, aliases ->
        tracks
            .filterVisible(hiddenFolderIds)
            .canonicalizeArtists(aliases)
            .map { it.toDomain() }
    }

    override fun observeAllTracks(): Flow<List<Track>> = combine(
        localDataSource.observeTracks(),
        artistIdentityRepository.observeAliases(),
    ) { tracks, aliases ->
        tracks.canonicalizeArtists(aliases).map(TrackEntity::toDomain)
    }

    override fun observeFolders(): Flow<List<LibraryFolder>> = combine(
        localDataSource.observeTracks(),
        preferencesRepository.observeHiddenFolderIds(),
        artistIdentityRepository.observeAliases(),
    ) { tracks, hiddenFolderIds, aliases ->
        val canonicalTracks = tracks.canonicalizeArtists(aliases)
        val rootFolderIds = canonicalTracks.mapNotNull(TrackEntity::toBaseFolder)
            .map(FolderTrack::folderId)
            .toSet()
        canonicalTracks.toLibraryFolders(hiddenFolderIds).filter { it.id in rootFolderIds }
    }

    override fun observeFolderContent(folderId: String): Flow<LibraryFolderContent?> = combine(
        localDataSource.observeTracks(),
        preferencesRepository.observeHiddenFolderIds(),
        artistIdentityRepository.observeAliases(),
    ) { tracks, hiddenFolderIds, aliases ->
        tracks.canonicalizeArtists(aliases)
            .toLibraryFolderContent(folderId, hiddenFolderIds)
    }

    override suspend fun resolvePlaylistSource(source: PlaylistSource): List<Track> {
        val aliases = artistIdentityRepository.observeAliases().first()
        val tracks = localDataSource.observeTracks().first().canonicalizeArtists(aliases)
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

private fun List<TrackEntity>.canonicalizeArtists(
    aliases: List<ArtistAlias>,
): List<TrackEntity> {
    if (aliases.isEmpty()) return this
    val aliasesBySourceKey = aliases.associateBy { artistIdentityKey(it.source.name) }
    val currentArtistsByKey = associate { track ->
        artistIdentityKey(track.artistName) to Artist(track.artistId, track.artistName)
    }
    return map { track ->
        aliasesBySourceKey[artistIdentityKey(track.artistName)]?.let { alias ->
            val target = currentArtistsByKey[artistIdentityKey(alias.target.name)] ?: alias.target
            track.copy(artistId = target.id, artistName = target.name)
        } ?: track
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
    )
}.groupBy(AlbumTrack::albumId)
    .map { (albumId, tracks) ->
        val album = tracks.first()
        Album(
            id = albumId,
            title = album.albumTitle,
            artist = Artist(album.artistId, album.artistName),
            artworkUri = tracks.firstNotNullOfOrNull(AlbumTrack::artworkUri),
            trackCount = tracks.size,
        )
    }
    .sortedWith(compareBy({ it.title.lowercase() }, { it.artist.name.lowercase() }))

private fun List<TrackEntity>.toArtists(): List<ArtistSummary> = groupBy(TrackEntity::artistId)
    .map { (artistId, tracks) ->
        ArtistSummary(
            artist = Artist(artistId, tracks.first().artistName),
            artworkUri = tracks.firstNotNullOfOrNull(TrackEntity::artworkUri),
            albumCount = tracks.mapNotNull(TrackEntity::albumId).distinct().size,
            trackCount = tracks.size,
        )
    }
    .sortedBy { it.artist.name.lowercase() }

private fun List<TrackEntity>.toArtistContent(artistId: String): ArtistContent? {
    val artistTracks = filter { it.artistId == artistId }
    if (artistTracks.isEmpty()) return null

    val summary = artistTracks.toArtists().single()
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
            artworkUri = albumTracks.firstNotNullOfOrNull(TrackEntity::artworkUri),
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
