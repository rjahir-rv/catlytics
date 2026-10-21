package com.catlytics.core.domain.repository

import com.catlytics.core.model.AppLanguage
import com.catlytics.core.model.ThemeMode
import kotlinx.coroutines.flow.Flow

interface AppPreferencesRepository {
    fun observeThemeMode(): Flow<ThemeMode>

    suspend fun setThemeMode(themeMode: ThemeMode)

    fun observeAppLanguage(): Flow<AppLanguage>

    suspend fun setAppLanguage(language: AppLanguage)
}
