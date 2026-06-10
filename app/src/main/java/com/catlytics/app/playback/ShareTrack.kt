package com.catlytics.app.playback

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.catlytics.core.model.Track

internal fun Context.shareTrack(track: Track) {
    val mediaUri = Uri.parse(track.mediaUri)
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "audio/*"
        putExtra(Intent.EXTRA_STREAM, mediaUri)
        clipData = ClipData.newUri(contentResolver, track.title, mediaUri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    runCatching {
        startActivity(Intent.createChooser(shareIntent, "Compartir canción"))
    }
}
