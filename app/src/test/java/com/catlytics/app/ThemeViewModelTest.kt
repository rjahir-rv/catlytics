package com.catlytics.app

import com.catlytics.core.domain.repository.AppPreferencesRepository
import com.catlytics.core.model.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class ThemeViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: FakeAppPreferencesRepository

    @Before
    fun setUp() {
        repository = FakeAppPreferencesRepository()
    }

    @Test
    fun `themeMode exposes repository changes`() = runTest {
        val viewModel = ThemeViewModel(repository)
        backgroundScope.launch { viewModel.themeMode.collect() }
        advanceUntilIdle()

        repository.themeMode.value = ThemeMode.Dark
        advanceUntilIdle()

        assertEquals(ThemeMode.Dark, viewModel.themeMode.value)
    }

}

private class FakeAppPreferencesRepository : AppPreferencesRepository {
    val themeMode = MutableStateFlow(ThemeMode.System)
    override fun observeThemeMode(): Flow<ThemeMode> = themeMode

    override suspend fun setThemeMode(themeMode: ThemeMode) {
        this.themeMode.value = themeMode
    }
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
