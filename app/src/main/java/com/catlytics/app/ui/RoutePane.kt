package com.catlytics.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.TopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.NavKey
import com.catlytics.app.ui.chrome.StatusBarProtection
import com.catlytics.core.designsystem.component.animateArtworkChromeColor
import com.catlytics.core.designsystem.layout.LocalPaneChrome
import com.catlytics.core.designsystem.layout.PaneChrome
import com.catlytics.core.navigation.TopLevelBackStack
import com.catlytics.feature.library.api.LibraryAlbumRoute
import com.catlytics.feature.library.api.LibraryArtistRoute
import com.catlytics.feature.playlists.api.PlaylistDetailRoute

@Stable
internal class DetailChromeState(backStack: () -> List<NavKey>) {
    val colors = mutableStateMapOf<NavKey, Color>()
    val titles = mutableStateMapOf<NavKey, String>()

    val pruneDecorator = NavEntryDecorator<NavKey>(
        onPop = {
            val keys = backStack().toSet()
            colors.keys.retainAll(keys)
            titles.keys.retainAll(keys)
        },
        decorate = { entry -> entry.Content() },
    )

    fun chromeColor(route: NavKey): Color? = when (route) {
        is LibraryAlbumRoute, is LibraryArtistRoute, is PlaylistDetailRoute -> colors[route]
        else -> null
    }
}

@Composable
internal fun rememberDetailChromeState(topLevelBackStack: TopLevelBackStack): DetailChromeState =
    remember(topLevelBackStack) { DetailChromeState { topLevelBackStack.backStack } }

/**
 * Scaffold de una pantalla: barra superior propia con scroll `enterAlways` independiente y
 * fondo del panel. El contenido se dibuja detrás de la barra y recibe su alto por [LocalPaneChrome].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RoutePane(
    chromeColor: Color?,
    statusBarHeight: Dp,
    canScroll: Boolean,
    scrollResetKey: Any?,
    showPaneDivider: Boolean,
    topBar: @Composable (TopAppBarScrollBehavior) -> Unit,
    content: @Composable () -> Unit,
) {
    val containerColor = animateArtworkChromeColor(
        target = chromeColor,
        fallback = MaterialTheme.colorScheme.background,
    )
    val topBarState = rememberSaveable(saver = TopAppBarState.Saver) {
        TopAppBarState(
            initialHeightOffsetLimit = -Float.MAX_VALUE,
            initialHeightOffset = 0f,
            initialContentOffset = 0f,
        )
    }
    val currentCanScroll by rememberUpdatedState(canScroll)
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(
        state = topBarState,
        canScroll = remember { { currentCanScroll } },
    )
    LaunchedEffect(scrollResetKey) {
        topBarState.heightOffset = 0f
        topBarState.contentOffset = 0f
    }
    val dividerColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = PANE_DIVIDER_ALPHA)
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .then(
                if (showPaneDivider) {
                    Modifier.drawWithContent {
                        drawContent()
                        drawRect(color = dividerColor, size = Size(PANE_DIVIDER_WIDTH.toPx(), size.height))
                    }
                } else {
                    Modifier
                },
            ),
        containerColor = containerColor,
        contentColor = MaterialTheme.colorScheme.onBackground,
        contentWindowInsets = WindowInsets.systemBars
            .union(WindowInsets.displayCutout)
            .only(WindowInsetsSides.Horizontal),
        topBar = {
            TopBarPaneSegment(
                statusBarColor = containerColor,
                collapsedFraction = { scrollBehavior.state.collapsedFraction },
            ) {
                topBar(scrollBehavior)
            }
        },
    ) { innerPadding ->
        // collapsedFraction se lee al usar el padding, no aquí: el scroll de la barra no recompone
        // el panel en cada frame.
        val paneChrome = remember(innerPadding, statusBarHeight, topBarState) {
            PaneChrome(
                contentPadding = TopBarContentPadding(
                    scaffoldPadding = innerPadding,
                    statusBarPadding = statusBarHeight,
                    collapsedFraction = { topBarState.collapsedFraction },
                ),
                collapsedFraction = { topBarState.collapsedFraction },
            )
        }
        CompositionLocalProvider(LocalPaneChrome provides paneChrome) {
            content()
        }
    }
}

/** Segmento de la barra superior con su propia protección de la barra de estado. */
@Composable
private fun TopBarPaneSegment(
    statusBarColor: Color,
    collapsedFraction: () -> Float,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(modifier = modifier) {
        StatusBarProtection(
            color = statusBarColor,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .graphicsLayer { alpha = collapsedFraction() },
        )
        content()
    }
}

/** Padding del contenido bajo la barra superior; el superior depende del colapso de la barra. */
private class TopBarContentPadding(
    private val scaffoldPadding: PaddingValues,
    private val statusBarPadding: Dp,
    private val collapsedFraction: () -> Float,
) : PaddingValues {
    override fun calculateLeftPadding(layoutDirection: LayoutDirection): Dp =
        scaffoldPadding.calculateLeftPadding(layoutDirection)

    override fun calculateTopPadding(): Dp = calculateTopContentPadding(
        scaffoldTopPadding = scaffoldPadding.calculateTopPadding(),
        statusBarPadding = statusBarPadding,
        collapsedFraction = collapsedFraction(),
    )

    override fun calculateRightPadding(layoutDirection: LayoutDirection): Dp =
        scaffoldPadding.calculateRightPadding(layoutDirection)

    override fun calculateBottomPadding(): Dp = 0.dp
}

internal fun calculateTopContentPadding(
    scaffoldTopPadding: Dp,
    statusBarPadding: Dp,
    collapsedFraction: Float,
): Dp =
    (scaffoldTopPadding - statusBarPadding * collapsedFraction.coerceIn(0f, 1f))
        .coerceAtLeast(0.dp)

private const val PANE_DIVIDER_ALPHA = 0.5f
private val PANE_DIVIDER_WIDTH = 1.dp
