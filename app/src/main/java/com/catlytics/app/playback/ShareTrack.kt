package com.catlytics.app.playback

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.catlytics.app.R
import com.catlytics.core.model.Track
import androidx.core.net.toUri

internal fun Context.shareTrack(track: Track) {
    val mediaUri = track.mediaUri.toUri()
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "audio/*"
        putExtra(Intent.EXTRA_STREAM, mediaUri)
        clipData = ClipData.newUri(contentResolver, track.title, mediaUri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    runCatching {
        startActivity(Intent.createChooser(shareIntent, getString(R.string.app_action_share_track)))
    }
}
