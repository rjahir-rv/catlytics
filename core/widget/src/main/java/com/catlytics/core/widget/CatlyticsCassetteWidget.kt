package com.catlytics.core.widget

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.RowScope
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontFamily
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first
import com.catlytics.core.designsystem.locale.resolveLocale
import com.catlytics.core.designsystem.locale.withAppLocale
import com.catlytics.core.designsystem.text.displayArtistName
import com.catlytics.core.designsystem.text.displayTrackTitle
import com.catlytics.core.model.AppLanguage
import com.catlytics.core.designsystem.R as DesignR

private val COMPACT_SIZE = DpSize(250.dp, 60.dp)
private val FULL_SIZE = DpSize(250.dp, 170.dp)
private val FULL_TALL_SIZE = DpSize(250.dp, 220.dp)
private val FULL_XTALL_SIZE = DpSize(250.dp, 280.dp)
private val FULL_FIXED_HEIGHT = 132.dp
private const val NOW_PLAYING_URI = "catlytics://nowplaying"

class CatlyticsCassetteWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(
        setOf(COMPACT_SIZE, FULL_SIZE, FULL_TALL_SIZE, FULL_XTALL_SIZE),
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java)
        val store = entryPoint.nowPlayingWidgetStore()
        val sync = entryPoint.nowPlayingWidgetSync()
        val themeModes = entryPoint.appPreferencesRepository().observeThemeMode()
        val appLanguages = entryPoint.appPreferencesRepository().observeAppLanguage()

        val initialState = store.current()
        val initialThemeMode = themeModes.first()
        val initialAppLanguage = appLanguages.first()
        val initialArtwork = initialState.artworkUri?.let { loadWidgetArtwork(context, it) }

        provideContent {
            val state by store.observe().collectAsState(initialState)
            val themeMode by themeModes.collectAsState(initialThemeMode)
            val appLanguage by appLanguages.collectAsState(initialAppLanguage)
            val syncRunning by sync.isRunning.collectAsState()
            var artwork by remember { mutableStateOf(initialState.artworkUri to initialArtwork) }
            val localizedContext = remember(appLanguage) {
                if (appLanguage == AppLanguage.System) {
                    context
                } else {
                    context.withAppLocale(appLanguage.resolveLocale())
                }
            }

            LaunchedEffect(state.artworkUri) {
                val uri = state.artworkUri
                if (artwork.first != uri) {
                    artwork = uri to uri?.let { loadWidgetArtwork(context, it) }
                }
            }

            CompositionLocalProvider(LocalContext provides localizedContext) {
                CassetteWidgetContent(
                    state = state,
                    isPlaying = state.isPlaying && syncRunning,
                    artwork = artwork.second.takeIf { artwork.first == state.artworkUri },
                    palette = widgetPalette(themeMode),
                )
            }
        }
    }
}

@Composable
private fun CassetteWidgetContent(
    state: NowPlayingWidgetState,
    isPlaying: Boolean,
    artwork: Bitmap?,
    palette: WidgetPalette,
) {
    val context = LocalContext.current
    val openApp = actionStartActivity(openAppIntent(context, state.hasTrack))
    val modifier = GlanceModifier
        .fillMaxSize()
        .appWidgetBackground()
        .cornerRadius(20.dp)
        .tintedBackground(R.drawable.widget_bg_body, palette.body)
        .clickable(openApp)

    if (LocalSize.current.height >= FULL_SIZE.height) {
        FullCassette(state, isPlaying, artwork, palette, openApp, modifier)
    } else {
        CompactCassette(state, isPlaying, artwork, palette, openApp, modifier)
    }
}

