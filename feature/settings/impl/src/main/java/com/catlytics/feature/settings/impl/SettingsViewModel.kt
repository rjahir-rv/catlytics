package com.catlytics.feature.settings.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.catlytics.core.domain.repository.AppPreferencesRepository
import com.catlytics.core.domain.repository.EqualizerRepository
import com.catlytics.core.model.EqualizerMode
import com.catlytics.core.model.EqualizerPreset
import com.catlytics.core.model.EqualizerState
import com.catlytics.core.model.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
internal class SettingsViewModel @Inject constructor(
    private val appPreferencesRepository: AppPreferencesRepository,
    private val equalizerRepository: EqualizerRepository,
) : ViewModel() {
    val themeMode: StateFlow<ThemeMode> = appPreferencesRepository.observeThemeMode()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = ThemeMode.System,
        )
    val equalizerState: StateFlow<EqualizerState> = equalizerRepository.observeEqualizerState()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = EqualizerState(),
        )

    fun setThemeMode(themeMode: ThemeMode) {
        viewModelScope.launch {
            appPreferencesRepository.setThemeMode(themeMode)
        }
    }

    fun setEqualizerEnabled(enabled: Boolean) {
        viewModelScope.launch {
            equalizerRepository.setEnabled(enabled)
        }
    }

    fun selectEqualizerPreset(preset: EqualizerPreset) {
        viewModelScope.launch {
            equalizerRepository.selectPreset(preset)
        }
    }

    fun setEqualizerMode(mode: EqualizerMode) {
        viewModelScope.launch {
            equalizerRepository.setMode(mode)
        }
    }

    fun setCustomBandLevel(bandId: Short, level: Int, isFinal: Boolean) {
        viewModelScope.launch {
            if (isFinal) {
                equalizerRepository.setBandLevel(bandId, level)
            } else {
                equalizerRepository.setBandLevelTransient(bandId, level)
            }
        }
    }

    fun refreshEqualizer() {
        viewModelScope.launch {
            equalizerRepository.refreshCapabilities()
        }
    }
}
