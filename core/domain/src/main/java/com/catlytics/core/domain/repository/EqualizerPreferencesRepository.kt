package com.catlytics.core.domain.repository

import com.catlytics.core.model.EqualizerMode
import kotlinx.coroutines.flow.Flow

interface EqualizerPreferencesRepository {
    fun observeEqualizerEnabled(): Flow<Boolean>

    fun observeEqualizerMode(): Flow<EqualizerMode>

    fun observeEqualizerPresetName(): Flow<String?>

    fun observeCustomBandLevels(): Flow<Map<Short, Int>>

    suspend fun setEqualizerEnabled(enabled: Boolean)

    suspend fun setEqualizerMode(mode: EqualizerMode)

    suspend fun setEqualizerPresetName(presetName: String?)

    suspend fun setCustomBandLevel(bandId: Short, level: Int)
}
