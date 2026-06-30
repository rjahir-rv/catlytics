package com.catlytics.core.domain.repository

import com.catlytics.core.model.EqualizerMode
import com.catlytics.core.model.EqualizerPreset
import com.catlytics.core.model.EqualizerState
import kotlinx.coroutines.flow.Flow

interface EqualizerRepository {
    fun observeEqualizerState(): Flow<EqualizerState>

    suspend fun setEnabled(enabled: Boolean)

    suspend fun setMode(mode: EqualizerMode)

    suspend fun selectPreset(preset: EqualizerPreset)

    suspend fun setBandLevel(bandId: Short, level: Int)

    suspend fun setBandLevelTransient(bandId: Short, level: Int)

    suspend fun refreshCapabilities()
}