@Composable
private fun FullCassette(
    state: NowPlayingWidgetState,
    isPlaying: Boolean,
    artwork: Bitmap?,
    palette: WidgetPalette,
    openApp: Action,
    modifier: GlanceModifier,
) {
    val reelSize = (LocalSize.current.height - FULL_FIXED_HEIGHT - 10.dp).coerceIn(28.dp, 72.dp)
    Box(modifier = modifier) {
        Screws(palette)
        Column(modifier = GlanceModifier.fillMaxSize().padding(10.dp)) {
            Column(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .defaultWeight()
                    .tintedBackground(R.drawable.widget_bg_label, palette.labelPaper)
                    .padding(6.dp),
            ) {
                LabelStripe(state.hasTrack, isPlaying, palette)
                Column(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                ) {
                    TrackText(state, palette, titleSize = 14, artistSize = 12)
                }
                CassetteWindow(
                    artwork = artwork,
                    palette = palette,
                    reelSize = reelSize,
                    modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                )
            }
            Spacer(modifier = GlanceModifier.height(8.dp))
            TransportKeys(
                state = state,
                isPlaying = isPlaying,
                palette = palette,
                openApp = openApp,
                keyWidth = null,
                keyHeight = 30.dp,
                spacing = 6.dp,
                modifier = GlanceModifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun CompactCassette(
    state: NowPlayingWidgetState,
    isPlaying: Boolean,
    artwork: Bitmap?,
    palette: WidgetPalette,
    openApp: Action,
    modifier: GlanceModifier,
) {
    Row(
        modifier = modifier.padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = GlanceModifier
                .size(44.dp)
                .tintedBackground(R.drawable.widget_bg_window, palette.window),
            contentAlignment = Alignment.Center,
        ) {
            if (artwork != null) {
                Artwork(artwork, size = 36.dp)
            } else {
                Reel(palette, size = 32.dp)
            }
        }
        Spacer(modifier = GlanceModifier.width(8.dp))
        Column(modifier = GlanceModifier.defaultWeight()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Dot(if (isPlaying) palette.ledOn else palette.ledOff, size = 6.dp)
                Spacer(modifier = GlanceModifier.width(4.dp))
                Text(
                    text = LocalContext.current.getString(R.string.widget_brand),
                    maxLines = 1,
                    style = monoStyle(palette.labelSubtext, size = 9),
                )
            }
            TrackText(state, palette, titleSize = 13, artistSize = 11)
        }
        Spacer(modifier = GlanceModifier.width(6.dp))
        TransportKeys(
            state = state,
            isPlaying = isPlaying,
            palette = palette,
            openApp = openApp,
            keyWidth = 36.dp,
            keyHeight = 36.dp,
            spacing = 4.dp,
            modifier = GlanceModifier,
        )
    }
}

/** Cuatro tornillos en las esquinas de la carcasa. */
@Composable
private fun Screws(palette: WidgetPalette) {
    Column(modifier = GlanceModifier.fillMaxSize().padding(3.dp)) {
        ScrewRow(palette)
        Spacer(modifier = GlanceModifier.defaultWeight())
        ScrewRow(palette)
    }
}

@Composable
private fun ScrewRow(palette: WidgetPalette) {
    Row(modifier = GlanceModifier.fillMaxWidth()) {
        TintedImage(R.drawable.widget_screw, palette.screw, GlanceModifier.size(6.dp))
        Spacer(modifier = GlanceModifier.defaultWeight())
        TintedImage(R.drawable.widget_screw, palette.screw, GlanceModifier.size(6.dp))
    }
}


@Composable
private fun LabelStripe(hasTrack: Boolean, isPlaying: Boolean, palette: WidgetPalette) {
    val context = LocalContext.current
    val status = when {
        !hasTrack -> R.string.widget_status_empty
        isPlaying -> R.string.widget_status_playing
        else -> R.string.widget_status_paused
    }
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .height(18.dp)
            .tintedBackground(R.drawable.widget_bg_stripe, palette.labelStripe)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = context.getString(R.string.widget_brand),
            maxLines = 1,
            style = monoStyle(palette.onLabelStripe, size = 10, bold = true),
        )
        Spacer(modifier = GlanceModifier.defaultWeight())
        Dot(if (isPlaying) palette.onLabelStripe else palette.stripeLedOff, size = 6.dp)
        Spacer(modifier = GlanceModifier.width(4.dp))
        Text(
            text = context.getString(status),
            maxLines = 1,
            style = monoStyle(palette.onLabelStripe, size = 9),
        )
    }
}

@Composable
private fun TrackText(
    state: NowPlayingWidgetState,
    palette: WidgetPalette,
    titleSize: Int,
    artistSize: Int,
) {
    val context = LocalContext.current
    Text(
        text = if (state.hasTrack) {
            context.displayTrackTitle(state.title)
        } else {
            context.getString(R.string.widget_empty_title)
        },
        maxLines = 1,
        style = TextStyle(
            color = palette.labelText,
            fontSize = titleSize.sp,
            fontWeight = FontWeight.Bold,
        ),
    )
    Text(
        text = if (state.hasTrack) {
            context.displayArtistName(state.artist)
        } else {
            context.getString(R.string.widget_empty_subtitle)
        },
        maxLines = 1,
        style = TextStyle(color = palette.labelSubtext, fontSize = artistSize.sp),
    )
}

