package com.catlytics.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException

/**
 * Reads one value from the store, treating an unreadable file as empty. Emits only when that
 * value changes, not on every write to an unrelated key.
 */
internal fun <T> DataStore<Preferences>.observe(read: (Preferences) -> T): Flow<T> = data
    .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
    .map(read)
    .distinctUntilChanged()

internal inline fun <reified T : Enum<T>> String?.toEnumOr(default: T): T =
    this?.let { stored -> enumValues<T>().firstOrNull { it.name == stored } } ?: default
