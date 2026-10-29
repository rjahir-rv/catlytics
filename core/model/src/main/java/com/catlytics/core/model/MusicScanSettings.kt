package com.catlytics.core.model

data class MusicScanSettings(
    val durationFilter: MusicScanDurationFilter = MusicScanDurationFilter.Disabled,
    val sizeFilter: MusicScanSizeFilter = MusicScanSizeFilter.Disabled,
)

enum class MusicScanDurationFilter(
    val minimumDurationMillis: Long?,
) {
    Disabled(null),
    Seconds30(30_000L),
    Seconds60(60_000L),
}

enum class MusicScanSizeFilter(
    val minimumSizeBytes: Long?,
) {
    Disabled(null),
    Kilobytes500(500L * 1_024L),
    Megabyte1(1_024L * 1_024L),
}
