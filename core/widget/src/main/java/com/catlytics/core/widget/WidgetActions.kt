package com.catlytics.core.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TogglePlaybackAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        withRestoredSession(context) { togglePlaybackUseCase()() }
    }
}

class SkipNextAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        withRestoredSession(context) { skipPlaybackUseCase().next() }
    }
}

class SkipPreviousAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        withRestoredSession(context) { skipPlaybackUseCase().previous() }
    }
}

private suspend fun withRestoredSession(
    context: Context,
    block: suspend WidgetEntryPoint.() -> Unit,
) {
    val entryPoint = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java)
    entryPoint.nowPlayingWidgetSync().start()
    withContext(Dispatchers.Main) {
        entryPoint.restorePlaybackSessionUseCase()()
        entryPoint.block()
    }
}
