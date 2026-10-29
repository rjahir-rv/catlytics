package com.catlytics.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.navigation3.runtime.NavKey
import com.catlytics.feature.home.api.HomeRoute
import com.catlytics.feature.library.api.LibraryArtistRoute
import com.catlytics.feature.library.api.LibraryRoute
import com.catlytics.feature.playlists.api.PlaylistsRoute

@Stable
internal class SearchField(isExpanded: Boolean = false, query: String = "") {
    var isExpanded by mutableStateOf(isExpanded)
    var query by mutableStateOf(query)

    fun clear() {
        query = ""
        isExpanded = false
    }
}

@Stable
internal class AppSearchState(
    val home: SearchField = SearchField(),
    val library: SearchField = SearchField(),
    val playlists: SearchField = SearchField(),
    val artist: SearchField = SearchField(),
) {
    private val fields: List<SearchField>
        get() = listOf(home, library, playlists, artist)

    fun fieldFor(route: NavKey): SearchField? = when (route) {
        HomeRoute -> home
        LibraryRoute -> library
        PlaylistsRoute -> playlists
        is LibraryArtistRoute -> artist
        else -> null
    }

    fun isExpanded(route: NavKey): Boolean = fieldFor(route)?.isExpanded == true

    companion object {
        val Saver: Saver<AppSearchState, Any> = listSaver(
            save = { state -> state.fields.flatMap { listOf(it.isExpanded, it.query) } },
            restore = { values ->
                val fields = values.chunked(2) { (isExpanded, query) ->
                    SearchField(isExpanded as Boolean, query as String)
                }
                AppSearchState(fields[0], fields[1], fields[2], fields[3])
            },
        )
    }
}

@Composable
internal fun rememberAppSearchState(): AppSearchState =
    rememberSaveable(saver = AppSearchState.Saver) { AppSearchState() }
