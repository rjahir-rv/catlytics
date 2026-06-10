package com.catlytics.core.data.repository

import com.catlytics.core.data.local.LocalDataSource
import com.catlytics.core.data.mediator.DataMediator
import com.catlytics.core.data.model.TrackEntity
import com.catlytics.core.data.model.toDomain
import com.catlytics.core.domain.repository.LibraryPreferencesRepository
import com.catlytics.core.domain.repository.LibraryRepository
import com.catlytics.core.model.LibraryFolder
import com.catlytics.core.model.LibraryFolderContent
import com.catlytics.core.model.Track
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class OfflineFirstLibraryRepository @Inject constructor(
    private val localDataSource: LocalDataSource,
    private val mediator: DataMediator,
    private val preferencesRepository: LibraryPreferencesRepository,
) : LibraryRepository {
    override fun observeTracks(): Flow<List<Track>> = combine(
        localDataSource.observeTracks(),
        preferencesRepository.observeHiddenFolderIds(),
    ) { tracks, hiddenFolderIds ->
        tracks
            .filter { track ->
                val baseFolderId = track.toBaseFolder()?.folderId
                baseFolderId == null || baseFolderId !in hiddenFolderIds
            }
            .map { it.toDomain() }
    }

    override fun observeAllTracks(): Flow<List<Track>> = localDataSource.observeTracks()
        .map { tracks -> tracks.map(TrackEntity::toDomain) }

    override fun observeFolders(): Flow<List<LibraryFolder>> = combine(
        localDataSource.observeTracks(),
        preferencesRepository.observeHiddenFolderIds(),
    ) { tracks, hiddenFolderIds ->
        val rootFolderIds = tracks.mapNotNull(TrackEntity::toBaseFolder)
            .map(FolderTrack::folderId)
            .toSet()
        tracks.toLibraryFolders(hiddenFolderIds).filter { it.id in rootFolderIds }
    }

    override fun observeFolderContent(folderId: String): Flow<LibraryFolderContent?> = combine(
        localDataSource.observeTracks(),
        preferencesRepository.observeHiddenFolderIds(),
    ) { tracks, hiddenFolderIds ->
        tracks.toLibraryFolderContent(folderId, hiddenFolderIds)
    }

    override suspend fun refreshTracks() {
        mediator.syncLibrary()
    }

    override suspend fun setFolderVisible(folderId: String, visible: Boolean) {
        preferencesRepository.setFolderVisible(folderId, visible)
    }
}

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
