package com.catlytics.feature.home.impl

import com.catlytics.core.domain.repository.LibraryRepository
import com.catlytics.core.domain.usecase.ObserveLibraryUseCase
import com.catlytics.core.domain.usecase.RefreshLibraryUseCase
import com.catlytics.core.model.Artist
import com.catlytics.core.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: FakeLibraryRepository

    @Before
    fun setUp() {
        repository = FakeLibraryRepository()
    }

    @Test
    fun `uiState is empty when library has no tracks`() = runTest {
        val viewModel = homeViewModel()
        backgroundScope.startCollecting(viewModel)
        advanceUntilIdle()

        assertEquals(HomeUiState.Empty, viewModel.uiState.value)
    }

    @Test
    fun `uiState is success when library has tracks`() = runTest {
        val track = track(id = "track-1")
        repository.setTracks(listOf(track))
        val viewModel = homeViewModel()
        backgroundScope.startCollecting(viewModel)
        advanceUntilIdle()

        assertEquals(HomeUiState.Success(listOf(track)), viewModel.uiState.value)
    }

    @Test
    fun `refreshLibrary surfaces refresh errors`() = runTest {
        repository.refreshResult = Result.failure(IllegalStateException("MediaStore failed"))
        val viewModel = homeViewModel()
        backgroundScope.startCollecting(viewModel)

        viewModel.refreshLibrary()
        advanceUntilIdle()

        assertEquals(HomeUiState.Error("MediaStore failed"), viewModel.uiState.value)
    }

    @Test
    fun `uiState starts as loading`() = runTest {
        val viewModel = homeViewModel()

        assertTrue(viewModel.uiState.first() is HomeUiState.Loading)
    }

    private fun homeViewModel() = HomeViewModel(
        observeLibraryUseCase = ObserveLibraryUseCase(repository),
        refreshLibraryUseCase = RefreshLibraryUseCase(repository),
    )

    private fun kotlinx.coroutines.CoroutineScope.startCollecting(viewModel: HomeViewModel) {
        launch {
            viewModel.uiState.collect()
        }
    }

    private fun track(id: String) = Track(
        id = id,
        title = "Track $id",
        artist = Artist(
            id = "artist-$id",
            name = "Artist $id",
        ),
        durationMillis = 180_000L,
    )
}

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    private val testDispatcher: TestDispatcher = StandardTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}

private class FakeLibraryRepository : LibraryRepository {
    private val tracks = MutableStateFlow(emptyList<Track>())
    var refreshResult: Result<Unit> = Result.success(Unit)

    override fun observeTracks() = tracks

    override suspend fun refreshTracks() {
        refreshResult.getOrThrow()
    }

    fun setTracks(newTracks: List<Track>) {
        tracks.update { newTracks }
    }
}
