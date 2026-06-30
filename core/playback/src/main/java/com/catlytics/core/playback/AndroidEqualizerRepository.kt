package com.catlytics.core.playback

import android.media.audiofx.Equalizer
import androidx.media3.common.C
import com.catlytics.core.domain.repository.EqualizerPreferencesRepository
import com.catlytics.core.domain.repository.EqualizerRepository
import com.catlytics.core.model.EqualizerBand
import com.catlytics.core.model.EqualizerLevelRange
import com.catlytics.core.model.EqualizerMode
import com.catlytics.core.model.EqualizerPreset
import com.catlytics.core.model.EqualizerState
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@Singleton
class AndroidEqualizerRepository @Inject constructor(
    private val preferencesRepository: EqualizerPreferencesRepository,
) : EqualizerRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val runtimeState = MutableStateFlow(EqualizerRuntimeState())
    private var equalizer: Equalizer? = null
    private var latestEnabled = false
    private var latestPresetName: String? = null
    private var latestMode = EqualizerMode.Preset
    private var latestCustomBands: Map<Short, Int> = emptyMap()

    init {
        scope.launch {
            preferencesRepository.observeEqualizerEnabled()
                .distinctUntilChanged()
                .collect { enabled ->
                    latestEnabled = enabled
                    applyEnabled(enabled)
                    refreshRuntimeCapabilities()
                }
        }
        scope.launch {
            preferencesRepository.observeEqualizerPresetName()
                .distinctUntilChanged()
                .collect { presetName ->
                    latestPresetName = presetName
                    applyModeAndBands()
                    refreshRuntimeCapabilities()
                }
        }
        scope.launch {
            preferencesRepository.observeEqualizerMode()
                .distinctUntilChanged()
                .collect { mode ->
                    latestMode = mode
                    applyModeAndBands()
                    refreshRuntimeCapabilities()
                }
        }
        scope.launch {
            preferencesRepository.observeCustomBandLevels()
                .distinctUntilChanged()
                .collect { bands ->
                    latestCustomBands = bands
                    if (latestMode == EqualizerMode.Custom) {
                        applyModeAndBands()
                        refreshRuntimeCapabilities()
                    }
                }
        }
    }

    override fun observeEqualizerState(): Flow<EqualizerState> = combine(
        preferencesRepository.observeEqualizerEnabled(),
        preferencesRepository.observeEqualizerPresetName(),
        preferencesRepository.observeEqualizerMode(),
        runtimeState,
    ) { enabled, presetName, mode, runtime ->
        EqualizerState(
            enabled = enabled && runtime.isAvailable,
            mode = mode,
            selectedPresetName = runtime.resolvePresetName(presetName),
            presets = runtime.presets,
            bands = runtime.bands,
            levelRange = runtime.levelRange,
            isAvailable = runtime.isAvailable,
            errorMessage = runtime.errorMessage,
        )
    }

    override suspend fun setEnabled(enabled: Boolean) {
        preferencesRepository.setEqualizerEnabled(enabled)
    }

    override suspend fun selectPreset(preset: EqualizerPreset) {
        preferencesRepository.setEqualizerPresetName(preset.name)
    }

    override suspend fun setMode(mode: EqualizerMode) {
        preferencesRepository.setEqualizerMode(mode)
    }

    override suspend fun setBandLevel(bandId: Short, level: Int) {
        preferencesRepository.setCustomBandLevel(bandId, level)
    }

    override suspend fun setBandLevelTransient(bandId: Short, level: Int) {
        val effect = equalizer ?: return
        runCatching {
            if (bandId in 0 until effect.numberOfBands) {
                effect.setBandLevel(bandId, level.toShort())
            }
        }
    }

    override suspend fun refreshCapabilities() {
        refreshRuntimeCapabilities()
    }

    fun attachAudioSessionId(audioSessionId: Int) {
        releaseEqualizer()
        if (audioSessionId == C.AUDIO_SESSION_ID_UNSET) {
            runtimeState.value = EqualizerRuntimeState(errorMessage = "Sesión de audio no disponible.")
            return
        }
        runCatching {
            @Suppress("DEPRECATION")
            Equalizer(0, audioSessionId)
        }.onSuccess { effect ->
            equalizer = effect
            applyEnabled(latestEnabled)
            applyModeAndBands()
            refreshRuntimeCapabilities()
        }.onFailure { error ->
            runtimeState.value = EqualizerRuntimeState(
                errorMessage = error.message ?: "El ecualizador no está disponible en este dispositivo.",
            )
        }
    }

    fun release() {
        releaseEqualizer()
        runtimeState.value = EqualizerRuntimeState()
    }

    private fun refreshRuntimeCapabilities() {
        val effect = equalizer
        if (effect == null) {
            runtimeState.value = EqualizerRuntimeState(errorMessage = "Reproduce una canción para activar el ecualizador.")
            return
        }

        runCatching {
            val presetCount = effect.numberOfPresets.toInt().coerceAtLeast(0)
            val presets = (0 until presetCount).map { index ->
                val presetId = index.toShort()
                EqualizerPreset(
                    id = presetId,
                    name = effect.getPresetName(presetId),
                )
            }
            val bandCount = effect.numberOfBands.toInt().coerceAtLeast(0)
            val bands = (0 until bandCount).map { index ->
                val bandId = index.toShort()
                EqualizerBand(
                    id = bandId,
                    centerFrequencyHz = effect.getCenterFreq(bandId) / 1_000,
                    levelMilliBel = effect.getBandLevel(bandId).toInt(),
                )
            }
            val levelRange = effect.bandLevelRange.takeIf { it.size >= 2 }?.let { range ->
                EqualizerLevelRange(
                    minMilliBel = range[0].toInt(),
                    maxMilliBel = range[1].toInt(),
                )
            }

            runtimeState.value = EqualizerRuntimeState(
                presets = presets,
                bands = bands,
                levelRange = levelRange,
                currentPresetId = runCatching { effect.currentPreset }.getOrNull(),
                isAvailable = true,
            )
        }.onFailure { error ->
            runtimeState.value = EqualizerRuntimeState(
                errorMessage = error.message ?: "No se pudieron leer los presets del dispositivo.",
            )
        }
    }

    private fun applyEnabled(enabled: Boolean) {
        val effect = equalizer ?: return
        runCatching { effect.setEnabled(enabled) }
    }

    private fun applyModeAndBands() {
        if (latestMode == EqualizerMode.Preset) {
            applyPresetByName(latestPresetName)
        } else {
            val effect = equalizer ?: return
            runCatching {
                latestCustomBands.forEach { (bandId, level) ->
                    if (bandId in 0 until effect.numberOfBands) {
                        effect.setBandLevel(bandId, level.toShort())
                    }
                }
            }
        }
    }

    private fun applyPresetByName(presetName: String?) {
        val effect = equalizer ?: return
        val targetName = presetName ?: return
        runCatching {
            val presetId = (0 until effect.numberOfPresets.toInt())
                .map(Int::toShort)
                .firstOrNull { id -> effect.getPresetName(id).equals(targetName, ignoreCase = true) }
                ?: return
            effect.usePreset(presetId)
        }
    }

    private fun releaseEqualizer() {
        val effect = equalizer ?: return
        runCatching { effect.release() }
        equalizer = null
    }
}

private data class EqualizerRuntimeState(
    val presets: List<EqualizerPreset> = emptyList(),
    val bands: List<EqualizerBand> = emptyList(),
    val levelRange: EqualizerLevelRange? = null,
    val currentPresetId: Short? = null,
    val isAvailable: Boolean = false,
    val errorMessage: String? = null,
) {
    fun resolvePresetName(preferredName: String?): String? =
        presets.firstOrNull { it.name.equals(preferredName, ignoreCase = true) }?.name
            ?: presets.firstOrNull { it.id == currentPresetId }?.name
            ?: preferredName
}
