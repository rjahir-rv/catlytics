package com.catlytics.feature.settings.impl

import com.catlytics.core.domain.repository.AppPreferencesRepository
import com.catlytics.core.domain.repository.EqualizerRepository
import com.catlytics.core.model.EqualizerPreset
import com.catlytics.core.model.EqualizerMode
import com.catlytics.core.model.EqualizerState
import com.catlytics.core.model.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `setThemeMode persists every available mode`() = runTest {
        val repository = FakeAppPreferencesRepository()
        val equalizerRepository = FakeEqualizerRepository()
        val viewModel = SettingsViewModel(repository, equalizerRepository)

        ThemeMode.entries.forEach { themeMode ->
            viewModel.setThemeMode(themeMode)
            advanceUntilIdle()

            assertEquals(themeMode, repository.themeMode.value)
            assertEquals(themeMode, viewModel.themeMode.value)
        }
    }

    @Test
    fun `setEqualizerEnabled persists requested state`() = runTest {
        val equalizerRepository = FakeEqualizerRepository()
        val viewModel = SettingsViewModel(FakeAppPreferencesRepository(), equalizerRepository)

        viewModel.setEqualizerEnabled(true)
        advanceUntilIdle()

        assertEquals(true, equalizerRepository.state.value.enabled)
        assertEquals(true, viewModel.equalizerState.value.enabled)
    }

    @Test
    fun `selectEqualizerPreset persists selected preset`() = runTest {
        val equalizerRepository = FakeEqualizerRepository()
        val viewModel = SettingsViewModel(FakeAppPreferencesRepository(), equalizerRepository)
        val preset = EqualizerPreset(id = 1, name = "Rock")

        viewModel.selectEqualizerPreset(preset)
        advanceUntilIdle()

        assertEquals("Rock", equalizerRepository.state.value.selectedPresetName)
        assertEquals("Rock", viewModel.equalizerState.value.selectedPresetName)
    }
}

private class FakeAppPreferencesRepository : AppPreferencesRepository {
    val themeMode = MutableStateFlow(ThemeMode.System)

    override fun observeThemeMode(): Flow<ThemeMode> = themeMode

    override suspend fun setThemeMode(themeMode: ThemeMode) {
        this.themeMode.value = themeMode
    }
}

private class FakeEqualizerRepository : EqualizerRepository {
    val state = MutableStateFlow(
        EqualizerState(
            isAvailable = true,
            presets = listOf(
                EqualizerPreset(id = 0, name = "Normal"),
                EqualizerPreset(id = 1, name = "Rock"),
            ),
        ),
    )

    override fun observeEqualizerState(): Flow<EqualizerState> = state

    override suspend fun setEnabled(enabled: Boolean) {
        state.value = state.value.copy(enabled = enabled)
    }

    override suspend fun selectPreset(preset: EqualizerPreset) {
        state.value = state.value.copy(selectedPresetName = preset.name)
    }

    override suspend fun setMode(mode: EqualizerMode) {
        state.value = state.value.copy(mode = mode)
    }

    override suspend fun setBandLevel(bandId: Short, level: Int) {
        // Fake implementation
    }

    override suspend fun setBandLevelTransient(bandId: Short, level: Int) {}

    override suspend fun refreshCapabilities() = Unit
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
