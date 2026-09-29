package com.catlytics.core.designsystem.format

import java.util.Locale

/**
 * Formato numérico de duración de pista (`m:ss` / `h:mm:ss`).
 *
 * No es copy traducible: son separadores y dígitos, idénticos en todos los idiomas.
 * Vive aquí para evitar las copias duplicadas en cada feature.
 */
object TrackDurationFormat {

    /** Formato `m:ss` y `h:mm:ss` cuando la duración supera una hora. */
    fun format(durationMillis: Long): String {
        val totalSeconds = durationMillis.millisecondsToSeconds()
        val hours = totalSeconds / 3_600
        val minutes = (totalSeconds % 3_600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%d:%02d", minutes, seconds)
        }
    }

    /** Formato `mm:ss` acumulando minutos totales (p. ej. `75:03`). */
    fun formatMinutesSeconds(durationMillis: Long): String {
        val totalSeconds = durationMillis.millisecondsToSeconds()
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.US, "%d:%02d", minutes, seconds)
    }

    private fun Long.millisecondsToSeconds(): Long = this / 1_000L
}
