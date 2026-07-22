package com.catlytics.core.model

sealed interface SleepTimerState {
    data object Inactive : SleepTimerState

    data class Active(
        val totalDurationMillis: Long,
        val remainingMillis: Long,
    ) : SleepTimerState
}
