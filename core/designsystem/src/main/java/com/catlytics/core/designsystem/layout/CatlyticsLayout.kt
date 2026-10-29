package com.catlytics.core.designsystem.layout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/** Anchos máximos compartidos para que el contenido no se estire en pantallas grandes. */
object CatlyticsLayoutTokens {
    val ListContentMaxWidth: Dp = 840.dp
    val FormMaxWidth: Dp = 560.dp
    val ListPanePreferredWidth: Dp = 400.dp

    /**
     * Ancho mínimo de columna para filas de canciones, artistas y carpetas en grids adaptativos:
     * una columna en teléfonos y paneles angostos, más columnas en pantallas anchas.
     */
    val ListItemMinColumnWidth: Dp = 360.dp
    val CardMinColumnWidth: Dp = 420.dp
    val WideHeaderMinWidth: Dp = 600.dp
    val CompactHeightMax: Dp = 480.dp
}

fun Modifier.centeredContentMaxWidth(maxWidth: Dp): Modifier =
    this
        .fillMaxWidth()
        .wrapContentWidth(Alignment.CenterHorizontally)
        .widthIn(max = maxWidth)

@Composable
fun CenteredListContainer(
    modifier: Modifier = Modifier,
    maxContentWidth: Dp = CatlyticsLayoutTokens.ListContentMaxWidth,
    content: @Composable (horizontalInset: Dp) -> Unit,
) {
    BoxWithConstraints(modifier = modifier) {
        content(((maxWidth - maxContentWidth) / 2).coerceAtLeast(0.dp))
    }
}

fun PaddingValues.plusHorizontal(inset: Dp): PaddingValues =
    if (inset <= 0.dp) this else HorizontalInsetPaddingValues(this, inset)

private class HorizontalInsetPaddingValues(
    private val base: PaddingValues,
    private val inset: Dp,
) : PaddingValues {
    override fun calculateLeftPadding(layoutDirection: LayoutDirection): Dp =
        base.calculateLeftPadding(layoutDirection) + inset

    override fun calculateTopPadding(): Dp = base.calculateTopPadding()

    override fun calculateRightPadding(layoutDirection: LayoutDirection): Dp =
        base.calculateRightPadding(layoutDirection) + inset

    override fun calculateBottomPadding(): Dp = base.calculateBottomPadding()
}

/**
 * Padding cuyos lados [top] y [bottom] se leen al medir, no al componer. Es para insets que cambian
 * a cada frame (la barra superior al colapsar, el teclado, el mini player): así solo se vuelve a
 * medir y la pantalla no se recompone. Sirve como `contentPadding` o en `Modifier.padding`.
 */
fun deferredPadding(
    start: Dp = 0.dp,
    top: () -> Dp = NoPadding,
    end: Dp = 0.dp,
    bottom: () -> Dp = NoPadding,
): PaddingValues = DeferredPaddingValues(start, top, end, bottom)

private val NoPadding: () -> Dp = { 0.dp }

private class DeferredPaddingValues(
    private val start: Dp,
    private val top: () -> Dp,
    private val end: Dp,
    private val bottom: () -> Dp,
) : PaddingValues {
    override fun calculateLeftPadding(layoutDirection: LayoutDirection): Dp =
        if (layoutDirection == LayoutDirection.Ltr) start else end

    override fun calculateTopPadding(): Dp = top()

    override fun calculateRightPadding(layoutDirection: LayoutDirection): Dp =
        if (layoutDirection == LayoutDirection.Ltr) end else start

    override fun calculateBottomPadding(): Dp = bottom()
}

fun PaddingValues.withDeferredBottom(bottom: () -> Dp): PaddingValues =
    DeferredBottomPaddingValues(this, bottom)

private class DeferredBottomPaddingValues(
    private val base: PaddingValues,
    private val bottom: () -> Dp,
) : PaddingValues {
    override fun calculateLeftPadding(layoutDirection: LayoutDirection): Dp =
        base.calculateLeftPadding(layoutDirection)

    override fun calculateTopPadding(): Dp = base.calculateTopPadding()

    override fun calculateRightPadding(layoutDirection: LayoutDirection): Dp =
        base.calculateRightPadding(layoutDirection)

    override fun calculateBottomPadding(): Dp = bottom()
}

@Composable
fun isCompactWindowHeight(): Boolean =
    LocalWindowInfo.current.containerDpSize.height < CatlyticsLayoutTokens.CompactHeightMax

@Composable
fun ArtworkHeaderLayout(
    artwork: @Composable (isWide: Boolean) -> Unit,
    details: @Composable ColumnScope.(isWide: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    wideMinWidth: Dp = CatlyticsLayoutTokens.WideHeaderMinWidth,
    horizontalSpacing: Dp = 24.dp,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
) {
    val isCompactHeight = isCompactWindowHeight()
    BoxWithConstraints(modifier = modifier) {
        val isWide = maxWidth >= wideMinWidth ||
            (isCompactHeight && maxWidth >= MinHorizontalHeaderWidth)
        if (isWide) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(horizontalSpacing),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                artwork(true)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = verticalArrangement,
                ) {
                    details(true)
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = verticalArrangement,
            ) {
                artwork(false)
                details(false)
            }
        }
    }
}

private val MinHorizontalHeaderWidth = 420.dp
