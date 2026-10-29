package com.catlytics.feature.library.impl.collection

import com.catlytics.core.domain.repository.LibraryPreferencesRepository
import com.catlytics.core.domain.repository.PlaybackEventRepository
import com.catlytics.core.domain.usecase.library.ObserveDetailSortSettingsUseCase
import com.catlytics.core.domain.usecase.library.ObserveTrackPlayCountsUseCase
import com.catlytics.core.domain.usecase.library.SetAlbumTrackSortUseCase
import com.catlytics.core.domain.usecase.library.SetArtistAlbumSortUseCase
import com.catlytics.core.domain.usecase.library.SetArtistTrackSortUseCase
import com.catlytics.core.model.DetailSortSettings
import kotlinx.coroutines.flow.MutableStateFlow
import java.lang.reflect.Proxy

internal class SortTestFakes {
    val settings = MutableStateFlow(DetailSortSettings())
    val playCounts = MutableStateFlow(emptyMap<String, Int>())

    private val preferences: LibraryPreferencesRepository = proxy { name, args ->
        when (name) {
            "observeDetailSortSettings" -> settings
            "setAlbumTrackSort" -> settings.value = settings.value.copy(
                albumTracks = args[0] as com.catlytics.core.model.AlbumTrackSort,
            )
            "setArtistTrackSort" -> settings.value = settings.value.copy(
                artistTracks = args[0] as com.catlytics.core.model.ArtistTrackSort,
            )
            "setArtistAlbumSort" -> settings.value = settings.value.copy(
                artistAlbums = args[0] as com.catlytics.core.model.ArtistAlbumSort,
            )
            else -> error("Unexpected call $name")
        }
    }

    private val events: PlaybackEventRepository = proxy { name, _ ->
        when (name) {
            "observeTrackPlayCounts" -> playCounts
            else -> error("Unexpected call $name")
        }
    }

    val observeSettings = ObserveDetailSortSettingsUseCase(preferences)
    val observePlayCounts = ObserveTrackPlayCountsUseCase(events)
    val setAlbumTrackSort = SetAlbumTrackSortUseCase(preferences)
    val setArtistTrackSort = SetArtistTrackSortUseCase(preferences)
    val setArtistAlbumSort = SetArtistAlbumSortUseCase(preferences)
}

private inline fun <reified T> proxy(crossinline handler: (String, Array<Any?>) -> Any?): T =
    Proxy.newProxyInstance(T::class.java.classLoader, arrayOf(T::class.java)) { _, method, args ->
        val result = handler(method.name, args ?: emptyArray())
        if (result is Unit) Unit else result
    } as T
