package com.catlytics.core.widget

import android.content.Context
import android.graphics.Bitmap
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.size.Precision
import coil3.toBitmap

private const val WIDGET_ARTWORK_SIZE_PX = 192

internal suspend fun loadWidgetArtwork(context: Context, artworkUri: String): Bitmap? {
    val request = ImageRequest.Builder(context)
        .data(artworkUri)
        .size(WIDGET_ARTWORK_SIZE_PX)
        .precision(Precision.INEXACT)
        .allowHardware(false)
        .build()
    val result = context.imageLoader.execute(request) as? SuccessResult ?: return null
    return result.image.toBitmap()
}
