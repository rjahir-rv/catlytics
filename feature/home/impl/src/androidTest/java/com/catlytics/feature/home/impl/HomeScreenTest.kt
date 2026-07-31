package com.catlytics.feature.home.impl

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.catlytics.core.model.Artist
import com.catlytics.core.model.Track
import com.catlytics.core.model.TopTrack
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class HomeScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun playingBarsAreOnlyShownWhileCurrentTrackIsPlaying() {
        composeRule.setContent {
            MaterialTheme {
                HomeScreen(
                    uiState = HomeUiState.Success(
                        tracks = listOf(track),
                        currentTrackId = track.id,
                        isCurrentTrackPlaying = true,
                    ),
                    searchQuery = "",
                    hasAudioPermission = true,
                    onRequestPermission = {},
                    onTrackSelected = { _, _ -> },
                    onTrackOptions = {},
                )
            }
        }

        composeRule
            .onNodeWithContentDescription("Reproduciendo ${track.title}")
            .assertIsDisplayed()
    }

    @Test
    fun playingBarsAreHiddenWhileCurrentTrackIsPaused() {
        composeRule.setContent {
            MaterialTheme {
                HomeScreen(
                    uiState = HomeUiState.Success(
                        tracks = listOf(track),
                        currentTrackId = track.id,
                        isCurrentTrackPlaying = false,
                    ),
                    searchQuery = "",
                    hasAudioPermission = true,
                    onRequestPermission = {},
                    onTrackSelected = { _, _ -> },
                    onTrackOptions = {},
                )
            }
        }

        composeRule
            .onAllNodesWithContentDescription("Reproduciendo ${track.title}")
            .assertCountEquals(0)
    }

    @Test
    fun trackRowUsesOptionsMenuWithoutFavoriteButton() {
        var optionsClicks = 0
        composeRule.setContent {
            MaterialTheme {
                HomeScreen(
                    uiState = HomeUiState.Success(tracks = listOf(track)),
                    searchQuery = "",
                    hasAudioPermission = true,
                    onRequestPermission = {},
                    onTrackSelected = { _, _ -> },
                    onTrackOptions = { optionsClicks++ },
                )
            }
        }

        composeRule
            .onAllNodesWithContentDescription("Agregar ${track.title} a Tus me gusta")
            .assertCountEquals(0)
        composeRule
            .onNodeWithContentDescription("Opciones de ${track.title}")
            .performClick()

        assertEquals(1, optionsClicks)
    }

    @Test
    fun highlightsPlayRecentTrackWithFullLibraryQueueAndOpenStatistics() {
        val secondTrack = track.copy(id = "track-2", title = "Otra canción")
        var selectedTrack: Track? = null
        var selectedQueue: List<Track>? = null
        var statisticsClicks = 0
        composeRule.setContent {
            MaterialTheme {
                HomeScreen(
                    uiState = HomeUiState.Success(
                        tracks = listOf(track, secondTrack),
                        recentlyPlayedTracks = listOf(secondTrack),
                        topTracks = listOf(
                            TopTrack(
                                trackId = track.id,
                                title = track.title,
                                artistName = track.artist.name,
                                artworkUri = null,
                                playCount = 3,
                                totalListenedMillis = 180_000L,
                            ),
                        ),
                    ),
                    searchQuery = "",
                    hasAudioPermission = true,
                    onRequestPermission = {},
                    onTrackSelected = { selected, queue ->
                        selectedTrack = selected
                        selectedQueue = queue
                    },
                    onTrackOptions = {},
                    onNavigateToStatistics = { statisticsClicks++ },
                )
            }
        }

        composeRule.onAllNodesWithText("Para ti").assertCountEquals(0)
        composeRule.onNodeWithText("Últimas escuchadas").assertIsDisplayed()
        composeRule.onNodeWithText("Top 3 de esta semana").assertIsDisplayed()
        composeRule.onNodeWithText("Todas las canciones").assertIsDisplayed()
        composeRule
            .onNodeWithContentDescription("Reproducir ${secondTrack.title}")
            .performClick()
        composeRule.onNodeWithText("Ver estadísticas").performClick()

        assertEquals(secondTrack, selectedTrack)
        assertEquals(listOf(track, secondTrack), selectedQueue)
        assertEquals(1, statisticsClicks)
    }

    @Test
    fun topTrackDispatchesPlaybackFromItsRow() {
        var selectedTopTrackId: String? = null
        composeRule.setContent {
            MaterialTheme {
                HomeScreen(
                    uiState = HomeUiState.Success(
                        tracks = listOf(track),
                        topTracks = listOf(
                            TopTrack(
                                trackId = track.id,
                                title = track.title,
                                artistName = track.artist.name,
                                artworkUri = null,
                                playCount = 3,
                                totalListenedMillis = 180_000L,
                            ),
                        ),
                    ),
                    searchQuery = "",
                    hasAudioPermission = true,
                    onRequestPermission = {},
                    onTrackSelected = { _, _ -> },
                    onTopTrackSelected = { selectedTopTrackId = it },
                    onTrackOptions = {},
                )
            }
        }

        composeRule
            .onNodeWithContentDescription("Reproducir ${track.title} desde Top 3")
            .performClick()

        assertEquals(track.id, selectedTopTrackId)
    }

    @Test
    fun quickActionsAreShownIndividuallyAndDispatchTheirCallbacks() {
        var uiState: HomeUiState by mutableStateOf(
            HomeUiState.Success(
                tracks = List(5) { index -> track.copy(id = "track-$index") },
                dailyPlaylistTrackCount = 5,
            ),
        )
        var dailyClicks = 0
        var shuffleClicks = 0
        var favoriteClicks = 0
        composeRule.setContent {
            MaterialTheme {
                HomeScreen(
                    uiState = uiState,
                    searchQuery = "",
                    hasAudioPermission = true,
                    onRequestPermission = {},
                    onTrackSelected = { _, _ -> },
                    onTrackOptions = {},
                    onPlayDailyPlaylist = { dailyClicks++ },
                    onShuffleAll = { shuffleClicks++ },
                    onOpenFavorites = { favoriteClicks++ },
                )
            }
        }

        composeRule
            .onNodeWithContentDescription("Reproducir y abrir Playlist diaria")
            .performClick()
        composeRule.runOnIdle {
            uiState = HomeUiState.Success(
                tracks = listOf(track, track.copy(id = "track-2")),
                canShuffleAll = true,
            )
        }
        composeRule
            .onNodeWithContentDescription("Reproducir todas las canciones aleatoriamente")
            .performClick()
        composeRule.runOnIdle {
            uiState = HomeUiState.Success(
                tracks = listOf(track),
                favoriteTracks = listOf(track),
            )
        }
        composeRule.onNodeWithContentDescription("Abrir Favoritos").performClick()

        assertEquals(1, dailyClicks)
        assertEquals(1, shuffleClicks)
        assertEquals(1, favoriteClicks)
    }

    @Test
    fun quickActionsAreHiddenDuringSearch() {
        composeRule.setContent {
            MaterialTheme {
                HomeScreen(
                    uiState = HomeUiState.Success(
                        tracks = List(5) { index -> track.copy(id = "track-$index") },
                        dailyPlaylistTrackCount = 5,
                        canShuffleAll = true,
                        favoriteTracks = listOf(track),
                    ),
                    searchQuery = "Canción",
                    hasAudioPermission = true,
                    onRequestPermission = {},
                    onTrackSelected = { _, _ -> },
                    onTrackOptions = {},
                )
            }
        }

        composeRule.onAllNodesWithText("Accesos rápidos").assertCountEquals(0)
        composeRule.onAllNodesWithText("Playlist diaria").assertCountEquals(0)
        composeRule.onAllNodesWithText("Aleatorio").assertCountEquals(0)
        composeRule.onAllNodesWithText("Favoritos").assertCountEquals(0)
    }

    @Test
    fun featuredSectionsCanBeHiddenAndShownFromTheTracksHeader() {
        val tracks = List(5) { index ->
            track.copy(id = "track-$index", title = "Canción $index")
        }
        composeRule.setContent {
            MaterialTheme {
                HomeScreen(
                    uiState = HomeUiState.Success(
                        tracks = tracks,
                        dailyPlaylistTrackCount = 5,
                        canShuffleAll = true,
                        favoriteTracks = listOf(track),
                        recentlyPlayedTracks = listOf(track),
                        topTracks = listOf(
                            TopTrack(
                                trackId = track.id,
                                title = track.title,
                                artistName = track.artist.name,
                                artworkUri = null,
                                playCount = 2,
                                totalListenedMillis = 120_000L,
                            ),
                        ),
                    ),
                    searchQuery = "",
                    hasAudioPermission = true,
                    onRequestPermission = {},
                    onTrackSelected = { _, _ -> },
                    onTrackOptions = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription("Ocultar secciones destacadas").performClick()

        composeRule.onNodeWithText("Accesos rápidos").assertIsDisplayed()
        composeRule.onAllNodesWithText("Playlist diaria").assertCountEquals(0)
        composeRule.onAllNodesWithText("Aleatorio").assertCountEquals(0)
        composeRule.onAllNodesWithText("Favoritos").assertCountEquals(0)
        composeRule.onAllNodesWithText("Últimas escuchadas").assertCountEquals(0)
        composeRule.onAllNodesWithText("Top 3 de esta semana").assertCountEquals(0)
        composeRule.onNodeWithText("Canción 0").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Mostrar secciones destacadas").performClick()

        composeRule.onNodeWithText("Playlist diaria").assertIsDisplayed()
        composeRule.onNodeWithText("Últimas escuchadas").assertIsDisplayed()
        composeRule.onNodeWithText("Top 3 de esta semana").assertIsDisplayed()
    }

    @Test
    fun contentIsReadyOnlyAfterLoadingStateFinishes() {
        var uiState: HomeUiState by mutableStateOf(HomeUiState.Loading)
        var readyCalls = 0
        composeRule.setContent {
            MaterialTheme {
                HomeScreen(
                    uiState = uiState,
                    searchQuery = "",
                    hasAudioPermission = true,
                    onRequestPermission = {},
                    onTrackSelected = { _, _ -> },
                    onTrackOptions = {},
                    onContentReady = { readyCalls++ },
                )
            }
        }

        composeRule.runOnIdle { assertEquals(0, readyCalls) }
        composeRule.runOnIdle { uiState = HomeUiState.Empty }
        composeRule.runOnIdle { assertEquals(1, readyCalls) }
    }

    private val track = Track(
        id = "track-1",
        title = "Canción de prueba",
        artist = Artist(id = "artist-1", name = "Artista de prueba"),
        durationMillis = 180_000L,
        mediaUri = "content://media/external/audio/media/1",
    )
}
