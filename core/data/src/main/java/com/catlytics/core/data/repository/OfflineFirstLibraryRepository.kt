package com.catlytics.core.data.repository

import com.catlytics.core.data.local.LocalDataSource
import com.catlytics.core.data.mediator.DataMediator
import com.catlytics.core.data.model.TrackEntity
import com.catlytics.core.data.model.toDomain
import com.catlytics.core.domain.repository.LibraryPreferencesRepository
import com.catlytics.core.domain.repository.LibraryRepository
import com.catlytics.core.model.LibraryFolder
import com.catlytics.core.model.Track
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

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

    override fun observeFolders(): Flow<List<LibraryFolder>> = combine(
        localDataSource.observeTracks(),
        preferencesRepository.observeHiddenFolderIds(),
    ) { tracks, hiddenFolderIds ->
        tracks.toLibraryFolders(hiddenFolderIds)
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
): List<LibraryFolder> = mapNotNull { track ->
    track.toBaseFolder()
}
    .groupBy(FolderTrack::folderId)
    .map { (folderId, tracks) ->
        val folder = tracks.first()
        LibraryFolder(
            id = folderId,
            name = folder.folderName,
            path = folder.folderPath,
            trackCount = tracks.size,
            isVisible = folderId !in hiddenFolderIds,
        )
    }
    .sortedBy { it.path.lowercase() }

private data class FolderTrack(
    val folderId: String,
    val folderName: String,
    val folderPath: String,
)

private fun TrackEntity.toBaseFolder(): FolderTrack? {
    val sourceFolderId = folderId ?: return null
    val sourcePath = folderPath ?: return null
    val volume = sourceFolderId.substringBefore(':', missingDelimiterValue = "external")
    val pathSegments = sourcePath.split('/').filter(String::isNotBlank)
    val basePath = pathSegments.basePath() ?: return null
    return FolderTrack(
        folderId = "$volume:$basePath",
        folderName = basePath.substringAfterLast('/'),
        folderPath = basePath,
    )
}

private fun List<String>.basePath(): String? {
    if (isEmpty()) return null
    return when {
        size >= 4 && take(3) == listOf("storage", "emulated", "0") -> take(4).joinToString("/")
        size >= 3 && first() == "storage" -> take(3).joinToString("/")
        else -> first()
    }
}
