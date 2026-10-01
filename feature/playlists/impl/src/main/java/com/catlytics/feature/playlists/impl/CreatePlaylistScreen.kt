package com.catlytics.feature.playlists.impl

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import com.catlytics.core.designsystem.R
import com.catlytics.core.designsystem.component.ArtworkGradientBackground
import com.catlytics.core.designsystem.component.ArtworkGradientColors
import com.catlytics.core.designsystem.component.animateArtworkGradientColors
import com.catlytics.core.designsystem.component.extractArtworkGradientColors
import com.catlytics.core.model.Playlist
import com.catlytics.feature.playlists.impl.R as PlaylistsR

@Composable
internal fun CreatePlaylistScreen(
    playlists: List<Playlist>,
    initialTrackCount: Int,
    isSaving: Boolean,
    onClose: () -> Unit,
    onCreate: (name: String, description: String, artworkUri: String?) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var artworkUri by rememberSaveable { mutableStateOf<String?>(null) }
    val defaultNameFormat = stringResource(PlaylistsR.string.create_playlist_default_name)
    val defaultName = remember(playlists, defaultNameFormat) {
        val format = { number: Int -> defaultNameFormat.format(number) }
        format(nextDefaultPlaylistNumber(playlists, format))
    }
    val effectiveName = name.ifBlank { defaultName }
    val nameTaken = isPlaylistNameTaken(effectiveName, playlists)
    val canCreate = !nameTaken && !isSaving

    val coverPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri: Uri? ->
        if (uri != null) artworkUri = uri.toString()
    }
    fun pickCover() {
        coverPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    val gradientColors = animateArtworkGradientColors(
        target = rememberCreatePlaylistGradient(
            seed = effectiveName,
            artworkUri = artworkUri,
        ),
        labelPrefix = "createPlaylist",
    )
    val nameFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) { nameFocusRequester.requestFocus() }
    val entrance = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        entrance.animateTo(1f, spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessLow))
    }

    ArtworkGradientBackground(colors = gradientColors) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onClose) {
                    Icon(
                        painter = painterResource(R.drawable.ic_close),
                        contentDescription = stringResource(
                            PlaylistsR.string.create_playlist_close_content_description,
                        ),
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Button(
                    onClick = {
                        keyboardController?.hide()
                        onCreate(effectiveName, description, artworkUri)
                    },
                    enabled = canCreate,
                    modifier = Modifier.padding(end = 8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.onSurface,
                        contentColor = MaterialTheme.colorScheme.surface,
                    ),
                ) {
                    Text(stringResource(PlaylistsR.string.create_playlist_action_create))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = stringResource(PlaylistsR.string.create_playlist_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                modifier = Modifier.graphicsLayer { alpha = entrance.value.coerceIn(0f, 1f) },
            )
            Spacer(modifier = Modifier.height(24.dp))

            CreatePlaylistCover(
                artworkUri = artworkUri,
                onPick = ::pickCover,
                onRemove = { artworkUri = null },
                modifier = Modifier.graphicsLayer {
                    val progress = entrance.value
                    alpha = progress.coerceIn(0f, 1f)
                    scaleX = 0.85f + 0.15f * progress
                    scaleY = 0.85f + 0.15f * progress
                },
            )

            Spacer(modifier = Modifier.height(36.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
                    .graphicsLayer {
                        val progress = entrance.value
                        alpha = progress.coerceIn(0f, 1f)
                        translationY = (1f - progress) * 24.dp.toPx()
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CreatePlaylistTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = defaultName,
                    textStyle = MaterialTheme.typography.headlineMedium.copy(textAlign = TextAlign.Center),
                    isError = nameTaken,
                    imeAction = ImeAction.Next,
                    modifier = Modifier.focusRequester(nameFocusRequester),
                )
                AnimatedVisibility(
                    visible = nameTaken,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically(),
                ) {
                    Text(
                        text = stringResource(PlaylistsR.string.create_playlist_name_taken),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
                CreatePlaylistTextField(
                    value = description,
                    onValueChange = { description = it },
                    placeholder = stringResource(PlaylistsR.string.create_playlist_description_placeholder),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(textAlign = TextAlign.Center),
                    isError = false,
                    imeAction = ImeAction.Done,
                    onDone = {
                        keyboardController?.hide()
                        if (canCreate) onCreate(effectiveName, description, artworkUri)
                    },
                )
                if (initialTrackCount > 0) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                    ) {
                        Text(
                            text = pluralStringResource(
                                PlaylistsR.plurals.playlists_track_count,
                                initialTrackCount,
                                initialTrackCount,
                            ),
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        )
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun CreatePlaylistCover(
    artworkUri: String?,
    onPick: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(24.dp)
    val scale = remember { Animatable(1f) }
    LaunchedEffect(artworkUri) {
        scale.snapTo(0.92f)
        scale.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMediumLow))
    }

    Box(modifier = modifier.size(208.dp)) {
        Box(
            modifier = Modifier
                .size(200.dp)
                .align(Alignment.Center)
                .graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                }
                .shadow(elevation = 24.dp, shape = shape)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .clickable(onClick = onPick),
            contentAlignment = Alignment.Center,
        ) {
            AnimatedContent(
                targetState = artworkUri,
                transitionSpec = {
                    (fadeIn(tween(300)) + scaleIn(initialScale = 1.08f)) togetherWith fadeOut(tween(200))
                },
                label = "createPlaylistCover",
            ) { uri ->
                if (uri != null) {
                    AsyncImage(
                        model = uri,
                        contentDescription = stringResource(
                            PlaylistsR.string.playlist_detail_artwork_preview_content_description,
                        ),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Image(
                            painter = painterResource(R.drawable.placeholder_playlist),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                            alpha = 0.55f,
                        )
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_add),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .padding(12.dp)
                                        .size(24.dp),
                                )
                            }
                            Text(
                                text = stringResource(PlaylistsR.string.create_playlist_add_cover),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }
        }
        AnimatedVisibility(
            visible = artworkUri != null,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier.align(Alignment.TopEnd),
        ) {
            Surface(
                onClick = onRemove,
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 4.dp,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_close),
                    contentDescription = stringResource(PlaylistsR.string.playlists_remove_cover),
                    modifier = Modifier
                        .padding(6.dp)
                        .size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun CreatePlaylistTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    textStyle: TextStyle,
    isError: Boolean,
    imeAction: ImeAction,
    modifier: Modifier = Modifier,
    onDone: () -> Unit = {},
) {
    val contentColor = MaterialTheme.colorScheme.onSurface
    var focused by remember { mutableStateOf(false) }
    val underlineColor by animateColorAsState(
        targetValue = when {
            isError -> MaterialTheme.colorScheme.error
            focused -> contentColor
            else -> contentColor.copy(alpha = 0.25f)
        },
        label = "createPlaylistUnderline",
    )
    val underlineWidth by animateFloatAsState(
        targetValue = if (focused || isError) 1f else 0.6f,
        label = "createPlaylistUnderlineWidth",
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = textStyle.copy(color = contentColor),
            cursorBrush = SolidColor(contentColor),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = imeAction,
            ),
            keyboardActions = KeyboardActions(onDone = { onDone() }),
            singleLine = true,
            modifier = modifier
                .fillMaxWidth()
                .onFocusChanged { focused = it.isFocused },
            decorationBox = { innerTextField ->
                Box(contentAlignment = Alignment.Center) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = textStyle,
                            color = contentColor.copy(alpha = 0.4f),
                            maxLines = 1,
                        )
                    }
                    innerTextField()
                }
            },
        )
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth(underlineWidth)
                .height(if (focused) 2.dp else 1.dp)
                .background(underlineColor, CircleShape),
        )
    }
}

