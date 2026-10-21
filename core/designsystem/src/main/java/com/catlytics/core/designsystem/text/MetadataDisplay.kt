package com.catlytics.core.designsystem.text

import android.content.Context
import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.catlytics.core.designsystem.R
import com.catlytics.core.model.UNKNOWN_ALBUM_TITLE
import com.catlytics.core.model.UNKNOWN_ARTIST_NAME
import com.catlytics.core.model.UNTITLED_TRACK_TITLE

@Composable
fun String.displayTrackTitle(): String =
    if (this == UNTITLED_TRACK_TITLE) stringResource(R.string.ds_untitled_track) else this

@Composable
fun String.displayArtistName(): String =
    if (this == UNKNOWN_ARTIST_NAME) stringResource(R.string.ds_unknown_artist) else this

@Composable
fun String.displayAlbumTitle(): String =
    if (this == UNKNOWN_ALBUM_TITLE) stringResource(R.string.ds_unknown_album) else this

fun Resources.displayTrackTitle(title: String): String =
    if (title == UNTITLED_TRACK_TITLE) getString(R.string.ds_untitled_track) else title

fun Resources.displayArtistName(artistName: String): String =
    if (artistName == UNKNOWN_ARTIST_NAME) getString(R.string.ds_unknown_artist) else artistName

fun Resources.displayAlbumTitle(albumTitle: String): String =
    if (albumTitle == UNKNOWN_ALBUM_TITLE) getString(R.string.ds_unknown_album) else albumTitle

fun Context.displayTrackTitle(title: String): String =
    resources.displayTrackTitle(title)

fun Context.displayArtistName(artistName: String): String =
    resources.displayArtistName(artistName)

fun Context.displayAlbumTitle(albumTitle: String): String =
    resources.displayAlbumTitle(albumTitle)
