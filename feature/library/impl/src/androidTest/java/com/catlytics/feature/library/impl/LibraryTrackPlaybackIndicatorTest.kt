package com.catlytics.feature.library.impl

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import com.catlytics.core.model.Album
import com.catlytics.core.model.AlbumContent
import com.catlytics.core.model.Artist
import com.catlytics.core.model.ArtistContent
import com.catlytics.core.model.ArtistSummary
import com.catlytics.core.model.LibraryFolder
import com.catlytics.core.model.LibraryFolderContent
import com.catlytics.core.model.Track
import com.catlytics.feature.library.impl.album.LibraryAlbumScreen
import com.catlytics.feature.library.impl.album.LibraryAlbumUiState
import com.catlytics.feature.library.impl.artist.LibraryArtistScreen
import com.catlytics.feature.library.impl.artist.LibraryArtistUiState
import com.catlytics.feature.library.impl.folder.LibraryFolderScreen
import com.catlytics.feature.library.impl.folder.LibraryFolderUiState
import org.junit.Rule
import org.junit.Test

class LibraryTrackPlaybackIndicatorTest {
    @get:Rule(order = 0)
    val spanishLocale = SpanishLocaleRule()

    @get:Rule(order = 1)
    val composeRule = createComposeRule()

    @Test
    fun artistTrackListMarksOnlyTheCurrentPlayingTrack() {
        val tracks = listOf(currentTrack, otherTrack)
        val content = ArtistContent(
            summary = ArtistSummary(artist = artist, albumCount = 1, trackCount = tracks.size),
            albums = listOf(album),
            tracks = tracks,
        )
        composeRule.setContent {
            MaterialTheme {
                LibraryArtistScreen(
                    uiState = LibraryArtistUiState.Success(content),
                    onAlbumSelected = {},
                    onTrackSelected = { _, _ -> },
                    onAddToPlaylist = {},
                    onTrackOptions = {},
                    currentTrackId = currentTrack.id,
                    isCurrentTrackPlaying = true,
                    onTopBarColorChange = {},
                )
            }
        }

        assertPlayingIndicatorVisible(currentTrack)
        composeRule
            .onAllNodesWithContentDescription(playingDescription(otherTrack))
            .assertCountEquals(0)
    }

    @Test
    fun albumTrackListMarksTheCurrentPlayingTrack() {
        composeRule.setContent {
            MaterialTheme {
                LibraryAlbumScreen(
                    uiState = LibraryAlbumUiState.Success(
                        AlbumContent(album = album, tracks = listOf(currentTrack, otherTrack)),
                    ),
                    onTrackSelected = { _, _ -> },
                    onTrackOptions = {},
                    currentTrackId = currentTrack.id,
                    isCurrentTrackPlaying = true,
                    onTopBarColorChange = {},
                )
            }
        }

        assertPlayingIndicatorVisible(currentTrack)
        composeRule
            .onAllNodesWithContentDescription(playingDescription(otherTrack))
            .assertCountEquals(0)
    }

    @Test
    fun folderTrackListMarksTheCurrentPlayingTrack() {
        val folder = LibraryFolder(
            id = "external:Music",
            name = "Music",
            path = "Music",
            trackCount = 2,
            isVisible = true,
        )
        composeRule.setContent {
            MaterialTheme {
                LibraryFolderScreen(
                    uiState = LibraryFolderUiState.Success(
                        LibraryFolderContent(
                            folder = folder,
                            subfolders = emptyList(),
                            tracks = listOf(currentTrack, otherTrack),
                        ),
                    ),
                    onFolderSelected = {},
                    onTrackSelected = { _, _ -> },
                    onAddToPlaylist = {},
                    onTrackOptions = {},
                    currentTrackId = currentTrack.id,
                    isCurrentTrackPlaying = true,
                )
            }
        }

        assertPlayingIndicatorVisible(currentTrack)
        composeRule
            .onAllNodesWithContentDescription(playingDescription(otherTrack))
            .assertCountEquals(0)
    }

    @Test
    fun pausedCurrentTrackKeepsThePlayingBarsHidden() {
        val folder = LibraryFolder(
            id = "external:Music",
            name = "Music",
            path = "Music",
            trackCount = 1,
            isVisible = true,
        )
        composeRule.setContent {
            MaterialTheme {
                LibraryFolderScreen(
                    uiState = LibraryFolderUiState.Success(
                        LibraryFolderContent(
                            folder = folder,
                            subfolders = emptyList(),
                            tracks = listOf(currentTrack),
                        ),
                    ),
                    onFolderSelected = {},
                    onTrackSelected = { _, _ -> },
                    onAddToPlaylist = {},
                    onTrackOptions = {},
                    currentTrackId = currentTrack.id,
                    isCurrentTrackPlaying = false,
                )
            }
        }

        composeRule
            .onAllNodesWithContentDescription(playingDescription(currentTrack))
            .assertCountEquals(0)
    }

    private fun assertPlayingIndicatorVisible(track: Track) {
        composeRule.onNodeWithContentDescription(playingDescription(track)).assertIsDisplayed()
    }

    private fun playingDescription(track: Track) = "Reproduciendo ${track.title}"

    private companion object {
        val artist = Artist("artist-1", "Artista de prueba")
        val album = Album("album-1", "Álbum de prueba", artist, trackCount = 2)
        val currentTrack = Track(
            id = "track-current",
            title = "Canción en reproducción",
            artist = artist,
            durationMillis = 180_000L,
            mediaUri = "content://media/track-current",
        )
        val otherTrack = Track(
            id = "track-other",
            title = "Otra canción",
            artist = artist,
            durationMillis = 200_000L,
            mediaUri = "content://media/track-other",
        )
    }
}
