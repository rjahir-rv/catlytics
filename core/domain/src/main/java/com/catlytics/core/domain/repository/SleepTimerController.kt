package com.catlytics.core.domain.repository

import com.catlytics.core.model.SleepTimerState
import kotlinx.coroutines.flow.StateFlow

interface SleepTimerController {
    val state: StateFlow<SleepTimerState>

    fun start(durationMinutes: Int)

    fun cancel()
}
