package com.catlytics.feature.library.impl.artist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.catlytics.core.domain.usecase.library.ObserveArtistContentUseCase
import com.catlytics.core.domain.usecase.playback.PlayTrackUseCase
import com.catlytics.core.model.Album
import com.catlytics.core.model.Track
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
internal class LibraryArtistViewModel @Inject constructor(
    private val observeArtistContentUseCase: ObserveArtistContentUseCase,
    private val playTrackUseCase: PlayTrackUseCase,
) : ViewModel() {
    private val artistId = MutableStateFlow<String?>(null)
    private val searchQuery = MutableStateFlow("")

    val uiState = combine(
        artistId
            .filterNotNull()
            .flatMapLatest(observeArtistContentUseCase::invoke),
        searchQuery
    ) { content, query ->
        if (content == null) {
            LibraryArtistUiState.NotFound
        } else {
            val filteredTracks = content.tracks.filterArtistTracksByQuery(query)
            val filteredAlbums = content.albums.filterArtistAlbumsByQuery(query)
            LibraryArtistUiState.Success(
                content = content.copy(
                    summary = content.summary.copy(
                        albumCount = filteredAlbums.size,
                        trackCount = filteredTracks.size
                    ),
                    tracks = filteredTracks,
                    albums = filteredAlbums
                ),
                searchQuery = query
            )
        }
    }
        .catch { error ->
            emit(
                LibraryArtistUiState.Error(
                    error.message ?: "No se pudo cargar el contenido del artista.",
                ),
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = LibraryArtistUiState.Loading,
        )

    fun openArtist(id: String) {
        artistId.value = id
    }

    fun onSearchQueryChanged(query: String) {
        searchQuery.value = query
    }

    fun playTrack(track: Track, queue: List<Track>) {
        viewModelScope.launch {
            playTrackUseCase(track, queue)
        }
    }
}

private fun List<Track>.filterArtistTracksByQuery(query: String): List<Track> {
    val normalizedQuery = query.trim()
    if (normalizedQuery.isEmpty()) return this
    return filter { track ->
        track.title.contains(normalizedQuery, ignoreCase = true) ||
            track.albumTitle?.contains(normalizedQuery, ignoreCase = true) == true
    }
}

private fun List<Album>.filterArtistAlbumsByQuery(query: String): List<Album> {
    val normalizedQuery = query.trim()
    if (normalizedQuery.isEmpty()) return this
    return filter { album ->
        album.title.contains(normalizedQuery, ignoreCase = true)
    }
}