/** Ventana del casete: dos bobinas y, entre ellas, la carátula como si se viera la cinta. */
@Composable
private fun CassetteWindow(
    artwork: Bitmap?,
    palette: WidgetPalette,
    reelSize: Dp,
    modifier: GlanceModifier,
) {
    Row(
        modifier = modifier
            .tintedBackground(R.drawable.widget_bg_window, palette.window)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Reel(palette, size = reelSize)
        Spacer(modifier = GlanceModifier.defaultWeight())
        if (artwork != null) {
            Artwork(artwork, size = reelSize + 2.dp)
        } else {
            TintedImage(DesignR.drawable.ic_audio, palette.reel, GlanceModifier.size(14.dp))
        }
        Spacer(modifier = GlanceModifier.defaultWeight())
        Reel(palette, size = reelSize)
    }
}

@Composable
private fun TransportKeys(
    state: NowPlayingWidgetState,
    isPlaying: Boolean,
    palette: WidgetPalette,
    openApp: Action,
    keyWidth: Dp?,
    keyHeight: Dp,
    spacing: Dp,
    modifier: GlanceModifier,
) {
    val context = LocalContext.current
    // Sin cinta no hay nada que controlar: las teclas abren la app para elegir música.
    fun actionOrOpen(action: Action) = if (state.hasTrack) action else openApp

    Row(modifier = modifier.height(keyHeight)) {
        TransportKey(
            icon = DesignR.drawable.ic_skip_back_fill,
            contentDescription = context.getString(R.string.widget_previous),
            onClick = actionOrOpen(actionRunCallback<SkipPreviousAction>()),
            background = palette.key,
            tint = palette.keyIcon,
            width = keyWidth,
        )
        Spacer(modifier = GlanceModifier.width(spacing))
        TransportKey(
            icon = if (isPlaying) DesignR.drawable.ic_pause_fill else DesignR.drawable.ic_play_fill,
            contentDescription = context.getString(
                if (isPlaying) R.string.widget_pause else R.string.widget_play,
            ),
            onClick = actionOrOpen(actionRunCallback<TogglePlaybackAction>()),
            background = palette.accentKey,
            tint = palette.onAccentKey,
            width = keyWidth,
        )
        Spacer(modifier = GlanceModifier.width(spacing))
        TransportKey(
            icon = DesignR.drawable.ic_skip_next_fill,
            contentDescription = context.getString(R.string.widget_next),
            onClick = actionOrOpen(actionRunCallback<SkipNextAction>()),
            background = palette.key,
            tint = palette.keyIcon,
            width = keyWidth,
        )
    }
}

/** Tecla mecánica; sin ancho fijo se reparte la fila a partes iguales. */
@Composable
private fun RowScope.TransportKey(
    @DrawableRes icon: Int,
    contentDescription: String,
    onClick: Action,
    background: ColorProvider,
    tint: ColorProvider,
    width: Dp?,
) {
    val sizeModifier = if (width != null) {
        GlanceModifier.width(width)
    } else {
        GlanceModifier.defaultWeight()
    }
    Box(
        modifier = sizeModifier
            .fillMaxHeight()
            .tintedBackground(R.drawable.widget_bg_key, background)
            .clickable(onClick),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            provider = ImageProvider(icon),
            contentDescription = contentDescription,
            colorFilter = ColorFilter.tint(tint),
            modifier = GlanceModifier.size(14.dp),
        )
    }
}

@Composable
private fun Reel(palette: WidgetPalette, size: Dp) {
    TintedImage(R.drawable.widget_reel, palette.reel, GlanceModifier.size(size))
}

@Composable
private fun Dot(color: ColorProvider, size: Dp) {
    TintedImage(R.drawable.widget_dot, color, GlanceModifier.size(size))
}

@Composable
private fun Artwork(artwork: Bitmap, size: Dp) {
    Image(
        provider = ImageProvider(artwork),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = GlanceModifier.size(size).cornerRadius(4.dp),
    )
}

@Composable
private fun TintedImage(@DrawableRes drawable: Int, tint: ColorProvider, modifier: GlanceModifier) {
    Image(
        provider = ImageProvider(drawable),
        contentDescription = null,
        colorFilter = ColorFilter.tint(tint),
        modifier = modifier,
    )
}

private fun GlanceModifier.tintedBackground(@DrawableRes shape: Int, color: ColorProvider): GlanceModifier =
    background(
        imageProvider = ImageProvider(shape),
        colorFilter = ColorFilter.tint(color),
    )

private fun monoStyle(color: ColorProvider, size: Int, bold: Boolean = false) = TextStyle(
    color = color,
    fontSize = size.sp,
    fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
    fontFamily = FontFamily.Monospace,
)

private fun openAppIntent(context: Context, hasTrack: Boolean): Intent {
    val intent = if (hasTrack) {
        Intent(Intent.ACTION_VIEW, NOW_PLAYING_URI.toUri()).setPackage(context.packageName)
    } else {
        context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?: Intent(Intent.ACTION_MAIN).setPackage(context.packageName)
    }
    return intent.addFlags(
        Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP,
    )
}
