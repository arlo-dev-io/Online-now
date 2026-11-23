package com.arlodev.onlinenow.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.arlodev.onlinenow.AppInfo
import com.arlodev.onlinenow.LauncherState

/**
 * Text drawn as a dot matrix.
 * [pitch] is the distance between dot centres; unlit dots are only drawn when [offColor] is given.
 */
@Composable
fun DotText(
    text: String,
    pitch: Dp,
    color: Color,
    modifier: Modifier = Modifier,
    spacing: Int = 1,
    dotRatio: Float = 0.74f,
    offColor: Color? = null,
) {
    val layout = remember(text, spacing) { DotFont.layout(text, spacing) }
    Canvas(modifier.size(pitch * layout.cols, pitch * layout.rows)) {
        val step = pitch.toPx()
        val radius = step * dotRatio / 2f
        for (row in 0 until layout.rows) {
            for (col in 0 until layout.cols) {
                val lit = layout.lit[row * layout.cols + col]
                val dotColor = if (lit) color else offColor
                if (dotColor != null) {
                    drawCircle(dotColor, radius, Offset(col * step + step / 2f, row * step + step / 2f))
                }
            }
        }
    }
}

/** An app-icon dot grid drawn into a square of [boxSize]. Bigger dots = brighter parts of the icon. */
@Composable
fun DotGlyph(grid: DotGrid, boxSize: Dp, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.size(boxSize)) {
        val n = DotMath.GRID
        val step = size.width / n
        for (i in grid.levels.indices) {
            val level = grid.levels[i].toInt()
            if (level == 0) continue
            val radius = step * when (level) {
                1 -> 0.20f
                2 -> 0.32f
                else -> 0.43f
            }
            drawCircle(color, radius, Offset((i % n + 0.5f) * step, (i / n + 0.5f) * step))
        }
    }
}

/**
 * The icon of one app: a dot-matrix picture inside a dark circle (default),
 * or the original icon. A small accent dot shows when the app has a notification.
 */
@Composable
fun AppIcon(
    app: AppInfo,
    state: LauncherState,
    diameter: Dp,
    hasNotification: Boolean,
    modifier: Modifier = Modifier,
) {
    val p = LocalPalette.current
    val pm = LocalContext.current.packageManager

    Box(modifier.size(diameter), contentAlignment = Alignment.Center) {
        if (state.iconStyle == IconStyle.Original) {
            val bitmap by produceState<ImageBitmap?>(DotIcons.cachedOriginal(app.packageName), app.packageName) {
                value = DotIcons.loadOriginal(pm, app.packageName)
            }
            bitmap?.let {
                Image(bitmap = it, contentDescription = null, modifier = Modifier.size(diameter * 0.94f))
            }
        } else {
            val grid by produceState<DotGrid?>(DotIcons.cached(app.packageName), app.packageName) {
                value = DotIcons.load(pm, app)
            }
            Box(
                Modifier
                    .size(diameter)
                    .clip(CircleShape)
                    .background(p.card)
                    .border(1.dp, p.border, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                grid?.let { DotGlyph(it, diameter * 0.68f, p.text) }
            }
        }

        if (hasNotification) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .size(12.dp)
                    .background(p.background, CircleShape)
                    .padding(2.dp)
                    .background(p.accent, CircleShape)
            )
        }
    }
}
