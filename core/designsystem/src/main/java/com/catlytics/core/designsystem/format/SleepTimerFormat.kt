package com.catlytics.core.designsystem.format

import java.util.Locale
import kotlin.math.ceil

/**
 * Cuenta regresiva del temporizador de sueño (`mm:ss` / `h:mm:ss`).
 *
 * Redondea hacia arriba los segundos para que nunca muestre `00:00` mientras quede tiempo.
 */
object SleepTimerFormat {

    fun formatRemaining(remainingMillis: Long): String {
        val totalSeconds = ceil(remainingMillis.coerceAtLeast(0L) / 1_000.0).toLong()
        val hours = totalSeconds / 3_600L
        val minutes = totalSeconds % 3_600L / 60L
        val seconds = totalSeconds % 60L
        return if (hours > 0L) {
            String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%02d:%02d", minutes, seconds)
        }
    }
}