/**
 * Degradado de fondo: con portada usa su paleta; sin portada deriva un tono estable del nombre
 * para que el color "siga" lo que escribe la persona.
 */
@Composable
private fun rememberCreatePlaylistGradient(
    seed: String,
    artworkUri: String?,
): ArtworkGradientColors {
    val colorScheme = MaterialTheme.colorScheme
    val isDark = colorScheme.surface.luminance() < 0.5f
    val fallback = ArtworkGradientColors(
        start = colorScheme.surfaceContainerHighest,
        center = colorScheme.surfaceContainer,
        end = colorScheme.surface,
    )
    val nameGradient = remember(seed, isDark, colorScheme.surface) {
        nameSeededGradient(seed, isDark, colorScheme.surface)
    }
    var artworkGradient by remember { mutableStateOf<Pair<String, ArtworkGradientColors>?>(null) }
    val context = LocalPlatformContext.current
    LaunchedEffect(artworkUri, isDark) {
        if (artworkUri == null) return@LaunchedEffect
        val request = ImageRequest.Builder(context)
            .data(artworkUri)
            .allowHardware(false)
            .size(256)
            .build()
        val result = context.imageLoader.execute(request) as? SuccessResult ?: return@LaunchedEffect
        artworkGradient = artworkUri to result.image.toBitmap().extractArtworkGradientColors(
            fallback = fallback,
            surfaceBlend = 0.4f,
        )
    }
    return artworkGradient
        ?.takeIf { (uri, _) -> uri == artworkUri }
        ?.second
        ?: nameGradient
}

internal fun nameSeededGradient(
    seed: String,
    isDark: Boolean,
    surface: Color,
): ArtworkGradientColors {
    val hue = ((seed.trim().lowercase().hashCode() % 360) + 360) % 360
    return if (isDark) {
        ArtworkGradientColors(
            start = Color.hsl(hue.toFloat(), 0.55f, 0.40f),
            center = Color.hsl(((hue + 28) % 360).toFloat(), 0.45f, 0.22f),
            end = surface,
        )
    } else {
        ArtworkGradientColors(
            start = Color.hsl(hue.toFloat(), 0.65f, 0.74f),
            center = Color.hsl(((hue + 28) % 360).toFloat(), 0.55f, 0.87f),
            end = surface,
        )
    }
}
