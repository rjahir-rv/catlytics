package com.catlytics.core.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import com.catlytics.core.domain.repository.AppPreferencesRepository
import com.catlytics.core.domain.repository.PlaybackController
import com.catlytics.core.domain.repository.PlaybackSessionRepository
import dagger.Lazy
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@Singleton
class NowPlayingWidgetSync @Inject constructor(
    @ApplicationContext private val context: Context,
    private val playbackController: Lazy<PlaybackController>,
    private val playbackSessionRepository: PlaybackSessionRepository,
    private val appPreferencesRepository: AppPreferencesRepository,
    private val store: NowPlayingWidgetStore,
) {
    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val running = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = running.asStateFlow()

    fun start() {
        if (!running.compareAndSet(expect = false, update = true)) return

        syncScope.launch {
            var isFirstUpdate = true
            combine(
                playbackController.get().playbackState.map { it.toWidgetState() },
                playbackSessionRepository.observeSession().map { it != null },
            ) { live, hasSavedSession -> live to hasSavedSession }
                .distinctUntilChanged()
                .collect { (live, hasSavedSession) ->
                    val cached = store.current()
                    val state = resolveWidgetState(live, hasSavedSession, cached)
                    if (state != cached) {
                        store.save(state)
                    } else if (!isFirstUpdate) {
                        return@collect
                    }
                    isFirstUpdate = false
                    CatlyticsCassetteWidget().updateAll(context)
                }
        }
        syncScope.launch {
            appPreferencesRepository.observeThemeMode()
                .distinctUntilChanged()
                .collect { CatlyticsCassetteWidget().updateAll(context) }
        }
        syncScope.launch {
            appPreferencesRepository.observeAppLanguage()
                .distinctUntilChanged()
                .collect { CatlyticsCassetteWidget().updateAll(context) }
        }
    }
}
