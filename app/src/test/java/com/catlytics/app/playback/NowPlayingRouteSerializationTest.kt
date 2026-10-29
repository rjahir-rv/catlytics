package com.catlytics.app.playback

import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.serialization.NavKeySerializer
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class NowPlayingRouteSerializationTest {
    @Test
    fun nowPlayingRouteSurvivesSavedStateRoundTrip() {
        val serializer = NavKeySerializer<NavKey>()

        val restored = Json.decodeFromString(serializer, Json.encodeToString(serializer, NowPlayingRoute))

        assertEquals(NowPlayingRoute, restored)
    }
}
